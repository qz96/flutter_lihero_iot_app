import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import 'package:thingsboard_app/modules/cyx7/cyx7_log.dart';

/// CYX7「参数校准配置」子页（对齐原生 `LiherCYXParamSetActivity`）。
///
/// 点位的寄存器地址、类型与缩放与原程序一致（ModBus 40001 基址 + `Write DIY=<hex>`）：
///   pH  ：从站 3，测量值 40002(2 寄存器 float)、信号值 40008(2)、
///         第一点 40049(2 寄存器 float 信号值) + 40051(1 寄存器 整型×0.01)、
///         第二点 40052/40054、第三点 40055/40057、温度 40059(2 寄存器 float)
///   ORP ：从站 5，测点地址与 pH 相同；测量值 40002、信号值 40008
///   电导：从站 4，第一点 40050(2 寄存器 float 信号值) + 40052(2 寄存器 float 电导率值)
///   启动：40058(1 寄存器)=定标点数、40061(1 寄存器)=1 启动温度定标
class Cyx7ParamSetPage extends StatefulWidget {
  const Cyx7ParamSetPage({super.key});

  @override
  State<Cyx7ParamSetPage> createState() => _Cyx7ParamSetPageState();
}

/// 单个定标点：信号值(mV) + 校准值
class _CalibPoint {
  final int signalAddress;
  final int valueAddress;
  final bool valueIsFloat;

  double? signal;
  double? value;

  _CalibPoint({
    required this.signalAddress,
    required this.valueAddress,
    required this.valueIsFloat,
  });

  bool get isSet => signal != null && value != null;
}

class _Cyx7ParamSetPageState extends State<Cyx7ParamSetPage> {
  static const _channel = MethodChannel('lihero_cyx7');
  static const _events = EventChannel('lihero_cyx7/events');

  static const _readStart = 40002;
  static const _readCount = 8;

  StreamSubscription<dynamic>? _eventSubscription;
  final _logLines = <String>[];

  String _param = 'ph';
  String _status = '未连接';
  bool _busy = false;

  double? _measured;
  double? _signal;
  double? _temperature;

  late final Map<String, List<_CalibPoint>> _points = {
    'ph': [
      _CalibPoint(signalAddress: 40049, valueAddress: 40051, valueIsFloat: false),
      _CalibPoint(signalAddress: 40052, valueAddress: 40054, valueIsFloat: false),
      _CalibPoint(signalAddress: 40055, valueAddress: 40057, valueIsFloat: false),
    ],
    'orp': [
      _CalibPoint(signalAddress: 40049, valueAddress: 40051, valueIsFloat: false),
      _CalibPoint(signalAddress: 40052, valueAddress: 40054, valueIsFloat: false),
      _CalibPoint(signalAddress: 40055, valueAddress: 40057, valueIsFloat: false),
    ],
    'ddl': [
      _CalibPoint(signalAddress: 40050, valueAddress: 40052, valueIsFloat: true),
    ],
  };

  bool get _connected => _status == '已连接';

  /// 从站地址：pH=3 / 电导率=4 / ORP=5（原程序 L331/L352/L343）。
  int get _slave => switch (_param) {
    'ph' => 3,
    'ddl' => 4,
    _ => 5,
  };

  String get _paramName => switch (_param) {
    'ph' => 'pH',
    'ddl' => '电导率',
    _ => 'ORP',
  };

  String get _valueUnit => switch (_param) {
    'ddl' => 'uS/cm',
    _ => '',
  };

