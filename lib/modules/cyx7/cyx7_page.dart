import 'dart:async';
import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';

import 'package:thingsboard_app/modules/cyx7/cyx7_log.dart';
import 'package:thingsboard_app/modules/cyx7/cyx7_scan.dart';

/// 井下采样器 CYX7（采样箱 2026 兼容 RTU 程序）页面。
///
/// 功能与协议完全对齐原生 `APP_SampleBox_sum/app/.../UI/LiherCYX7Activity.java`：
/// 帧格式 `AA BB + len + desID(12345678) + payloadLen + 文本指令 + XOR + EE FF`，
/// 指令全部是 ASCII 文本（`Read All` / `Write WorkMode=1` …），
/// 指令下发与解析都在原生层 `org.thingsboard.app.cyx7`，本页只负责交互与展示。
class Cyx7Page extends StatefulWidget {
  const Cyx7Page({super.key});

  @override
  State<Cyx7Page> createState() => _Cyx7PageState();
}

/// 采样模式（StringUtils.getSampleMode）
const _sampleModes = <int, String>{
  0: '维护',
  1: '等时',
  2: '雨量',
  3: '单触发',
  4: '定时',
  5: '液位',
  6: '手动测试',
  7: '流程测试',
  8: '等比例',
  9: '远程',
  10: '浮子',
  11: '五参数',
  12: '平台雨量',
  13: '疾控',
};

/// 触发位（对应 `Write Trigger=` 的 16 位位图，bit0 为最高位）
const _triggerLabels = <int, String>{
  0: '水位',
  1: '雨量',
  2: 'pH',
  3: '电导率',
  4: '浊度',
  5: '溶解氧',
  6: '水温',
  7: '瞬时流量',
  8: '液位',
};

/// 运行状态（StringUtils.getCYXRTUStatus 的常见取值）
const _deviceStatusText = <String, String>{
  '00': '正常',
  '001': '工作中',
  '002': '采样故障',
  '003': '缺液报警',
  '004': '泵故障',
  '005': '超时未完成',
};

/// 装瓶换连接流程的当前阶段（对应原程序连接回调里的 MAC 前缀分支）。
enum _BottleStage {
  idle,

  /// 已断开箱体，正在连接采样瓶（0B/0C）
  connectingBottle,

  /// 已发 lc 收到中间应答，等待设备推送 olc
  waitingValveClosed,

  /// 已断开采样瓶，正在回连箱体（0D）
  reconnectingBox,
}

class _Cyx7PageState extends State<Cyx7Page> {
  static const _channel = MethodChannel('lihero_cyx7');
  static const _events = EventChannel('lihero_cyx7/events');

  StreamSubscription<dynamic>? _eventSubscription;
  final _addressController = TextEditingController();
  final _rawCommandController = TextEditingController();
  final _logController = ScrollController();

  final _devices = <Map<String, dynamic>>[];
  final _logLines = <String>[];
  final _deviceSearchController = TextEditingController();
  StateSetter? _scanDialogSetState;

  Map<String, dynamic>? _basic;
  Map<String, dynamic>? _proInfo;
  Map<String, dynamic>? _samInf;

  String _status = '未连接';
  String _busyMessage = '正在处理采样箱指令…';
  bool _busy = false;
  bool _backgroundRead = false;
  bool _windowMode = false;

  /// 报警开关是本地状态：写入成功后翻转（原程序 alarmState，不来自应答字段）。
  bool _alarmEnabled = false;

  /// 强制卸瓶时日志前缀（原程序："确认强制卸瓶," + 指令）。
  String _pendingLogPrefix = '';

  /// 装瓶换连接流程状态（原程序用 tempBottleMac + 连接回调分支实现）。
  _BottleStage _bottleStage = _BottleStage.idle;
  int? _installBottleIndex;
  String? _installBottleCode;
  String _boxAddressCache = '';

  /// 本地维护的 6 段定时采样计划与 16 位触发位图（读取后同步）。
  List<Map<String, dynamic>> _plan = List.generate(
    6,
    (index) => <String, dynamic>{'index': index + 1, 'enabled': false, 'hour': 0, 'minute': 0},
  );
  List<bool> _triggerBits = List<bool>.filled(16, false);

  bool get _connected => _status == '已连接';

  @override
  void initState() {
    super.initState();
    _eventSubscription = _events.receiveBroadcastStream().listen((event) {
      if (!mounted || event is! Map) return;
      final name = event['event']?.toString() ?? '';
      if (_backgroundRead &&
          (name == 'commandStarted' || name == 'response' || name == 'commandResult')) {
        return;
      }
      final hasNewLog = name == 'commandStarted' || name == 'response' || name == 'commandResult';
      setState(() {
        if (name == 'commandStarted') {
          _logLines.add('发送:${event['command'] ?? ''}');
        }
        if (name == 'commandResult') {
          _busy = false;
        }
        if (name == 'response') {
          _logLines.add('接收:${event['text'] ?? ''}');
        }
        if (name == 'commandResult') {
          if (event['ok'] == true) {
            _logLines.add('成功:${event['response'] ?? '采样箱已响应'}');
          } else {
            _logLines.add('失败:${event['error'] ?? ''}');
          }
        }
        if (_logLines.length > 120) {
          _logLines.removeRange(0, _logLines.length - 120);
        }
        if (name == 'ready') _status = '已连接';
        if (name == 'connecting') _status = '连接中';
        if (name == 'unsolicitedResponse') {
          _logLines.add('接收(主动):${event['text'] ?? ''}');
        }
        if (name == 'disconnected') {
          _status = '未连接';
          _busy = false;
        }
        if (name == 'scanResult') {
          final item = Map<String, dynamic>.from(event);
          if (item['isBox'] == true &&
              !_devices.any((element) => element['address'] == item['address'])) {
            _devices.add(item);
            _scanDialogSetState?.call(() {});
          }
        }
      });
      if (hasNewLog) _scrollLogToEnd();
      // 原程序在 sendLHBleData 里对每条下发指令都调用 insertLog。
      if (name == 'commandStarted') {
        unawaited(_uploadLog(event['command']?.toString() ?? ''));
      }
      // 装瓶换连接流程：连接成功回调按 MAC 前缀分派（原程序 L2982-2993）
      if (name == 'ready') {
        unawaited(_onLinkReady());
      }
      if (name == 'unsolicitedResponse') {
        unawaited(_onUnsolicitedResponse(event['text']?.toString().trim() ?? ''));
      }
    }, onError: (_) {});
  }

  @override
  void dispose() {
    _eventSubscription?.cancel();
    _channel.invokeMethod('stopScan');
    _channel.invokeMethod('disconnect');
    _addressController.dispose();
    _rawCommandController.dispose();
    _logController.dispose();
    _deviceSearchController.dispose();
    super.dispose();
  }

  // ==========================================================================
  // 基础通道封装
  // ==========================================================================
  Future<dynamic> _call(String method, [Map<String, dynamic>? arguments]) async {
    try {
      return await _channel.invokeMethod(method, arguments);
    } on PlatformException catch (error) {
      if (mounted) _toast(error.message ?? error.code);
      return null;
    }
  }

