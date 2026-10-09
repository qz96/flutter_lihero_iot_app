import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import 'package:thingsboard_app/modules/cyx7/cyx7_log.dart';
import 'package:thingsboard_app/modules/cyx7/cyx7_scan.dart';

/// CYX7 的「RTU 功能入口」子页（对齐原生 `LiherCYXRTUActivity`）。
///
/// 三个分区：雷达水位计 / 雷达流量计 / RTU 设置，底部指令日志与窗口模式发送框。
/// 指令文本、参数格式与缩放全部按原程序实现：
///   `Read RtuInf` / `Read SamInf` / `Read LotInf`
///   `Set Start` / `Set Stop` / `Set ZeroInit` / `Set DataUp`
///   `Write PwrEn=` / `Write SapInterval=` / `Write UpInterval=` / `Write DevST=` / `Write DevMN=`
///   `Write SamWait=` / `Write LotType=` / `Write DevIp=nn#ip12` / `Write DevPort=nn#ppppp`
///   `Write Protocol=nn#t` / `Write LotIpEn=xxx` / `Write SamdevType=` / `Write BleAddr=` / `Write SysTime=`
class Cyx7RtuPage extends StatefulWidget {
  const Cyx7RtuPage({super.key});

  @override
  State<Cyx7RtuPage> createState() => _Cyx7RtuPageState();
}

class _Cyx7RtuPageState extends State<Cyx7RtuPage> {
  static const _channel = MethodChannel('lihero_cyx7');
  static const _events = EventChannel('lihero_cyx7/events');

  StreamSubscription<dynamic>? _eventSubscription;
  final _rawCommandController = TextEditingController();
  final _logController = ScrollController();
  final _logLines = <String>[];

  Map<String, dynamic>? _rtuInf;
  Map<String, dynamic>? _lotInf;
  Map<String, dynamic>? _samInf;

  String _tab = 'set';
  String _status = '未连接';
  String _busyMessage = '正在处理指令…';
  bool _busy = false;
  bool _windowMode = false;
  bool _debugMode = false;
  bool _uploading = false;
  Timer? _uploadTimer;
  Timer? _debugTimer;