  @override
  void initState() {
    super.initState();
    _eventSubscription = _events.receiveBroadcastStream().listen((event) {
      if (!mounted || event is! Map) return;
      final name = event['event']?.toString() ?? '';
      setState(() {
        if (name == 'ready') _status = '已连接';
        if (name == 'connecting') _status = '连接中';
        if (name == 'disconnected') _status = '未连接';
        if (name == 'commandStarted') _logLines.add('发送:${event['command'] ?? ''}');
        if (name == 'response') _logLines.add('接收:${event['text'] ?? ''}');
        if (name == 'commandResult') {
          _logLines.add(
            event['ok'] == true
                ? '成功:${event['response'] ?? '已响应'}'
                : '失败:${event['error'] ?? ''}',
          );
        }
        if (_logLines.length > 120) {
          _logLines.removeRange(0, _logLines.length - 120);
        }
      });
      // 原程序 LiherCYXParamSetActivity 对每条指令都上报，sampleId 固定为 LhRTULLJ
      // （ModBus 的 Write DIY=<hex> 也走这里）。
      if (name == 'commandStarted') {
        unawaited(Cyx7Log.upload(
          sampleId: 'LhRTULLJ',
          content: event['command']?.toString() ?? '',
        ));
      }
    }, onError: (_) {});
    WidgetsBinding.instance.addPostFrameCallback((_) => _readRegisters());
  }

  @override
  void dispose() {
    _eventSubscription?.cancel();
    super.dispose();
  }