  void _scrollLogToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_logController.hasClients) return;
      _logController.animateTo(
        _logController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    });
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), duration: const Duration(seconds: 3)),
    );
  }

  // ==========================================================================
  // 连接管理
  // ==========================================================================
  Future<bool> _ensurePermissions() async {
    if (!Platform.isAndroid) return true;
    final statuses = await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.locationWhenInUse,
    ].request();
    final sdk = (await DeviceInfoPlugin().androidInfo).version.sdkInt;
    final required = sdk >= 31
        ? [statuses[Permission.bluetoothScan], statuses[Permission.bluetoothConnect]]
        : [statuses[Permission.locationWhenInUse]];
    return !required.any(
      (status) => status == null || status.isPermanentlyDenied || status.isDenied,
    );
  }

  Future<void> _scan() async {
    if (!await _ensurePermissions()) {
      _toast('请允许附近设备/位置权限后再扫描');
      return;
    }
    if (!mounted) return;
    setState(() => _devices.clear());
    _deviceSearchController.clear();
    unawaited(_call('startScan', {'durationMs': 10000}));
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            _scanDialogSetState = setDialogState;
            final query = _deviceSearchController.text.trim().toLowerCase();
            final devices = _devices.where((device) {
              if (query.isEmpty) return true;
              final name = device['name']?.toString().toLowerCase() ?? '';
              final address = device['address']?.toString().toLowerCase() ?? '';
              return name.contains(query) || address.contains(query);
            }).toList();
            return AlertDialog(
              title: Row(
                children: [
                  const Expanded(child: Text('选择采样箱设备')),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.of(dialogContext).pop(),
                  ),
                ],
              ),
              content: SizedBox(
                width: 460,
                height: 380,
                child: Column(
                  children: [
                    TextField(
                      controller: _deviceSearchController,
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.search),
                        hintText: '按名称或地址模糊搜索',
                      ),
                      onChanged: (_) => setDialogState(() {}),
                    ),
                    const SizedBox(height: 10),
                    Expanded(
                      child: devices.isEmpty
                          ? Center(
                              child: Text(
                                _devices.isEmpty ? '正在扫描采样箱…' : '没有匹配的采样箱设备',
                              ),
                            )
                          : ListView.separated(
                              itemCount: devices.length,
                              separatorBuilder: (_, __) => const Divider(height: 1),
                              itemBuilder: (_, index) {
                                final device = devices[index];
                                final name =
                                    (device['name']?.toString().trim().isNotEmpty == true)
                                        ? device['name'].toString()
                                        : '采样箱';
                                final address = device['address']?.toString() ?? '';
                                final isCyx7 = device['isCyx7'] == true;
                                return ListTile(
                                  leading: const Icon(Icons.inventory_2_outlined),
                                  title: Text(name),
                                  subtitle: Text(
                                    '$address    RSSI ${device['rssi'] ?? '-'}'
                                    '${isCyx7 ? '    CYX7' : ''}',
                                  ),
                                  onTap: () {
                                    Navigator.of(dialogContext).pop();
                                    _connect(address);
                                  },
                                );
                              },
                            ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('取消'),
                ),
              ],
            );
          },
        );
      },
    );
    _scanDialogSetState = null;
    _devices.clear();
    await _call('stopScan');
  }

  Future<void> _connect([String? address]) async {
    final input = (address ?? _addressController.text).trim();
    if (input.isEmpty) {
      await _editText(
        title: '连接采样箱',
        label: '蓝牙地址（0D 开头的 12 位十六进制）',
        initial: _addressController.text,
        onConfirm: (value) => _connect(value),
      );
      return;
    }
    setState(() => _status = '连接中');
    final ok = await _call('connect', {'address': input});
    if (!mounted) return;
    if (ok == true) {
      _addressController.text = input.toUpperCase();
      Cyx7Log.blueMac = input.replaceAll(':', '').replaceAll('-', '').toUpperCase();
      setState(() => _status = '已连接');
      await _refresh();
    } else {
      setState(() => _status = '未连接');
    }
  }

  Future<void> _disconnect() async {
    await _call('disconnect');
    if (!mounted) return;
    setState(() {
      _status = '未连接';
      _basic = null;
      _proInfo = null;
      _samInf = null;
    });
  }

  // ==========================================================================
  // 读取：Read All / Read ProInfo / Read SamInf / Read CID / Read SamPoint
  // ==========================================================================
  Future<void> _refresh() async {
    if (!_connected) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在读取采样箱信息…';
    });
    final basic = await _call('readAll');
    final proInfo = await _call('readProInfo');
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (basic is Map) {
        _basic = Map<String, dynamic>.from(basic);
        final bleAddress = basic['bleAddress']?.toString().trim() ?? '';
        if (bleAddress.isNotEmpty) _addressController.text = bleAddress;
        _syncLogBlueMac();
      }
      if (proInfo is Map) {
        _proInfo = Map<String, dynamic>.from(proInfo);
        _syncTriggerBits(_proInfo?['triggerBits']);
      }
    });
  }

  void _syncTriggerBits(dynamic bits) {
    if (bits is List) {
      _triggerBits = List<bool>.generate(
        16,
        (index) => index < bits.length ? bits[index] == true : false,
      );
    }
  }

  Future<void> _readPlan() async {
    final response = await _call('readPlan');
    if (!mounted || response is! Map) return;
    final segments = response['segments'];
    if (segments is List) {
      setState(() {
        _plan = segments
            .map((segment) => Map<String, dynamic>.from(segment as Map))
            .toList();
      });
    }
    _toast('已读取定时采样计划');
  }

  Future<void> _readCid() async {
    if (!_connected) return;
    setState(() => _busy = true);
    final response = await _call('readCid');
    if (!mounted) return;
    setState(() => _busy = false);
    if (response is Map) {
      final iccid = response['iccid']?.toString() ?? '';
      setState(() => _basic = {...?_basic, 'iccid': iccid});
      _toast(iccid.isEmpty ? '未读取到物联网卡号' : '物联网卡：$iccid');
    }
  }

  Future<void> _readSamInf() async {
    if (!_connected) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在读取现场参数…';
    });
    final response = await _call('readSamInf');
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (response is Map) _samInf = Map<String, dynamic>.from(response);
    });
  }

  // ==========================================================================
  // 写入：设备信息区
  // ==========================================================================
  Future<void> _editSysTime() async {
    final basic = _basic ?? const <String, dynamic>{};
    final current = basic['systemTimeRaw']?.toString() ?? '';
    var value = current.replaceAll(RegExp(r'[^0-9]'), '');
    if (value.length != 12) value = _nowProtocolTime();
    await _editText(
      title: '系统时间',
      label: '格式 yyMMddHHmmss',
      initial: value,
      keyboardType: TextInputType.number,
      onConfirm: (text) async {
        final ok = await _send('writeSysTime', {'value': text});
        if (ok) await _refresh();
      },
    );
  }

  Future<void> _editEffective() async {
    await _editText(
      title: '有效时刻',
      label: '格式 yyMMddHHmmss',
      initial: _nowProtocolTime(),
      keyboardType: TextInputType.number,
      onConfirm: (text) => _send('writeEffective', {'value': text}),
    );
  }

  Future<void> _setAlarm(bool enabled) async {
    final ok = await _send('writeAlarm', {'enabled': enabled});
    if (ok) setState(() => _alarmEnabled = enabled);
  }

  Future<void> _setGnss(bool enabled) async {
    final ok = await _send('writeGnss', {'enabled': enabled});
    if (ok) setState(() => _proInfo = {...?_proInfo, 'gnss': enabled ? 1 : 0});
  }

  Future<void> _selectWorkMode() async {
    final selected = await _select<int>(
      title: '选择工作模式',
      options: _sampleModes,
      current: _sampleModeCode,
    );
    if (selected == null) return;
    final ok = await _send('writeWorkMode', {'mode': selected});
    if (ok) {
      setState(() => _proInfo = {...?_proInfo, 'sampleMode': selected.toString().padLeft(2, '0')});
      if (selected == 13) {
        // 疾控模式：先清空采样计划再切换（与原程序一致）。
        await _send('writePlan', {
          'segments': List.generate(
            6,
            (index) => {'index': index + 1, 'enabled': false, 'hour': 0, 'minute': 0},
          ),
        });
      }
      await _refresh();
    }
  }

  int get _sampleModeCode {
    final raw = _proInfo?['sampleMode']?.toString() ?? '';
    return int.tryParse(raw.replaceAll(RegExp(r'[^0-9]'), '')) ?? -1;
  }

  // ==========================================================================
  // 写入：网络参数
  // ==========================================================================
  Future<void> _editHost() async {
    await _editText(
      title: '212 平台地址',
      label: 'IP 或域名',
      initial: _basic?['host']?.toString() ?? '',
      onConfirm: (value) => _send('writeNet', {'host': value}),
    );
  }

  Future<void> _editPort() async {
    await _editNumber(
      title: '212 平台端口',
      initial: double.tryParse(_basic?['port']?.toString() ?? '') ?? 0,
      onConfirm: (value) => _send('writePort', {'port': value.toInt()}),
    );
  }

  Future<void> _editCypIpPort() async {
    final cyp = _proInfo?['cypIpPort'];
    final host = cyp is Map ? cyp['host']?.toString() ?? '' : '';
    final port = cyp is Map ? int.tryParse(cyp['port']?.toString() ?? '') ?? 0 : 0;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final hostController = TextEditingController(text: host);
        final portController = TextEditingController(text: port == 0 ? '' : '$port');
        return AlertDialog(
          title: const Text('采样通道地址（212 平台）'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: hostController,
                decoration: const InputDecoration(labelText: 'IP 或域名（12 位）'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: portController,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: '端口（5 位）'),
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
                final value = int.tryParse(portController.text.trim()) ?? 0;
                if (value <= 0) {
                  _toast('端口不合法');
                  return;
                }
                Navigator.of(dialogContext).pop();
                await _send('writeCypIpPort', {
                  'host': hostController.text.trim(),
                  'port': value,
                });
              },
              child: const Text('写入'),
            ),
          ],
        );
      },
    );
  }

  /// 扫码设置箱体蓝牙地址（原程序：必须 `0D` 开头 12 位）。
  Future<void> _scanBoxMac() async {
    final code = await Cyx7Scan.scan(context);
    if (code == null) {
      _toast('未读取到二维码信息');
      return;
    }
    if (!Cyx7Scan.isBoxCode(code)) {
      _toast('二维码非力合采样箱标准码（需 0D 开头 12 位）');
      return;
    }
    await _send('writeBleAddr', {'value': code});
  }

  Future<void> _editBleAddr() async {
    await _editText(
      title: '箱体蓝牙地址',
      label: '0D 开头的 12 位十六进制',
      initial: _basic?['bleAddress']?.toString() ?? '',
      onConfirm: (value) => _send('writeBleAddr', {'value': value}),
    );
  }

  // ==========================================================================
  // 写入：采样参数
  // ==========================================================================
  Future<void> _editJyTime() async {
    final current = (_proInfo?['jyTime'] as num?)?.toInt() ?? 0;
    await _editNumber(
      title: '采样高程（米）',
      initial: _heightFromJyTime(current),
      decimals: 2,
      onConfirm: (height) => _send('writeJyTime', {'seconds': _jyTimeFromHeight(height)}),
    );
  }

  Future<void> _editValveTime() async {
    final current = (_proInfo?['jyTime'] as num?)?.toInt() ?? 301;
    await _editNumber(
      title: '进样阀值（脉冲）',
      initial: current.toDouble().clamp(301, 6000),
      onConfirm: (value) => _send('writeJyTime', {'seconds': value.toInt()}),
    );
  }

  Future<void> _editPyTime() async {
    await _editNumber(
      title: '排样时长（秒）',
      initial: ((_proInfo?['pyTime'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writePyTime', {'seconds': value.toInt()}),
    );
  }

  Future<void> _editWashTime() async {
    await _editNumber(
      title: '润洗时长（秒）',
      initial: ((_proInfo?['washTime'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writeWashTime', {'seconds': value.toInt()}),
    );
  }

  Future<void> _editPipeLen() async {
    await _editNumber(
      title: '采样管长（米）',
      initial: (_proInfo?['pipeLen'] as num?)?.toDouble() ?? 0,
      decimals: 2,
      onConfirm: (value) => _send('writePipeLen', {'meters': value}),
    );
  }

  Future<void> _editCapacity() async {
    await _editNumber(
      title: '采样管容量（mL）',
      initial: ((_proInfo?['capacity'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writeCapacity', {'ml': value.toInt()}),
    );
  }

  Future<void> _editSamflow() async {
    final top = (_proInfo?['topCoefficient'] as num?)?.toDouble() ?? 1;
    final bottom = (_proInfo?['bottomCoefficient'] as num?)?.toDouble() ?? 1;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final topController = TextEditingController(text: _format(top));
        final bottomController = TextEditingController(text: _format(bottom));
        return AlertDialog(
          title: const Text('采样流量修正系数'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: topController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: '上层系数'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: bottomController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: '下层系数'),
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
                final topValue = double.tryParse(topController.text.trim());
                final bottomValue = double.tryParse(bottomController.text.trim());
                if (topValue == null || bottomValue == null) {
                  _toast('请输入合法系数');
                  return;
                }
                Navigator.of(dialogContext).pop();
                await _send('writeSamflow', {'top': topValue, 'bottom': bottomValue});
              },
              child: const Text('写入'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _editCyNum() async {
    await _editNumber(
      title: '等比例采样次数',
      initial: ((_proInfo?['cyNum'] as num?)?.toInt() ?? 1).toDouble(),
      onConfirm: (value) => _send('writeCyNum', {'count': value.toInt()}),
    );
  }

  // ==========================================================================
  // 写入：定时采样计划
  // ==========================================================================
  Future<void> _editPlan() async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final edited = _plan
            .map((segment) => Map<String, dynamic>.from(segment))
            .toList();
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('定时采样计划（6 段）'),
              content: SizedBox(
                width: 420,
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: edited.length,
                  itemBuilder: (_, index) {
                    final segment = edited[index];
                    final enabled = segment['enabled'] == true;
                    final hour = (segment['hour'] as num?)?.toInt() ?? 0;
                    final minute = (segment['minute'] as num?)?.toInt() ?? 0;
                    return Row(
                      children: [
                        SizedBox(width: 56, child: Text('第 ${index + 1} 段')),
                        Switch(
                          value: enabled,
                          onChanged: (value) =>
                              setDialogState(() => segment['enabled'] = value),
                        ),
                        TextButton(
                          onPressed: enabled
                              ? () async {
                                  final time = await showTimePicker(
                                    context: context,
                                    initialTime: TimeOfDay(hour: hour, minute: minute),
                                  );
                                  if (time == null) return;
                                  setDialogState(() {
                                    segment['hour'] = time.hour;
                                    segment['minute'] = time.minute;
                                  });
                                }
                              : null,
                          child: Text(
                            '${hour.toString().padLeft(2, '0')}:${minute.toString().padLeft(2, '0')}',
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () async {
                    Navigator.of(dialogContext).pop();
                    final ok = await _send('writePlan', {'segments': edited});
                    if (ok) {
                      setState(() => _plan = edited);
                      await _readPlan();
                    }
                  },
                  child: const Text('写入'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  // ==========================================================================
  // 写入：触发条件与阈值
  // ==========================================================================
  Future<void> _editTrigger() async {
    final bits = List<bool>.from(_triggerBits);
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('触发条件'),
              content: SizedBox(
                width: 420,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: _triggerLabels.entries.map((entry) {
                      return CheckboxListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text(entry.value),
                        value: bits[entry.key],
                        onChanged: (value) =>
                            setDialogState(() => bits[entry.key] = value ?? false),
                      );
                    }).toList(),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () async {
                    Navigator.of(dialogContext).pop();
                    final bitmap =
                        bits.map((value) => value ? '1' : '0').join();
                    final ok = await _send('writeTrigger', {'bitmap': bitmap});
                    if (ok) setState(() => _triggerBits = bits);
                  },
                  child: const Text('写入'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  bool _triggerEnabled(int index) => index < _triggerBits.length && _triggerBits[index];

  Future<void> _editPhTh() async {
    final min = (_proInfo?['phThMin'] as num?)?.toDouble() ?? 0;
    final max = (_proInfo?['phThMax'] as num?)?.toDouble() ?? 14;
    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        final minController = TextEditingController(text: _format(min));
        final maxController = TextEditingController(text: _format(max));
        return AlertDialog(
          title: const Text('pH 上下限'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: minController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: '下限'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: maxController,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(labelText: '上限'),
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
                final minValue = double.tryParse(minController.text.trim());
                final maxValue = double.tryParse(maxController.text.trim());
                if (minValue == null || maxValue == null) {
                  _toast('请输入合法数值');
                  return;
                }
                Navigator.of(dialogContext).pop();
                await _send('writePhTh', {'min': minValue, 'max': maxValue});
              },
              child: const Text('写入'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _editThreshold(String kind, String title, double current, String unit) async {
    await _editNumber(
      title: '$title阈值（$unit）',
      initial: current,
      decimals: 2,
      onConfirm: (value) => _send('writeThreshold', {'kind': kind, 'value': value}),
    );
  }

  Future<void> _editRainNum() async {
    await _editNumber(
      title: '雨量阈值（mm）',
      initial: (_proInfo?['rainNum'] as num?)?.toDouble() ?? 0,
      decimals: 1,
      onConfirm: (value) => _send('writeRainNum', {'mm': value}),
    );
  }

  Future<void> _editWaterHeight() async {
    await _editNumber(
      title: '水位阈值（m）',
      initial: (_proInfo?['waterHeight'] as num?)?.toDouble() ?? 0,
      decimals: 3,
      onConfirm: (value) => _send('writeWaterHeight', {'meters': value}),
    );
  }

  Future<void> _editBatLow() async {
    final current = ((_proInfo?['batLow'] as num?)?.toInt() ?? 1000) / 100;
    await _editNumber(
      title: '低压阈值（V）',
      initial: current,
      decimals: 2,
      onConfirm: (value) => _send('writeBatLow', {'volts': value}),
    );
  }

  Future<void> _editCurrentThreshold() async {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    await _editThreshold('flow', '瞬时流量', (proInfo['flowTh'] as num?)?.toDouble() ?? 0, 'm³/s');
  }

  Future<void> _editVin() async {
    await _editNumber(
      title: '电压校正值（V）',
      initial: (_basic?['batteryVoltage'] as num?)?.toDouble() ?? 12,
      decimals: 2,
      onConfirm: (value) => _send('writeVin', {'volts': value}),
    );
  }

  // ==========================================================================
  // 写入：周期
  // ==========================================================================
  Future<void> _editCycleTime() async {
    await _editNumber(
      title: '采样周期（分钟）',
      initial: ((_proInfo?['cycleTime'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writeCycleTime', {'minutes': value.toInt()}),
    );
  }

  Future<void> _editRevpd() async {
    await _editNumber(
      title: '同步周期（分钟）',
      initial: ((_proInfo?['requestCycle'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writeRevpd', {'minutes': value.toInt()}),
    );
  }

  Future<void> _editReppd() async {
    await _editNumber(
      title: '上传周期（分钟）',
      initial: ((_basic?['uploadInterval'] as num?)?.toInt() ?? 0).toDouble(),
      onConfirm: (value) => _send('writeReppd', {'minutes': value.toInt()}),
    );
  }

  // ==========================================================================
  // 设备控制
  // ==========================================================================
  Future<void> _unlock() async {
    if (await _send('sendCommand', {'command': 'll'})) _toast('已发送开锁指令');
  }

  Future<void> _resetDevice() async {
    final confirmed = await _confirm('设备复位', '确认要对采样箱执行复位（ln）吗？');
    if (!confirmed) return;
    if (await _send('sendCommand', {'command': 'ln'})) _toast('已发送复位指令');
  }

  Future<void> _sysReset() async {
    final confirmed = await _confirm('重启（急停）', '确认要重启采样箱吗？正在执行的动作会被中断。');
    if (!confirmed) return;
    await _send('sendCommand', {'command': 'Set SysReset'});
  }

  Future<void> _pumpControl() async {
    final mode = await _select<int>(
      title: '水泵控制',
      options: const {0: '停止', 1: '正转', 2: '反转'},
      current: 0,
    );
    if (mode == null) return;
    if (mode != 0) {
      final confirmed = await _confirm(
        '水泵控制',
        '启动水泵可能造成蹦管或空转，确认继续（${mode == 1 ? '正转' : '反转'}）？',
      );
      if (!confirmed) return;
    }
    await _send('sendCommand', {'command': 'Write bump=$mode'});
  }

  Future<void> _pumpStop() async {
    await _send('sendCommand', {'command': 'Write bump=0'});
  }

  /// 「现场参数」入口：读取参数数据（Read SamInf）/ 参数校准配置（Write DIY）。
  /// 对应原程序 `tv_read_data` 的 SelecterObjectDailog 分支（L808-821）。
  Future<void> _openFieldParams() async {
    final action = await _select<String>(
      title: '操作选择',
      options: const {
        'read': '读取参数数据（Read SamInf）',
        'calib': '参数校准配置（ModBus）',
      },
      current: null,
    );
    if (action == null) return;
    if (action == 'read') {
      await _readSamInf();
    } else {
      context.push('/cyx7/param-set');
    }
  }

  // ==========================================================================
  // 瓶位操作
  // ==========================================================================
  /// 瓶位操作：卸瓶 / 采样 / 装瓶 / 禁用 / 启用 / 留样（LiherCYX7Activity L1440-1525）
  Future<void> _bottleAction(int index) async {
    if (!_connected) return;
    final banned = _isBanned(index);
    final action = await _select<String>(
      title: '瓶位 ${index + 1} 操作',
      options: {
        'sample': '采样（Write samnum）',
        'install': '装瓶（Write Subaddr + 样品编号）',
        'uninstall': '卸瓶（Write Subaddr）',
        'banned': banned ? '启用（Write Banned=0）' : '禁用（Write Banned=1）',
        'hold': '留样（Write Banned=2）',
      },
      current: null,
    );
    if (action == null) return;
    switch (action) {
      case 'sample':
        await _sampleBottle(index);
        break;
      case 'install':
        await _installBottle(index);
        break;
      case 'uninstall':
        await _uninstallBottle(index);
        break;
      case 'banned':
        await _setBanned(index, banned ? '0' : '1');
        break;
      case 'hold':
        await _setBanned(index, '2');
        break;
    }
  }

  String get _currentBottleCode =>
      (_proInfo?['currentBottle']?.toString() ?? '').trim().padLeft(2, '0');

  bool _isBottleBusy(int index) {
    final currentNum = (_proInfo?['currentNum'] as num?)?.toInt() ?? -1;
    final cyNum = (_proInfo?['cyNum'] as num?)?.toInt() ?? 0;
    return currentNum > 0 && cyNum > 1 && currentNum < cyNum;
  }

  /// 原程序：当前采样瓶且状态"工作中"时不能手动采样；等比例未完成时也不能。
  Future<void> _sampleBottle(int index) async {
    final bottleNo = index + 1;
    if (_currentBottleCode == bottleNo.toString().padLeft(2, '0') && _statusText() == '工作中') {
      _toast('目前正在采样中，无法手动采样！');
      return;
    }
    if (_isBottleBusy(index)) {
      _toast('当前等比例采样未完成，无法手动采样！');
      return;
    }
    if ((_proInfo?['llj'] as num?)?.toInt() == 1) {
      _toast('液位状态为无水，采样可能失败');
    }
    _toast('发起 $bottleNo 号瓶采样');
    await _send('writeSamNum', {'bottle': bottleNo});
  }

  /// 装瓶（完全对齐原程序 L3035-3058 / L2982-2993 / L2360-2389 / L1168-1178）：
  /// 1. 扫码取瓶码：必须 12 位且 `0B`/`0C` 开头，否则提示"二维码，非力合蓝牙瓶标准码"
  /// 2. 断开箱体 → 提示"正在连接采样瓶..." → 连接采样瓶
  /// 3. 连瓶成功回调：500ms 后发 `ls` 判瓶阀
  /// 4. `ls` 应答：含 `2` → 回连箱体；含 `1` → 确认后 `lc` 关阀；其它 → 状态异常
  /// 5. `lc` 应答：`olc` → 回连箱体；`lc` → 提示等待，收到 `olc` 再回连
  /// 6. 回连箱体成功回调：弹出「装瓶样品信息」录入样品编号 → `Write Subaddr=00<瓶号><瓶MAC>-<16位样品编号>`
  Future<void> _installBottle(int index) async {
    final code = await Cyx7Scan.scan(context);
    if (code == null) {
      _toast('未读取到二维码信息');
      return;
    }
    if (!Cyx7Scan.isBottleCode(code)) {
      _toast('二维码，非力合蓝牙瓶标准码');
      return;
    }
    final boxAddress = (_basic?['bleAddress']?.toString() ?? _addressController.text).trim();
    if (boxAddress.isEmpty) {
      _toast('未获取到箱体蓝牙地址，请先刷新后再装瓶');
      return;
    }
    _installBottleIndex = index;
    _installBottleCode = code;
    _boxAddressCache = boxAddress;
    _bottleStage = _BottleStage.connectingBottle;
    await _call('stopScan');
    await _call('disconnect');
    if (!mounted) return;
    _toast('正在连接采样瓶...');
    await _call('connect', {'address': code});
  }

  /// 连接成功后的分派（对应原程序 connectBleMac 成功回调里的 MAC 前缀分支）。
  Future<void> _onLinkReady() async {
    switch (_bottleStage) {
      case _BottleStage.connectingBottle:
        // 0B/0C：连上采样瓶后 500ms 发 ls
        await Future<void>.delayed(const Duration(milliseconds: 500));
        if (mounted) await _readBottleValve();
        break;
      case _BottleStage.reconnectingBox:
        // 0D：回连箱体成功后弹出样品编号录入
        _bottleStage = _BottleStage.idle;
        await _promptSampleNo();
        break;
      case _BottleStage.idle:
      case _BottleStage.waitingValveClosed:
        break;
    }
  }

  /// `ls` 判瓶阀（原程序 Return_Read_LS）。
  Future<void> _readBottleValve() async {
    final result = await _call('sendCommand', {'command': 'ls'});
    final text = result is Map ? result['response']?.toString().trim() ?? '' : '';
    if (text.contains('2')) {
      _toast('读取瓶信息成功...');
      await _reconnectBox();
    } else if (text.contains('1')) {
      final confirmed = await _confirm('关闭瓶阀', '样瓶阀门未关，请确认关闭瓶阀后才可装瓶');
      if (!confirmed) {
        _bottleStage = _BottleStage.idle;
        return;
      }
      await _closeBottleValve();
    } else {
      _toast('$text,样瓶阀门状态异常无法装瓶！');
      _bottleStage = _BottleStage.idle;
    }
  }

  /// `lc` 关阀（原程序 Return_BLE_CloseCap）。
  Future<void> _closeBottleValve() async {
    final result = await _call('sendCommand', {'command': 'lc'});
    final text = result is Map ? result['response']?.toString().trim() ?? '' : '';
    if (text == 'olc') {
      _toast('阀门关闭成功');
      await _reconnectBox();
    } else if (text == 'lc') {
      // 中间应答：继续等待设备随后推送的 olc
      _toast('正在关闭瓶阀，请稍后...');
      _bottleStage = _BottleStage.waitingValveClosed;
    } else {
      _toast('$text样瓶关阀失败，无法装瓶！');
      _bottleStage = _BottleStage.idle;
    }
  }

  /// 主动上报/主动推送的应答（等待 olc 时用）。
  Future<void> _onUnsolicitedResponse(String text) async {
    if (_bottleStage != _BottleStage.waitingValveClosed) return;
    if (text == 'olc') {
      _toast('阀门关闭成功');
      await _reconnectBox();
    } else if (text.isNotEmpty && text != 'lc') {
      _toast('$text样瓶关阀失败，无法装瓶！');
      _bottleStage = _BottleStage.idle;
    }
  }

  Future<void> _reconnectBox() async {
    _bottleStage = _BottleStage.reconnectingBox;
    await _call('disconnect');
    if (!mounted) return;
    await _call('connect', {'address': _boxAddressCache});
  }

  /// 回连箱体成功后录入样品编号（原程序 L2991 / L1168-1178）。
  Future<void> _promptSampleNo() async {
    final index = _installBottleIndex;
    final bottleCode = _installBottleCode;
    if (index == null || bottleCode == null) return;
    final sampleController = TextEditingController();
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('装瓶样品信息'),
        content: TextField(
          controller: sampleController,
          autofocus: true,
          decoration: const InputDecoration(labelText: '样品编号'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () async {
              final sampleNo = sampleController.text.trim();
              if (sampleNo.length <= 2 || sampleNo.length >= 16) {
                _toast('样品编号长度需为 3~15 位');
                return;
              }
              Navigator.of(dialogContext).pop();
              final ok = await _send('writeSampleNo', {
                'bottle': index + 1,
                'mac': bottleCode,
                'sampleNo': sampleNo,
              });
              if (ok) await _refresh();
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
    sampleController.dispose();
  }

  /// 卸瓶：采样中不允许；等比例未完成需二次确认"强制卸瓶确认"。
  Future<void> _uninstallBottle(int index) async {
    final bottleNo = index + 1;
    if (_currentBottleCode == bottleNo.toString().padLeft(2, '0') && _statusText() == '工作中') {
      _toast('目前正在采样中，无法卸瓶！');
      return;
    }
    if (_isBottleBusy(index)) {
      final currentNum = (_proInfo?['currentNum'] as num?)?.toInt() ?? 0;
      final cyNum = (_proInfo?['cyNum'] as num?)?.toInt() ?? 0;
      final forced = await _confirm(
        '强制卸瓶确认',
        '目前等比例采样 $currentNum/$cyNum 次还未完成，是否强制卸瓶？',
      );
      if (!forced) return;
      _pendingLogPrefix = '确认强制卸瓶,';
    }
    await _send('writeSubaddr', {'bottle': bottleNo});
  }

  bool _isBanned(int index) {
    final mask = (_proInfo?['lockBottles']?.toString() ?? '000000').padLeft(6, '0');
    return index < mask.length && mask[index] == '1';
  }

  /// 瓶位掩码：0 启用 / 1 禁用 / 2 留样。
  Future<void> _setBanned(int index, String value) async {
    final current = (_proInfo?['lockBottles']?.toString() ?? '000000').padLeft(6, '0');
    if (current.length != 6) {
      _toast('瓶位状态异常，请先刷新');
      return;
    }
    final mask = current.split('');
    mask[index] = value;
    final text = mask.join();
    final ok = await _send('writeBanned', {'mask': text});
    if (ok) setState(() => _proInfo = {...?_proInfo, 'lockBottles': text});
  }

  // ==========================================================================
  // 窗口模式
  // ==========================================================================
  Future<void> _sendRawCommand() async {
    final command = _rawCommandController.text.trim();
    if (command.isEmpty) {
      _toast('请输入要发送的指令');
      return;
    }
    _rawCommandController.clear();
    await _send('sendCommand', {'command': command});
  }

  // ==========================================================================
  // 采样日志上报
  // ==========================================================================

  /// 每条指令下发时上报，等价原 `LiherCYX7Activity.insertLog(blueMac, sampleId, logContent)`。
  ///
  /// 注意：原程序该页的 `sampleId` 字段初值为 `""` 且全程没有赋值（真正带样品编号的是
  /// 参数校准页/RTU 子页，它们各自用固定值 `LhRTULLJ` / `LhCYXRTU`），所以这里传空串。
  /// `blueMac` 为空不上报、失败静默，均与原程序一致。
  Future<void> _uploadLog(String command) async {
    if (command.isEmpty) return;
    _syncLogBlueMac();
    final content = _pendingLogPrefix.isEmpty ? command : '$_pendingLogPrefix$command';
    _pendingLogPrefix = '';
    await Cyx7Log.upload(sampleId: '', content: content);
  }

  /// 维护共享上报器里的设备蓝牙地址（原程序由 Intent 传给子页的 mDeviceAddress）。
  void _syncLogBlueMac() {
    final value = (_basic?['bleAddress']?.toString() ?? _addressController.text)
        .replaceAll(':', '')
        .replaceAll('-', '')
        .trim();
    if (value.isNotEmpty) Cyx7Log.blueMac = value;
  }

  Future<void> _editLogSettings() async {
    final urlController = TextEditingController(text: Cyx7Log.url);
    final tokenController = TextEditingController(text: Cyx7Log.token);
    final operatorController = TextEditingController(text: Cyx7Log.operator);
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('采样日志上报设置'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: urlController,
              decoration: const InputDecoration(labelText: '上报地址'),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: tokenController,
              decoration: const InputDecoration(
                labelText: 'Token（可留空，留空则不带鉴权头）',
              ),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: operatorController,
              decoration: const InputDecoration(labelText: '操作员 ID'),
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
              setState(() {
                Cyx7Log.url = urlController.text.trim().isEmpty
                    ? Cyx7Log.url
                    : urlController.text.trim();
                Cyx7Log.token = tokenController.text.trim();
                Cyx7Log.operator = operatorController.text.trim();
              });
              Navigator.of(dialogContext).pop();
              _toast('日志上报设置已保存');
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
  }

  // ==========================================================================
  // 指令下发统一入口
  // ==========================================================================
  Future<bool> _send(String method, Map<String, dynamic> arguments) async {
    if (!_connected) {
      _toast('请先连接采样箱');
      return false;
    }
    setState(() {
      _busy = true;
      _busyMessage = '正在处理采样箱指令…';
    });
    final response = await _call(method, arguments);
    if (!mounted) return false;
    setState(() => _busy = false);
    return response != null;
  }

  // ==========================================================================
  // 通用对话框
  // ==========================================================================
  Future<void> _editText({
    required String title,
    required String label,
    required String initial,
    required Future<void> Function(String value) onConfirm,
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
            decoration: InputDecoration(labelText: label),
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
    int decimals = 0,
  }) async {
    await _editText(
      title: title,
      label: '数值',
      initial: decimals == 0 ? initial.toInt().toString() : initial.toStringAsFixed(decimals),
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
  }) async {
    return showDialog<T>(
      context: context,
      builder: (dialogContext) {
        return SimpleDialog(
          title: Text(title),
          children: options.entries.map((entry) {
            return SimpleDialogOption(
              onPressed: () => Navigator.of(dialogContext).pop(entry.key),
              child: Row(
                children: [
                  Expanded(child: Text(entry.value)),
                  if (entry.key == current) const Icon(Icons.check, size: 18),
                ],
              ),
            );
          }).toList(),
        );
      },
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
  String _format(double value) {
    if (value == value.roundToDouble()) return value.toInt().toString();
    return value.toStringAsFixed(2);
  }

  String _nowProtocolTime() {
    final now = DateTime.now();
    String pad(int value) => value.toString().padLeft(2, '0');
    return '${pad(now.year % 100)}${pad(now.month)}${pad(now.day)}'
        '${pad(now.hour)}${pad(now.minute)}${pad(now.second)}';
  }

  double _heightFromJyTime(int jyTime) => jyTime <= 0 ? 0 : (jyTime - 10.787) / 2.9626;

  int _jyTimeFromHeight(double height) => (height * 2.9626 + 10.787).round();

  String _text(dynamic value, {String fallback = '-'}) {
    final text = value?.toString().trim() ?? '';
    return text.isEmpty ? fallback : text;
  }

  String _stepText() {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    final step = _text(proInfo['step']);
    final index = (proInfo['stepIndex'] as num?)?.toInt() ?? -999;
    final total = (proInfo['stepTotal'] as num?)?.toInt() ?? -999;
    if (index > 0 && total > 0) return '$step（第 $index/$total 步）';
    return step;
  }

  String _statusText() {
    final raw = _text(_basic?['deviceStatus']);
    return _deviceStatusText[raw] ?? raw;
  }

  String _lljText() {
    final value = (_proInfo?['llj'] as num?)?.toInt() ?? -998;
    return switch (value) {
      0 => '有水',
      1 => '无水',
      _ => '暂无',
    };
  }

  // ==========================================================================
  // UI
  // ==========================================================================
  @override
  Widget build(BuildContext context) {
    final basic = _basic ?? const <String, dynamic>{};
    final title = _text(basic['deviceName'], fallback: '井下采样器 CYX7');
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          onPressed: () => context.go('/more'),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(title),
        actions: [
          IconButton(
            tooltip: _connected ? '断开连接' : '扫描采样箱',
            onPressed: _busy ? null : (_connected ? _disconnect : _scan),
            icon: Icon(_connected ? Icons.bluetooth_connected : Icons.bluetooth_searching),
          ),
          IconButton(
            tooltip: '刷新',
            onPressed: _busy || !_connected ? null : _refresh,
            icon: const Icon(Icons.refresh),
          ),
          PopupMenuButton<String>(
            tooltip: '更多',
            onSelected: (value) {
              switch (value) {
                case 'window':
                  setState(() => _windowMode = !_windowMode);
                  break;
                case 'unlock':
                  _unlock();
                  break;
                case 'reset':
                  _resetDevice();
                  break;
                case 'sysReset':
                  _sysReset();
                  break;
                case 'vin':
                  _editVin();
                  break;
                case 'scanMac':
                  _scanBoxMac();
                  break;
                case 'rtu':
                  context.push('/cyx7/rtu');
                  break;
                case 'paramSet':
                  context.push('/cyx7/param-set');
                  break;
                case 'logSettings':
                  _editLogSettings();
                  break;
                case 'upgrade':
                  // 原程序同样未实现：提示先用 GRToolbox 升级
                  _toast('功能正在开发完善中，请先用GRToolbox软件升级');
                  break;
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'window',
                child: Text(_windowMode ? '切换到交互模式' : '切换到窗口模式'),
              ),
              const PopupMenuItem(value: 'rtu', child: Text('RTU 功能入口')),
              const PopupMenuItem(value: 'paramSet', child: Text('参数校准配置')),
              const PopupMenuItem(value: 'scanMac', child: Text('扫码设置 Mac地址')),
              const PopupMenuItem(value: 'logSettings', child: Text('采样日志上报设置')),
              const PopupMenuItem(value: 'unlock', child: Text('开锁（ll）')),
              const PopupMenuItem(value: 'reset', child: Text('设备复位（ln）')),
              const PopupMenuItem(value: 'sysReset', child: Text('重启/急停')),
              const PopupMenuItem(value: 'vin', child: Text('电压校正')),
              const PopupMenuItem(value: 'upgrade', child: Text('版本升级')),
            ],
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
            children: [
              if (!_connected) _connectCard(),
              _runtimeCard(),
              _deviceCard(),
              _stateCard(),
              _networkCard(),
              _sampleParamCard(),
              _planCard(),
              _triggerCard(),
              _cycleCard(),
              _progressCard(),
              _actionCard(),
              _bottleCard(),
              if (_windowMode) _windowCard(),
              _logCard(),
            ],
          ),
          if (_busy) _loadingOverlay(),
        ],
      ),
    );
  }

  Widget _connectCard() {
    return _card(
      '设备连接',
      [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _addressController,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: '蓝牙地址（0D 开头 12 位）',
                ),
              ),
            ),
            const SizedBox(width: 8),
            FilledButton(
              onPressed: _busy ? null : () => _connect(),
              child: const Text('连接'),
            ),
            const SizedBox(width: 8),
            OutlinedButton(
              onPressed: _busy ? null : _scan,
              child: const Text('扫描'),
            ),
          ],
        ),
        const SizedBox(height: 6),
        const Text(
          '井下采样器对应 MAC 前缀 0D:03 / 0D:07（采样箱 2026 兼容 RTU 程序）。',
          style: TextStyle(fontSize: 12, color: Colors.black54),
        ),
      ],
    );
  }

  Widget _runtimeCard() {
    final basic = _basic ?? const <String, dynamic>{};
    final voltage = (basic['batteryVoltage'] as num?)?.toDouble() ?? 0;
    final percent = (_proInfo?['batteryPercent'] as num?)?.toInt() ?? -1;
    return _card(
      '采样器运行状态',
      [
        Row(
          children: [
            Expanded(
              child: _kv('设备状态', _connected ? '在线' : '离线'),
            ),
            Expanded(
              child: _kv('电量', percent >= 0 ? '$percent%' : '-'),
            ),
            Expanded(
              child: _kv('电压', voltage > 0 ? '${voltage.toStringAsFixed(2)} V' : '-'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _deviceCard() {
    final basic = _basic ?? const <String, dynamic>{};
    return _card(
      '设备信息',
      [
        _row('设备名称', _text(basic['deviceName'])),
        _row('设备编号', _text(basic['deviceNumber'])),
        _row('当前版本', _text(basic['version'])),
        SwitchListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          title: const Text('报警功能'),
          value: _alarmEnabled,
          onChanged: _connected && !_busy ? _setAlarm : null,
        ),
        _row('系统时间', _text(basic['systemTime']), onTap: _connected ? _editSysTime : null),
        _row('运行状态', _statusText()),
        _row('蓝牙地址', _text(basic['bleAddress']), onTap: _connected ? _editBleAddr : null),
        _row('出厂编号', _text(basic['mn'])),
      ],
    );
  }

  Widget _stateCard() {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    final mode = _sampleModes[_sampleModeCode] ?? '-';
    return _card(
      '工作状态',
      [
        _row('工作模式', mode, onTap: _connected ? _selectWorkMode : null),
        _row('液位状态', _lljText()),
        _row(
          '物联网卡',
          _text(_basic?['iccid']),
          onTap: _connected ? _readCid : null,
        ),
        SwitchListTile(
          dense: true,
          contentPadding: EdgeInsets.zero,
          title: const Text('GPS 定位'),
          value: (proInfo['gnss'] as num?)?.toInt() == 1,
          onChanged: _connected && !_busy ? _setGnss : null,
        ),
      ],
    );
  }

  Widget _networkCard() {
    final cyp = _proInfo?['cypIpPort'];
    final cypHost = cyp is Map ? cyp['host']?.toString() ?? '-' : '-';
    final cypPort = cyp is Map ? cyp['port']?.toString() ?? '-' : '-';
    return _card(
      '更多参数配置',
      [
        _row('212 平台地址', _text(_basic?['host']), onTap: _connected ? _editHost : null),
        _row('212 平台端口', _text(_basic?['port']), onTap: _connected ? _editPort : null),
        _row('采样通道地址', _text(cypHost), onTap: _connected ? _editCypIpPort : null),
        _row('采样通道端口', _text(cypPort), onTap: _connected ? _editCypIpPort : null),
      ],
    );
  }

  Widget _sampleParamCard() {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    final jyTime = (proInfo['jyTime'] as num?)?.toInt() ?? 0;
    return _card(
      '采样参数',
      [
        _row(
          '采样高程',
          jyTime > 0 ? '${_heightFromJyTime(jyTime).toStringAsFixed(2)} m' : '-',
          onTap: _connected ? _editJyTime : null,
        ),
        _row('进样时长', jyTime > 300 ? '-（阀值模式）' : '$jyTime s'),
        _row(
          '进样阀值',
          jyTime > 300 ? '$jyTime 脉冲' : '-',
          onTap: _connected ? _editValveTime : null,
        ),
        _row('采样管长', '${_format((proInfo['pipeLen'] as num?)?.toDouble() ?? 0)} m', onTap: _connected ? _editPipeLen : null),
        _row('采样管容量', '${(proInfo['capacity'] as num?)?.toInt() ?? 0} mL', onTap: _connected ? _editCapacity : null),
        _row(
          '流量修正系数',
          '${_format((proInfo['topCoefficient'] as num?)?.toDouble() ?? 0)} / '
              '${_format((proInfo['bottomCoefficient'] as num?)?.toDouble() ?? 0)}',
          onTap: _connected ? _editSamflow : null,
        ),
        _row('排样时长', '${(proInfo['pyTime'] as num?)?.toInt() ?? 0} s', onTap: _connected ? _editPyTime : null),
        _row('润洗时长', '${(proInfo['washTime'] as num?)?.toInt() ?? 0} s', onTap: _connected ? _editWashTime : null),
        _row('等比例次数', '${(proInfo['cyNum'] as num?)?.toInt() ?? 0} 次', onTap: _connected ? _editCyNum : null),
      ],
    );
  }

  Widget _planCard() {
    final enabled = _plan.where((segment) => segment['enabled'] == true).toList();
    return _card(
      '定时采样计划',
      [
        _row('已启用段数', '${enabled.length} / 6'),
        if (enabled.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 4, bottom: 4),
            child: Text(
              enabled
                  .map((segment) =>
                      '${segment['index']}: ${(segment['hour'] as num).toInt().toString().padLeft(2, '0')}:${(segment['minute'] as num).toInt().toString().padLeft(2, '0')}')
                  .join('   '),
              style: const TextStyle(fontSize: 12.5),
            ),
          ),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _connected && !_busy ? _readPlan : null,
                child: const Text('读取计划'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: FilledButton(
                onPressed: _connected && !_busy ? _editPlan : null,
                child: const Text('编辑计划'),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _triggerCard() {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    return _card(
      '触发条件与阈值',
      [
        _row(
          '触发条件',
          _triggerBits.asMap().entries
              .where((entry) => entry.value && _triggerLabels.containsKey(entry.key))
              .map((entry) => _triggerLabels[entry.key]!)
              .join('、')
              .ifEmpty('-'),
          onTap: _connected ? _editTrigger : null,
        ),
        if (_triggerEnabled(1))
          _row('雨量阈值', '${_format((proInfo['rainNum'] as num?)?.toDouble() ?? 0)} mm', onTap: _connected ? _editRainNum : null),
        if (_triggerEnabled(0))
          _row('水位阈值', '${_format((proInfo['waterHeight'] as num?)?.toDouble() ?? 0)} m', onTap: _connected ? _editWaterHeight : null),
        if (_triggerEnabled(7))
          _row('瞬时流量阈值', '${_format((proInfo['flowTh'] as num?)?.toDouble() ?? 0)} m³/s', onTap: _connected ? _editCurrentThreshold : null),
        if (_triggerEnabled(2))
          _row(
            'pH 上下限',
            '${_format((proInfo['phThMin'] as num?)?.toDouble() ?? 0)} ~ ${_format((proInfo['phThMax'] as num?)?.toDouble() ?? 0)}',
            onTap: _connected ? _editPhTh : null,
          ),
        if (_triggerEnabled(3))
          _row('电导率阈值', '${_format((proInfo['condTh'] as num?)?.toDouble() ?? 0)} us/cm', onTap: _connected ? () => _editThreshold('cond', '电导率', (proInfo['condTh'] as num?)?.toDouble() ?? 0, 'us/cm') : null),
        if (_triggerEnabled(4))
          _row('浊度阈值', '${_format((proInfo['turbTh'] as num?)?.toDouble() ?? 0)} NTU', onTap: _connected ? () => _editThreshold('turb', '浊度', (proInfo['turbTh'] as num?)?.toDouble() ?? 0, 'NTU') : null),
        if (_triggerEnabled(5))
          _row('溶解氧阈值', '${_format((proInfo['oxygenTh'] as num?)?.toDouble() ?? 0)} mg/L', onTap: _connected ? () => _editThreshold('oxygen', '溶解氧', (proInfo['oxygenTh'] as num?)?.toDouble() ?? 0, 'mg/L') : null),
        if (_triggerEnabled(6))
          _row('水温阈值', '${_format((proInfo['waterTempTh'] as num?)?.toDouble() ?? 0)} °C', onTap: _connected ? () => _editThreshold('waterTemp', '水温', (proInfo['waterTempTh'] as num?)?.toDouble() ?? 0, '°C') : null),
        _row(
          '低压阈值',
          '${((proInfo['batLow'] as num?)?.toInt() ?? 0) / 100} V',
          onTap: _connected ? _editBatLow : null,
        ),
        _row(
          '同步周期',
          '${(proInfo['requestCycle'] as num?)?.toInt() ?? 0} 分钟',
          onTap: _connected ? _editRevpd : null,
        ),
      ],
    );
  }

  Widget _cycleCard() {
    return _card(
      '采样与上传周期',
      [
        _row('采样周期', '${(_proInfo?['cycleTime'] as num?)?.toInt() ?? 0} 分钟', onTap: _connected ? _editCycleTime : null),
        _row('上传周期', '${(_basic?['uploadInterval'] as num?)?.toInt() ?? 0} 分钟', onTap: _connected ? _editReppd : null),
        _row('有效时刻', '-', onTap: _connected ? _editEffective : null),
      ],
    );
  }

  Widget _progressCard() {
    final proInfo = _proInfo ?? const <String, dynamic>{};
    final percent = (_proInfo?['batteryPercent'] as num?)?.toInt() ?? -1;
    return _card(
      '采样进度',
      [
        _row('下次采样时间', _text(proInfo['nextSampleTime'])),
        _row('当前采样瓶', '${_text(proInfo['currentNum'])} / ${_text(proInfo['currentBottle'])}'),
        _row('当前进度', _stepText()),
        if (percent >= 0)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: LinearProgressIndicator(value: (percent / 100).clamp(0, 1)),
          ),
      ],
    );
  }

  Widget _actionCard() {
    return _card(
      '设备控制',
      [
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _openFieldParams,
                child: const Text('现场参数'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _pumpControl,
                child: const Text('水泵控制'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _pumpStop,
                child: const Text('水泵停止'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _unlock,
                child: const Text('开锁'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _resetDevice,
                child: const Text('设备复位'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : _sysReset,
                child: const Text('重启/急停'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : () => context.push('/cyx7/rtu'),
                child: const Text('RTU 功能入口'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: OutlinedButton(
                onPressed: _busy || !_connected ? null : () => context.push('/cyx7/param-set'),
                child: const Text('参数校准配置'),
              ),
            ),
          ],
        ),
        if (_samInf != null) ...[
          const SizedBox(height: 10),
          _samInfTable(_samInf!),
        ],
      ],
    );
  }

  Widget _samInfTable(Map<String, dynamic> info) {
    final rows = <List<String>>[
      ['水位', '${_format((info['waterLevel'] as num?)?.toDouble() ?? 0)} m'],
      ['水深', '${_format((info['depth'] as num?)?.toDouble() ?? 0)} m'],
      ['空高', '${_format((info['height'] as num?)?.toDouble() ?? 0)} m'],
      ['流速', '${_format((info['sectionVelocity'] as num?)?.toDouble() ?? 0)} m/s'],
      ['瞬时流量', '${_format((info['rateFlow'] as num?)?.toDouble() ?? 0)} m³/s'],
      ['累计流量', '${_format((info['cumulativeFlowTotal'] as num?)?.toDouble() ?? 0)} m³'],
      ['雨量', '${_format((info['rain'] as num?)?.toDouble() ?? 0)} mm'],
      ['pH', _format((info['ph'] as num?)?.toDouble() ?? 0)],
      ['溶解氧', '${_format((info['oxygen'] as num?)?.toDouble() ?? 0)} mg/L'],
      ['电导率', '${_format((info['conductivity'] as num?)?.toDouble() ?? 0)} us/cm'],
      ['浊度', '${_format((info['turbidity'] as num?)?.toDouble() ?? 0)} NTU'],
    ];
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.black12),
        borderRadius: BorderRadius.circular(8),
      ),
      padding: const EdgeInsets.all(10),
      child: Column(
        children: rows
            .map((row) => Row(
                  children: [
                    Expanded(child: Text(row[0], style: const TextStyle(fontSize: 12, color: Colors.black54))),
                    Text(row[1], style: const TextStyle(fontSize: 12.5)),
                  ],
                ))
            .toList(),
      ),
    );
  }

  Widget _bottleCard() {
    final bottles = _proInfo?['bottles'];
    final list = bottles is List ? bottles : const [];
    return _card(
      '采样瓶位',
      [
        if (list.isEmpty)
          const Text('未读取到瓶位信息，请先刷新', style: TextStyle(fontSize: 12.5, color: Colors.black54))
        else
          ...List.generate(list.length, (index) {
            final bottle = Map<String, dynamic>.from(list[index] as Map);
            final sampleId = bottle['sampleId']?.toString() ?? '';
            final sampleTime = bottle['sampleTime']?.toString() ?? '';
            final banned = _isBanned(index);
            return ListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              leading: Icon(
                banned ? Icons.block : Icons.inventory_2_outlined,
                color: banned ? Colors.grey : Theme.of(context).colorScheme.primary,
              ),
              title: Text('第 ${index + 1} 瓶'),
              subtitle: Text(
                banned
                    ? '已禁用'
                    : (sampleId.isEmpty ? '空位待装瓶' : '样品编号: $sampleId    $sampleTime'),
                style: const TextStyle(fontSize: 12),
              ),
              trailing: const Icon(Icons.chevron_right, size: 18),
              onTap: _connected && !_busy ? () => _bottleAction(index) : null,
            );
          }),
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
                decoration: const InputDecoration(hintText: '例如：Read All'),
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
          height: 180,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.black12),
            borderRadius: BorderRadius.circular(6),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child: _logLines.isEmpty
              ? const Align(
                  alignment: Alignment.topLeft,
                  child: Text(
                    '采样箱指令交互信息将在此显示',
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

  Widget _kv(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 11.5, color: Colors.black54)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
      ],
    );
  }

  Widget _loadingOverlay() {
    return Positioned.fill(
      child: ColoredBox(
        color: const Color(0x66000000),
        child: Center(
          child: Container(
            width: 210,
            padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(
                  width: 30,
                  height: 30,
                  child: CircularProgressIndicator(strokeWidth: 3),
                ),
                const SizedBox(height: 14),
                Text(_busyMessage, textAlign: TextAlign.center),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

extension _EmptyText on String {
  String ifEmpty(String fallback) => isEmpty ? fallback : this;
}