  bool get _connected => _status == '已连接';
  List<Map<String, dynamic>> get _services {
    final services = _lotInf?['services'];
    if (services is List) {
      return services.map((item) => Map<String, dynamic>.from(item as Map)).toList();
    }
    return const [];
  }

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
      // 原程序 LiherCYXRTUActivity 对每条指令都上报，sampleId 固定为 LhCYXRTU
      if (name == 'commandStarted') {
        unawaited(Cyx7Log.upload(
          sampleId: 'LhCYXRTU',
          content: event['command']?.toString() ?? '',
        ));
      }
    }, onError: (_) {});
    WidgetsBinding.instance.addPostFrameCallback((_) => _refresh());
  }

  @override
  void dispose() {
    _uploadTimer?.cancel();
    _debugTimer?.cancel();
    _eventSubscription?.cancel();
    _rawCommandController.dispose();
    _logController.dispose();
    super.dispose();
  }

  // ==========================================================================
  // 通道封装
  // ==========================================================================
  Future<dynamic> _call(String method, [Map<String, dynamic>? arguments]) async {
    try {
      return await _channel.invokeMethod(method, arguments);
    } on PlatformException catch (error) {
      if (mounted) _toast(error.message ?? error.code);
      return null;
    }
  }

  Future<bool> _send(String method, Map<String, dynamic> arguments) async {
    if (!_connected) {
      _toast('请先在采样箱页面连接设备');
      return false;
    }
    setState(() {
      _busy = true;
      _busyMessage = '正在处理指令…';
    });
    final response = await _call(method, arguments);
    if (!mounted) return false;
    setState(() => _busy = false);
    return response != null;
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 3)),
    );
  }

  // ==========================================================================
  // 读取
  // ==========================================================================
  Future<void> _refresh() async {
    if (!_connected) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在读取 RTU 信息…';
    });
    final rtuInf = await _call('readRtuInf');
    final lotInf = _tab == 'set' ? await _call('readLotInf') : null;
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (rtuInf is Map) _rtuInf = Map<String, dynamic>.from(rtuInf);
      if (lotInf is Map) _lotInf = Map<String, dynamic>.from(lotInf);
    });
  }

  Future<void> _readSamInf() async {
    if (!_connected) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在读取传感器数据…';
    });
    final response = await _call('readSamInf');
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (response is Map) _samInf = Map<String, dynamic>.from(response);
    });
  }

  void _switchTab(String tab) {
    setState(() => _tab = tab);
    if (tab == 'set') {
      _refresh();
    } else {
      _readSamInf();
    }
  }

  // ==========================================================================
  // 设备控制
  // ==========================================================================
  Future<void> _toggleDeviceOnOff(bool on) async {
    await _send(on ? 'setStart' : 'setStop', {});
    await _refresh();
  }

  Future<void> _togglePwrEn(bool on) async {
    final ok = await _send('writePwrEn', {'enabled': on});
    if (ok) setState(() => _lotInf = {...?_lotInf, 'powerEnabled': on ? 1 : 0});
  }

  /// 「采集上报」：先 Set DataUp，应答后 10s 自动 Read LotInf（原程序 L1395-1410/L1530-1545）。
  Future<void> _upload() async {
    final ok = await _send('setDataUp', {});
    if (!ok) return;
    setState(() => _uploading = true);
    _toast('已发起采集上报，10 秒后查询结果');
    _uploadTimer?.cancel();
    _uploadTimer = Timer(const Duration(seconds: 10), () async {
      if (!mounted) return;
      final response = await _call('readLotInf');
      if (!mounted) return;
      setState(() {
        _uploading = false;
        if (response is Map) _lotInf = Map<String, dynamic>.from(response);
      });
      final step = (_lotInf?['uploadStep'] as num?)?.toInt() ?? 0;
      final error = (_lotInf?['uploadError'] as num?)?.toInt() ?? 0;
      if (error > 10) {
        _toast('上报失败');
      } else if (step >= 10 && step <= 49) {
        _toast('正在上报中');
      } else {
        _toast('上报完毕  ${_lotInf?['uploadEndTime'] ?? ''}');
      }
    });
  }

  /// 「调试模式」：Set Start 后每 3s 回读 Read SamInf；Set Stop 后 1s 回读一次。
  Future<void> _toggleDebugMode() async {
    _debugTimer?.cancel();
    _debugTimer = null;
    if (_debugMode) {
      await _send('setStop', {});
      setState(() => _debugMode = false);
      _debugTimer = Timer(const Duration(seconds: 1), _readSamInf);
    } else {
      final ok = await _send('setStart', {});
      if (!ok) return;
      setState(() => _debugMode = true);
      _debugTimer = Timer.periodic(const Duration(seconds: 3), (_) => _readSamInf());
    }
  }

  Future<void> _zeroInit() async {
    final ok = await _send('setZeroInit', {});
    if (!ok) return;
    _toast('累计流量已清空');
    _debugTimerCheck();
  }

  void _debugTimerCheck() {
    Timer(const Duration(milliseconds: 500), _readSamInf);
  }

  // ==========================================================================
  // RTU 设置项
  // ==========================================================================
  Future<void> _editSamWait() async {
    final current = (_samInf?['sampleWait'] as num?)?.toInt() ?? 0;
    await _editNumber(
      title: '上电延时（秒）',
      initial: current.toDouble(),
      onConfirm: (value) => _send('writeSamWait', {'seconds': value.toInt()}),
    );
  }

  Future<void> _editSampleInterval() async {
    final current = (_lotInf?['sampleInterval'] as num?)?.toInt() ?? 0;
    await _editNumber(
      title: '采集间隔（分钟）',
      initial: current.toDouble(),
      onConfirm: (value) => _send('writeSapInterval', {'minutes': value.toInt()}),
    );
  }

  Future<void> _editUploadInterval() async {
    final current = (_lotInf?['uploadInterval'] as num?)?.toInt() ?? 0;
    await _editNumber(
      title: '上传间隔（分钟）',
      initial: current.toDouble(),
      onConfirm: (value) => _send('writeUpInterval', {'minutes': value.toInt()}),
    );
  }

  Future<void> _editSystemSt() async {
    await _editText(
      title: '系统编码',
      initial: (_lotInf?['systemSt']?.toString() ?? ''),
      onConfirm: (value) {
        if (value.isEmpty) {
          _toast('系统编码不能为空');
          return Future.value();
        }
        return _send('writeDevST', {'value': value});
      },
    );
  }

  Future<void> _editMn() async {
    await _editText(
      title: '设备 MN',
      initial: (_rtuInf?['mn']?.toString() ?? ''),
      onConfirm: (value) => _send('writeDevMN', {'value': value}),
    );
  }

  Future<void> _editSysTime() async {
    await _editText(
      title: '系统时间',
      initial: (_rtuInf?['systemTimeRaw']?.toString() ?? ''),
      helper: '格式 yyMMddHHmmss',
      onConfirm: (value) => _send('writeSysTime', {'value': value}),
    );
  }

  Future<void> _selectLotType() async {
    final current = (_lotInf?['moduleType'] as num?)?.toInt() ?? -1;
    final value = await _select<int>(
      title: '通信模组',
      options: const {0: 'NB 模组', 1: '4G 模组'},
      current: current,
    );
    if (value == null) return;
    await _send('writeLotType', {'type': value});
  }

  Future<void> _selectSamdevType() async {
    final current = (_rtuInf?['sampleDeviceType'] as num?)?.toInt() ?? 0;
    // 原程序：bit0 雷达水位计=1、bit1 雷达流量计=2、未连接=0
    final options = <int, String>{0: '未连接设备', 1: '雷达水位计', 2: '雷达流量计'};
    final value = await _select<int>(
      title: '连接设备',
      options: options,
      current: options.containsKey(current) ? current : 0,
    );
    if (value == null) return;
    final ok = await _send('writeSamdevType', {'value': value});
    if (ok) await _refresh();
  }

  Future<void> _scanBoxMac() async {
    final code = await Cyx7Scan.scan(context);
    if (code == null) {
      _toast('未读取到二维码信息');
      return;
    }
    if (code.length != 12 || !code.startsWith(Cyx7Scan.boxRtuPrefix)) {
      _toast('二维码非力合采样箱标准码（需 0D07 开头 12 位）');
      return;
    }
    await _send('writeBleAddr', {'value': code});
  }

  /// 新增/编辑某一路通讯：Write DevIp → Write DevPort（如填写）→ Write Protocol。
  Future<void> _editService(int index) async {
    final services = _services;
    final service = index < services.length ? services[index] : <String, dynamic>{};
    final hostController = TextEditingController(text: service['host']?.toString() ?? '');
    final portController = TextEditingController(text: service['port']?.toString() ?? '');
    final protocol = (service['protocol'] as num?)?.toInt() ?? 0;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        var selectedProtocol = protocol;
        return StatefulBuilder(
          builder: (context, setDialogState) => AlertDialog(
            title: Text('通讯${_netLabel(index)}'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: hostController,
                  decoration: const InputDecoration(labelText: 'IP / 域名'),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: portController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '端口'),
                ),
                const SizedBox(height: 8),
                SegmentedButton<int>(
                  segments: const [
                    ButtonSegment(value: 0, label: Text('212')),
                    ButtonSegment(value: 1, label: Text('MQTT')),
                  ],
                  selected: {selectedProtocol},
                  onSelectionChanged: (value) =>
                      setDialogState(() => selectedProtocol = value.first),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () async {
                  final host = hostController.text.trim();
                  Navigator.of(dialogContext).pop();
                  if (host.isEmpty) {
                    _toast('IP/域名不能为空');
                    return;
                  }
                  final ipOk = await _send('writeDevIp', {'index': index, 'host': host});
                  if (!ipOk) return;
                  final port = int.tryParse(portController.text.trim());
                  if (port != null && port > 0) {
                    final portOk = await _send('writeDevPort', {'index': index, 'port': port});
                    if (!portOk) return;
                  }
                  await _send('writeProtocol', {'index': index, 'protocol': selectedProtocol});
                  // 使能该路
                  final bits = ((_lotInf?['serviceEnabledBits'] as num?)?.toInt() ?? 0) | (1 << index);
                  await _send('writeLotIpEn', {'bits': bits});
                  await _refresh();
                },
                child: const Text('写入'),
              ),
            ],
          ),
        );
      },
    );
  }

  /// 某一路通讯的状态操作：启用 / 停用 / 删除（删除=先清使能位，再清 IP）。
  Future<void> _serviceAction(int index) async {
    final services = _services;
    final service = index < services.length ? services[index] : <String, dynamic>{};
    final enabled = service['enabled'] == true;
    final action = await _select<String>(
      title: '通讯${_netLabel(index)}状态',
      options: {
        'enable': enabled ? '已启用（点击停用）' : '已停用（点击启用）',
        'delete': '删除本通信服务',
      },
      current: null,
    );
    if (action == null) return;
    final currentBits = (_lotInf?['serviceEnabledBits'] as num?)?.toInt() ?? 0;
    if (action == 'enable') {
      final bits = enabled ? currentBits & ~(1 << index) : currentBits | (1 << index);
      await _send('writeLotIpEn', {'bits': bits});
    } else {
      final confirmed = await _confirm('删除通信服务', '确认删除第 ${index + 1} 路通讯配置？');
      if (!confirmed) return;
      final bits = currentBits & ~(1 << index);
      await _send('writeLotIpEn', {'bits': bits});
      await _send('writeDevIp', {'index': index, 'host': '000000000000'});
      _toast('通信服务删除完成');
    }
    await _refresh();
  }

  Future<void> _addService() async {
    final services = _services;
    final freeIndex = List.generate(5, (index) => index).firstWhere(
      (index) => index >= services.length || services[index]['valid'] != true,
      orElse: () => -1,
    );
    if (freeIndex < 0) {
      _toast('当前 RTU 的通信服务已达到上限 5 个，请清空现有通信服务再新增');
      return;
    }
    await _editService(freeIndex);
  }

  String _netLabel(int index) => ['一', '二', '三', '四', '五'][index];

  // ==========================================================================
  // 窗口模式
  // ==========================================================================
  Future<void> _sendRawCommand() async {
    final command = _rawCommandController.text.trim();
    if (command.isEmpty) {
      _toast('请输入指令内容');
      return;
    }
    _rawCommandController.clear();
    await _send('sendCommand', {'command': command});
  }

  // ==========================================================================
  // 对话框
  // ==========================================================================
  Future<void> _editText({
    required String title,
    required String initial,
    required Future<void> Function(String value) onConfirm,
    String? helper,
    TextInputType? keyboardType,
  }) async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final controller = TextEditingController(text: initial);
        return AlertDialog(
          title: Text(title),
          content: TextField(
            controller: controller,
            autofocus: true,
            keyboardType: keyboardType,
            decoration: InputDecoration(labelText: title, helperText: helper),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: const Text('取消'),
            ),
            FilledButton(
              onPressed: () async {
                final value = controller.text.trim();
                Navigator.of(dialogContext).pop();
                await onConfirm(value);
              },
              child: const Text('确定'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _editNumber({
    required String title,
    required double initial,
    required Future<void> Function(double value) onConfirm,
  }) async {
    await _editText(
      title: title,
      initial: initial.toInt().toString(),
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      onConfirm: (value) async {
        final parsed = double.tryParse(value);
        if (parsed == null) {
          _toast('请输入合法数值');
          return;
        }
        await onConfirm(parsed);
      },
    );
  }

  Future<T?> _select<T>({
    required String title,
    required Map<T, String> options,
    required T? current,
  }) {
    return showDialog<T>(
      context: context,
      builder: (dialogContext) => SimpleDialog(
        title: Text(title),
        children: options.entries
            .map(
              (entry) => SimpleDialogOption(
                onPressed: () => Navigator.of(dialogContext).pop(entry.key),
                child: Row(
                  children: [
                    Expanded(child: Text(entry.value)),
                    if (entry.key == current) const Icon(Icons.check, size: 18),
                  ],
                ),
              ),
            )
            .toList(),
      ),
    );
  }

  Future<bool> _confirm(String title, String message) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: const Text('确定'),
          ),
        ],
      ),
    );
    return result == true;
  }

  // ==========================================================================
  // 展示辅助
  // ==========================================================================
  String _text(dynamic value, {String fallback = '-'}) {
    final text = value?.toString().trim() ?? '';
    return text.isEmpty ? fallback : text;
  }

  String _format(dynamic value, {int digits = 3}) {
    final number = value is num ? value.toDouble() : double.tryParse('${value ?? ''}');
    if (number == null || number <= -9999) return '-';
    return number.toStringAsFixed(digits);
  }

  String _deviceStateText() {
    final state = (_rtuInf?['deviceState'] as num?)?.toInt() ?? 0;
    if (state & 0x04 != 0) return '正在上传';
    if (state & 0x02 != 0) return '正在采样';
    if (state & 0x01 != 0) return '定时采样';
    return '空闲';
  }

  String _moduleText() {
    final value = (_lotInf?['moduleType'] as num?)?.toInt() ?? -1;
    return switch (value) {
      0 => 'NB',
      1 => '4G',
      _ => '-',
    };
  }

  String _signalText() {
    final value = (_lotInf?['signal'] as num?)?.toInt() ?? -1;
    if (value < 0) return '-';
    if (value >= 99) return '无信号';
    if (value <= 16) return '弱';
    if (value <= 20) return '中';
    return '强';
  }

  String _directionText() {
    final value = (_samInf?['direction'] as num?)?.toInt() ?? -1;
    return switch (value) {
      1 => '靠近',
      2 => '远离',
      _ => '-',
    };
  }

  String _batteryText() {
    final voltage = (_rtuInf?['batteryVoltage'] as num?)?.toDouble() ?? 0;
    if (voltage < 9.8) return '0%';
    if (voltage >= 10.6) return '100%';
    return '${((voltage - 9.8) / 0.008).round()}%';
  }

  /// 累计流量 = 高位 × 10^7 + 低位（原程序 L1827）。
  String _cumulative(dynamic low, dynamic high, {String suffix = ''}) {
    final lowValue = (low is num) ? low.toDouble() : double.tryParse('${low ?? ''}');
    if (lowValue == null || lowValue <= -9999) return '-';
    final highValue = (high is num) ? high.toInt() : int.tryParse('${high ?? ''}') ?? 0;
    final prefix = highValue > 0 ? '$highValue * 10^7 + ' : '';
    return '$prefix${lowValue.toStringAsFixed(3)}$suffix';
  }

  // ==========================================================================
  // UI
  // ==========================================================================
  @override
  Widget build(BuildContext context) {
    final title = _text(_rtuInf?['deviceName'], fallback: 'RTU 功能入口');
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          onPressed: () => context.pop(),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(title),
        actions: [
          IconButton(
            tooltip: '刷新',
            onPressed: _busy || !_connected ? null : _refresh,
            icon: const Icon(Icons.refresh),
          ),
          PopupMenuButton<String>(
            onSelected: (value) {
              switch (value) {
                case 'window':
                  setState(() => _windowMode = !_windowMode);
                  break;
                case 'mn':
                  _editMn();
                  break;
                case 'st':
                  _editSystemSt();
                  break;
                case 'time':
                  _editSysTime();
                  break;
                case 'mac':
                  _scanBoxMac();
                  break;
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'window',
                child: Text(_windowMode ? '切换到交互模式' : '切换到窗口模式'),
              ),
              const PopupMenuItem(value: 'mn', child: Text('编辑设备 MN')),
              const PopupMenuItem(value: 'st', child: Text('编辑系统编码')),
              const PopupMenuItem(value: 'time', child: Text('设置系统时间')),
              const PopupMenuItem(value: 'mac', child: Text('扫码设置 MAC 地址')),
            ],
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
            children: [
              _tabSelector(),
              if (_tab == 'swj') ...[
                _waterLevelBlock(),
              ],
              if (_tab == 'llj') ...[
                _flowBlock(),
              ],
              if (_tab == 'set') ...[
                _deviceInfoBlock(),
                _serviceBlock(),
              ],
              if (_windowMode) _windowCard(),
              _logCard(),
            ],
          ),
          if (_busy) _loadingOverlay(),
        ],
      ),
    );
  }

  Widget _tabSelector() {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: SegmentedButton<String>(
        segments: const [
          ButtonSegment(value: 'swj', label: Text('雷达水位计')),
          ButtonSegment(value: 'llj', label: Text('雷达流量计')),
          ButtonSegment(value: 'set', label: Text('RTU 设置')),
        ],
        selected: {_tab},
        onSelectionChanged: (value) => _switchTab(value.first),
      ),
    );
  }

  Widget _waterLevelBlock() {
    final info = _samInf ?? const <String, dynamic>{};
    return _card(
      '雷达水位计',
      [
        _row('上电延时', '${(info['sampleWait'] as num?)?.toInt() ?? 0} 秒', onTap: _editSamWait),
        _row('倾斜角', '${_format(info['pitch'], digits: 2)} °'),
        _row('空高(m)', _format(info['height'])),
        _row('水位(m)', _format(info['waterLevel'])),
        _row('水深(m)', _format(info['depth'])),
        _row('瞬时流量(m³/s)', _format(info['rateFlow'])),
        _row('时隔流量(m³)', _format(info['intervalFlow'])),
        _row(
          '累计流量(m³)',
          _cumulative(info['cumulativeFlow'], info['cumulativeFlowHigh']),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _toggleDebugMode,
                child: Text(_debugMode ? '停止测量' : '开始测量'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: FilledButton(
                onPressed: _busy || !_connected ? null : _upload,
                child: Text(_uploading ? '获取上报结果' : '采集上报'),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _flowBlock() {
    final info = _samInf ?? const <String, dynamic>{};
    return _card(
      '雷达流量计',
      [
        _row('上电延时', '${(info['sampleWait'] as num?)?.toInt() ?? 0} 秒', onTap: _editSamWait),
        _row('横滚角', '${_format(info['roll'], digits: 2)} °'),
        _row('俯仰角', '${_format(info['pitch'], digits: 2)} °'),
        _row('水位(m)', _format(info['waterLevel'])),
        _row('空高(m)', _format(info['height'])),
        _row('水深(m)', _format(info['depth'])),
        _row('流速方向', _directionText()),
        _row('表面流速(m/s)', _format(info['surfaceVelocity'])),
        _row('断面流速(m/s)', _format(info['sectionVelocity'])),
        _row('过水面积(m²)', _format(info['area'])),
        _row('瞬时流量(m³/s)', _format(info['rateFlow'])),
        _row('淤积厚度(m)', _format(info['thickness'])),
        _row('时隔流量(m³)', _format(info['intervalFlow'])),
        _row('累计流量(m³)', _cumulative(info['cumulativeFlow'], info['cumulativeFlowHigh'])),
        _row(
          '正向累计流量(m³)',
          _cumulative(info['secondaryCumulativeFlow'], info['secondaryCumulativeFlowHigh']),
        ),
        _row(
          '反向累计流量(m³)',
          _cumulative(info['nightCumulativeFlow'], info['nightCumulativeFlowHigh']),
        ),
        _row(
          '绝对值累计流量(m³)',
          _cumulative(info['absoluteCumulativeFlow'], info['absoluteCumulativeFlowHigh']),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _zeroInit,
                child: const Text('清空累计流量'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _toggleDebugMode,
                child: Text(_debugMode ? '关闭调试模式' : '打开调试模式'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: FilledButton(
                onPressed: _busy || !_connected ? null : _upload,
                child: Text(_uploading ? '获取上报结果' : '采集上报'),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _deviceInfoBlock() {
    final rtu = _rtuInf ?? const <String, dynamic>{};
    final lot = _lotInf ?? const <String, dynamic>{};
    final deviceOn = ((rtu['deviceState'] as num?)?.toInt() ?? 0) != 0;
    final pwrEnabled = (lot['powerEnabled'] as num?)?.toInt() == 1;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _card(
          '设备信息',
          [
            _row(
              '电池',
              '${_format(rtu['batteryVoltage'], digits: 2)} V   ${_batteryText()}',
            ),
            _row('版本信息', _text(rtu['version'])),
            _row('系统时间', _text(rtu['systemTime']), onTap: _editSysTime),
            _row('工作状态', _deviceStateText()),
            _row('设备MN', _text(rtu['mn']), onTap: _editMn),
            _row('设备名称', _text(rtu['deviceName'])),
            _row('设备编号', _text(rtu['deviceNumber'])),
            _row('蓝牙地址', _text(rtu['bleAddress']), onTap: _scanBoxMac),
            _row('连接设备', _text(rtu['sampleDeviceType'] == null ? null : _samdevText()), onTap: _selectSamdevType),
            SwitchListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              title: const Text('设备开关'),
              value: deviceOn,
              onChanged: _busy || !_connected ? null : _toggleDeviceOnOff,
            ),
            SwitchListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              title: const Text('长供电开关'),
              value: pwrEnabled,
              onChanged: _busy || !_connected ? null : _togglePwrEn,
            ),
          ],
        ),
        _card(
          '通信信息',
          [
            _row('通信模组', _moduleText(), onTap: _selectLotType),
            _row('物联网卡', _text(lot['ccid'])),
            _row('IMSI号', _text(lot['imsi'])),
            _row('网络信号', _signalText()),
            _row('采集间隔', '${(lot['sampleInterval'] as num?)?.toInt() ?? 0} 分钟', onTap: _editSampleInterval),
            _row('上传间隔', '${(lot['uploadInterval'] as num?)?.toInt() ?? 0} 分钟', onTap: _editUploadInterval),
            _row('上传步骤', _text(lot['uploadStep'])),
            _row('故障信息', _text(lot['uploadError'])),
            _row('待传数据', '${(lot['pendingCount'] as num?)?.toInt() ?? 0} 条'),
            _row('最近上传时间', _text(lot['uploadEndTime'])),
            _row('系统编码', _text(lot['systemSt']), onTap: _editSystemSt),
          ],
        ),
      ],
    );
  }

  String _samdevText() {
    final value = (_rtuInf?['sampleDeviceType'] as num?)?.toInt() ?? 0;
    return switch (value) {
      1 => '雷达水位计',
      2 => '雷达流量计',
      _ => '未连接设备',
    };
  }

  Widget _serviceBlock() {
    final services = _services;
    final rows = <Widget>[];
    for (var index = 0; index < 5; index++) {
      final service = index < services.length ? services[index] : <String, dynamic>{};
      final valid = index < services.length && service['valid'] == true;
      if (!valid && index >= services.length) continue;
      rows.add(
        _card(
          '通讯${_netLabel(index)}',
          [
            _row('IP', _text(service['host']), onTap: _busy || !_connected ? null : () => _editService(index)),
            _row('端口', _text(service['port'])),
            _row(
              '上传协议',
              switch ((service['protocol'] as num?)?.toInt() ?? -1) {
                0 => '212',
                1 => 'MQTT',
                _ => '-',
              },
            ),
            _row(
              '状态',
              service['enabled'] == true ? '已启用' : '已停用',
              onTap: _busy || !_connected ? null : () => _serviceAction(index),
            ),
          ],
        ),
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ...rows,
        OutlinedButton(
          onPressed: _busy || !_connected ? null : _addService,
          child: const Text('一点多传 新增通讯'),
        ),
        const SizedBox(height: 10),
      ],
    );
  }

  Widget _windowCard() {
    return _card(
      '窗口模式（原始指令）',
      [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _rawCommandController,
                decoration: const InputDecoration(hintText: '请输入指令内容'),
                onSubmitted: (_) => _sendRawCommand(),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: _busy || !_connected ? null : _sendRawCommand,
              child: const Text('发送'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _logCard() {
    return _card(
      '指令日志',
      [
        Container(
          height: 160,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.black12),
            borderRadius: BorderRadius.circular(6),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: _logLines.isEmpty
              ? const Align(
                  alignment: Alignment.topLeft,
                  child: Text(
                    'RTU 指令交互信息将在此显示',
                    style: TextStyle(fontSize: 12.5, color: Colors.black45),
                  ),
                )
              : ListView.builder(
                  controller: _logController,
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
            width: 200,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(
                  width: 28,
                  height: 28,
                  child: CircularProgressIndicator(strokeWidth: 3),
                ),
                const SizedBox(height: 12),
                Text(_busyMessage, textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