  // ==========================================================================
  // 通道
  // ==========================================================================
  Future<dynamic> _call(String method, [Map<String, dynamic>? arguments]) async {
    try {
      return await _channel.invokeMethod(method, arguments);
    } on PlatformException catch (error) {
      if (mounted) _toast(error.message ?? error.code);
      return null;
    }
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 3)),
    );
  }

  // ==========================================================================
  // 读取：Write DIY=<功能码03，40002 起 8 个寄存器>
  // ==========================================================================
  Future<void> _readRegisters() async {
    if (!_connected) return;
    setState(() => _busy = true);
    final response = await _call('modbusRead', {
      'adr': _slave,
      'address': _readStart,
      'count': _readCount,
    });
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (response is Map) _applyRegisters(response);
    });
  }

  void _applyRegisters(Map<dynamic, dynamic> response) {
    if (response['order'] != 0x03) return;
    final registers = response['registers'];
    if (registers is! List || registers.length < _readCount) return;
    double registerAt(int address) =>
        (registers[address - _readStart] as num).toDouble();
    double floatAt(int address) =>
        _registersToFloat(registerAt(address).toInt(), registerAt(address + 1).toInt());
    setState(() {
      _measured = floatAt(40002);
      _signal = floatAt(40008);
      // 温度定标值（40059 不在读取范围内时保持原值）
      _temperature = null;
    });
  }

  double _registersToFloat(int high, int low) {
    final bits = ((high & 0xFFFF) << 16) | (low & 0xFFFF);
    return _intBitsToFloat(bits);
  }

  double _intBitsToFloat(int bits) {
    final buffer = ByteData(4)..setUint32(0, bits);
    return buffer.getFloat32(0);
  }

  int _floatToBits(double value) {
    final buffer = ByteData(4)..setFloat32(0, value);
    return buffer.getUint32(0);
  }

  // ==========================================================================
  // 写入：Write DIY=<功能码10>
  // ==========================================================================
  /// 单点写寄存器序列 = 信号值(2 寄存器 float) + 值（pH/ORP 1 个整型寄存器、电导 2 个 float）。
  List<int> _pointRegisters(_CalibPoint point) {
    final bits = _floatToBits(point.signal ?? 0);
    final registers = <int>[(bits >> 16) & 0xFFFF, bits & 0xFFFF];
    if (point.valueIsFloat) {
      final valueBits = _floatToBits(point.value ?? 0);
      registers
        ..add((valueBits >> 16) & 0xFFFF)
        ..add(valueBits & 0xFFFF);
    } else {
      // 原程序：显示值 ÷ dm(0.01) 取整
      registers.add(((point.value ?? 0) / 0.01).round() & 0xFFFF);
    }
    return registers;
  }

  Future<void> _writePoints() async {
    if (!_connected) return;
    final points = _points[_param]!;
    final firstSet = points.indexWhere((point) => point.isSet);
    if (firstSet < 0) {
      _toast('请先设置定标点数据');
      return;
    }
    // 原程序按顺序拼接自第一个已设置点起的寄存器（1 点 3 个、2 点 6 个、3 点 9 个）
    final values = <int>[];
    for (var index = firstSet; index < points.length; index++) {
      if (!points[index].isSet) {
        if (values.isNotEmpty) {
          _toast('两点定标需要以第一点和第二点设置的数据来写入');
          return;
        }
        continue;
      }
      values.addAll(_pointRegisters(points[index]));
    }
    final address = points[firstSet].signalAddress - 40001;
    final response = await _call('modbusWrite', {
      'adr': _slave,
      'address': address,
      'values': values,
    });
    if (!mounted) return;
    if (response is Map && response['order'] == 0x10) {
      _toast('定标点数据已下发');
    }
  }

  Future<void> _startCalibration() async {
    if (!_connected) return;
    final points = _points[_param]!;
    final count = points.where((point) => point.isSet).length;
    if (count == 0) {
      _toast('请先设置定标点数据');
      return;
    }
    final pointResponse = await _call('modbusWrite', {
      'adr': _slave,
      'address': 40058 - 40001,
      'values': [count],
    });
    if (!mounted) return;
    if (pointResponse is! Map || pointResponse['order'] != 0x10) return;
    _toast('已启动定标校准（$count 点）');
    // 温度定标：写 40059 后启动 40061=1
    if (_temperature != null) {
      final bits = _floatToBits(_temperature!);
      await _call('modbusWrite', {
        'adr': _slave,
        'address': 40059 - 40001,
        'values': [(bits >> 16) & 0xFFFF, bits & 0xFFFF],
      });
      if (!mounted) return;
      await _call('modbusWrite', {
        'adr': _slave,
        'address': 40061 - 40001,
        'values': [1],
      });
    }
  }

  // ==========================================================================
  // 点位输入
  // ==========================================================================
  Future<void> _editPoint(int index, {required bool signalOnly}) async {
    final point = _points[_param]![index];
    final signalController = TextEditingController(
      text: point.signal == null ? '' : point.signal!.toString(),
    );
    final valueController = TextEditingController(
      text: point.value == null ? '' : point.value!.toString(),
    );
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text('$_paramName 第 ${index + 1} 点${signalOnly ? '信号值设置' : '设置'}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: signalController,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: '信号值', suffixText: 'mV'),
            ),
            const SizedBox(height: 8),
            if (!signalOnly)
              TextField(
                controller: valueController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: '$_paramName 值',
                  suffixText: _valueUnit,
                ),
              ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () {
              final signal = double.tryParse(signalController.text.trim());
              final value = signalOnly
                  ? point.value
                  : double.tryParse(valueController.text.trim());
              if (signal == null) {
                _toast('请输入合法的信号值');
                return;
              }
              if (!signalOnly && value == null) {
                _toast('请输入合法的$_paramName 值');
                return;
              }
              if (!signalOnly && !point.valueIsFloat && value! < 0) {
                _toast('当前协议暂不支持负数值设置');
                return;
              }
              setState(() {
                point.signal = signal;
                point.value = value;
              });
              Navigator.of(dialogContext).pop();
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  Future<void> _editTemperature() async {
    await _editValue(
      title: '温度定标设置（°C）',
      initial: _temperature,
      onConfirm: (value) => setState(() => _temperature = value),
    );
  }

  Future<void> _editValue({
    required String title,
    required double? initial,
    required void Function(double value) onConfirm,
  }) async {
    final controller = TextEditingController(text: initial?.toString() ?? '');
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          decoration: const InputDecoration(labelText: '数值'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () {
              final value = double.tryParse(controller.text.trim());
              if (value == null) {
                _toast('请输入合法数值');
                return;
              }
              onConfirm(value);
              Navigator.of(dialogContext).pop();
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  // ==========================================================================
  // UI
  // ==========================================================================
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          onPressed: () => context.pop(),
          icon: const Icon(Icons.arrow_back),
        ),
        title: const Text('参数校准'),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: _busy || !_connected ? null : _readRegisters,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
            children: [
              _card(
                '参数名称',
                [
                  _row('当前参数', '$_paramName（从站 $_slave）', onTap: _selectParam),
                  _row('测量值', _formatValue(_measured), onTap: null),
                  _row('信号值', _formatValue(_signal), onTap: null),
                ],
              ),
              _card(
                '定标点设置',
                [
                  for (var index = 0; index < _points[_param]!.length; index++) ...[
                    _row(
                      '第 ${index + 1} 点${_param == 'ddl' ? '设置' : '${_paramName}设置'}',
                      _formatValue(_points[_param]![index].value),
                      onTap: _busy || !_connected
                          ? null
                          : () => _editPoint(index, signalOnly: false),
                    ),
                    _row(
                      '第 ${index + 1} 点信号值设置',
                      _formatValue(_points[_param]![index].signal),
                      onTap: _busy || !_connected
                          ? null
                          : () => _editPoint(index, signalOnly: true),
                    ),
                  ],
                  _row(
                    '温度定标设置',
                    _formatValue(_temperature),
                    onTap: _busy || !_connected ? null : _editTemperature,
                  ),
                ],
              ),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: _busy || !_connected ? null : _writePoints,
                      child: const Text('下发定标点数据'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: FilledButton(
                      onPressed: _busy || !_connected ? null : _startCalibration,
                      child: const Text('启动定标校准'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              _logCard(),
            ],
          ),
          if (_busy) _loadingOverlay(),
        ],
      ),
    );
  }

  Future<void> _selectParam() async {
    final value = await showDialog<String>(
      context: context,
      builder: (dialogContext) => SimpleDialog(
        title: const Text('请选择参数'),
        children: ['ph', 'orp', 'ddl']
            .map(
              (key) => SimpleDialogOption(
                onPressed: () => Navigator.of(dialogContext).pop(key),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(switch (key) {
                        'ph' => 'pH',
                        'ddl' => '电导率',
                        _ => 'ORP',
                      }),
                    ),
                    if (key == _param) const Icon(Icons.check, size: 18),
                  ],
                ),
              ),
            )
            .toList(),
      ),
    );
    if (value == null) return;
    setState(() {
      _param = value;
      _measured = null;
      _signal = null;
    });
    await _readRegisters();
  }

  String _formatValue(double? value) {
    if (value == null) return '-';
    if (value == value.roundToDouble()) return value.toInt().toString();
    return value.toStringAsFixed(2);
  }

  Widget _logCard() {
    return _card(
      '指令日志',
      [
        Container(
          height: 150,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.black12),
            borderRadius: BorderRadius.circular(6),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: _logLines.isEmpty
              ? const Align(
                  alignment: Alignment.topLeft,
                  child: Text(
                    'ModBus 读写指令将在此显示',
                    style: TextStyle(fontSize: 12.5, color: Colors.black45),
                  ),
                )
              : ListView.builder(
                  itemCount: _logLines.length,
                  itemBuilder: (_, index) {
                    final line = _logLines[index];
                    final color = line.startsWith('发送')
                        ? Theme.of(context).colorScheme.primary
                        : line.startsWith('接收')
                        ? Colors.teal
                        : line.startsWith('成功')
                        ? Colors.green
                        : Colors.red;
                    return SelectableText(
                      line,
                      style: TextStyle(fontSize: 12.5, color: color),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _card(String title, List<Widget> children) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(title, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
            const SizedBox(height: 4),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _row(String label, String value, {VoidCallback? onTap}) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(
          children: [
            Text(label, style: const TextStyle(fontSize: 12.5, color: Colors.black54)),
            const Spacer(),
            Flexible(
              child: Text(
                value,
                textAlign: TextAlign.right,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 13),
              ),
            ),
            if (onTap != null) const Icon(Icons.chevron_right, size: 16, color: Colors.black38),
          ],
        ),
      ),
    );
  }

  Widget _loadingOverlay() {
    return Positioned.fill(
      child: ColoredBox(
        color: const Color(0x66000000),
        child: Center(
          child: Container(
            width: 180,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(strokeWidth: 3),
                ),
                SizedBox(height: 12),
                Text('正在读写 ModBus 寄存器…', textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
