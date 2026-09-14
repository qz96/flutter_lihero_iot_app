import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';

class RtuPage extends StatefulWidget {
  const RtuPage({super.key});

  @override
  State<RtuPage> createState() => _RtuPageState();
}

class _RtuPageState extends State<RtuPage> {
  static const _channel = MethodChannel('lihero_rtu');
  static const _events = EventChannel('lihero_rtu/events');

  StreamSubscription<dynamic>? _eventSubscription;
  final _addressController = TextEditingController();
  final _commandController = TextEditingController();
  final _commandLogController = ScrollController();
  final _mnController = TextEditingController();
  final _sampleIntervalController = TextEditingController();
  final _uploadIntervalController = TextEditingController();
  final _systemStController = TextEditingController();
  final _sensorWaitController = TextEditingController();
  final _systemTimeController = TextEditingController();
  final _lotTypeController = TextEditingController();
  final _sensorDeviceTypeController = TextEditingController();
  final _flowReadAddressController = TextEditingController(text: '40054');
  final _flowReadCountController = TextEditingController(text: '1');
  final _flowWriteAddressController = TextEditingController(text: '40054');
  final _flowWriteValueController = TextEditingController();
  final _networkControllers = List.generate(5, (_) => _NetworkControllers());
  final _devices = <Map<String, dynamic>>[];
  StateSetter? _scanDialogSetState;
  final _deviceSearchController = TextEditingController();

  Map<String, dynamic>? _basic;
  Map<String, dynamic>? _lot;
  Map<String, dynamic>? _sensor;
  String _sensorType = 'swj';
  String _activeTab = 'swj';
  String _status = '未连接';
  final _logLines = <String>[];
  bool _showDeviceDetails = true;
  bool _measuring = false;
  String? _measuringType;
  bool _sensorReadInFlight = false;
  // Set before cancelling the timer so a callback that is already queued in
  // the event loop cannot start another Read SamInf while upload is stopping
  // the measurement.
  bool _measurementStopRequested = false;
  bool _updating = false;
  bool _flowBaseConfigVisible = false;
  bool _flowConfigMoreVisible = false;
  final _flowRegisters = <int, int>{};
  String _busyMessage = '正在处理 RTU 指令…';
  final _networkAdded = List<bool>.filled(5, false);
  Timer? _measurementTimer;
  Timer? _uploadTimer;
  bool _busy = false;
  /// When true, BLE command events from the EventChannel are not written to
  /// the command log to avoid screen flickering from periodic reads and
  /// internal device-switch refreshes.
  bool _backgroundRead = false;

  bool get _connected => _status == '已连接';

  @override
  void initState() {
    super.initState();
    _systemTimeController.text = _formatDateTime(DateTime.now());
    _eventSubscription = _events.receiveBroadcastStream().listen((event) {
      if (!mounted || event is! Map) return;
      final name = event['event']?.toString() ?? '';
      // Suppress command-log entries during background reads (periodic sensor
      // polling and device-switch refreshes) to avoid screen flickering.
      if (_backgroundRead &&
          (name == 'commandStarted' ||
              name == 'response' ||
              name == 'commandResult')) {
        return;
      }
      final hasNewCommandLog =
          name == 'commandStarted' ||
          name == 'response' ||
          name == 'commandResult';
      setState(() {
        if (name == 'commandStarted') {
          _logLines.add('发送:${event['command'] ?? ''}');
        }
        if (name == 'response') {
          _logLines.add('接收:${event['text'] ?? ''}');
        }
        if (name == 'commandResult') {
          if (event['ok'] == true) {
            _logLines.add('成功:${event['response'] ?? 'RTU 已响应'}');
          } else {
            _logLines.add('失败:${event['error'] ?? ''}');
          }
        }
        if (_logLines.length > 80)
          _logLines.removeRange(0, _logLines.length - 80);
        if (name == 'ready') _status = '已连接';
        if (name == 'connecting') _status = '连接中';
        if (name == 'disconnected') {
          _status = '未连接';
          _busy = false;
          _measurementTimer?.cancel();
          _measuring = false;
          _measuringType = null;
          _measurementStopRequested = true;
        }
        if (name == 'scanResult') {
          final item = Map<String, dynamic>.from(event);
          if (item['isRtu'] == true &&
              !_devices.any((e) => e['address'] == item['address'])) {
            _devices.add(item);
            _scanDialogSetState?.call(() {});
          }
        }
      });
      if (hasNewCommandLog) _scrollCommandLogToEnd();
    }, onError: (_) {});
  }

  void _scrollCommandLogToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !_commandLogController.hasClients) return;
      _commandLogController.animateTo(
        _commandLogController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 180),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  void dispose() {
    _eventSubscription?.cancel();
    _measurementTimer?.cancel();
    _uploadTimer?.cancel();
    _channel.invokeMethod('stopScan');
    _channel.invokeMethod('disconnect');
    _deviceSearchController.dispose();
    _commandLogController.dispose();
    for (final c in _networkControllers) c.dispose();
    _addressController.dispose();
    _commandController.dispose();
    _mnController.dispose();
    _sampleIntervalController.dispose();
    _uploadIntervalController.dispose();
    _systemStController.dispose();
    _sensorWaitController.dispose();
    _systemTimeController.dispose();
    _lotTypeController.dispose();
    _sensorDeviceTypeController.dispose();
    _flowReadAddressController.dispose();
    _flowReadCountController.dispose();
    _flowWriteAddressController.dispose();
    _flowWriteValueController.dispose();
    super.dispose();
  }

  Future<dynamic> _call(
    String method, [
    Map<String, dynamic>? arguments,
  ]) async {
    try {
      return await _channel.invokeMethod(method, arguments);
    } on PlatformException catch (e) {
      if (mounted) _show('${e.message ?? e.code}');
      return null;
    }
  }

  Future<void> _scan() async {
    if (Platform.isAndroid) {
      final statuses =
          await [
            Permission.bluetoothScan,
            Permission.bluetoothConnect,
            Permission.locationWhenInUse,
          ].request();
      final sdk = (await DeviceInfoPlugin().androidInfo).version.sdkInt;
      final required =
          sdk >= 31
              ? [
                statuses[Permission.bluetoothScan],
                statuses[Permission.bluetoothConnect],
              ]
              : [statuses[Permission.locationWhenInUse]];
      if (required.any(
        (status) =>
            status == null || status.isPermanentlyDenied || status.isDenied,
      )) {
        _show('请允许附近设备/位置权限后再扫描');
        return;
      }
    }
    setState(() => _devices.clear());
    _deviceSearchController.clear();
    if (!mounted) return;
    // Start scanning before opening the modal; incoming scanResult events
    // update the dialog list in real time.
    unawaited(_call('startScan', {'durationMs': 10000}));
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            _scanDialogSetState = setDialogState;
            final query = _deviceSearchController.text.trim().toLowerCase();
            final devices =
                _devices.where((device) {
                  final name = device['name']?.toString() ?? '';
                  final address = device['address']?.toString() ?? '';
                  return query.isEmpty ||
                      name.toLowerCase().contains(query) ||
                      address.toLowerCase().contains(query);
                }).toList();
            return AlertDialog(
              title: Row(
                children: [
                  const Expanded(child: Text('选择 RTU 蓝牙设备')),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.of(dialogContext).pop(),
                  ),
                ],
              ),
              content: SizedBox(
                width: 460,
                height: 420,
                child: Column(
                  children: [
                    TextField(
                      controller: _deviceSearchController,
                      autofocus: true,
                      decoration: const InputDecoration(
                        prefixIcon: Icon(Icons.search),
                        hintText: '按名称或地址模糊搜索',
                        border: OutlineInputBorder(),
                      ),
                      onChanged: (_) => setDialogState(() {}),
                    ),
                    const SizedBox(height: 10),
                    Expanded(
                      child:
                          devices.isEmpty
                              ? Center(
                                child: Text(
                                  _devices.isEmpty
                                      ? '正在扫描 RTU 设备…'
                                      : '没有匹配的 RTU 设备',
                                ),
                              )
                              : ListView.separated(
                                itemCount: devices.length,
                                separatorBuilder:
                                    (_, __) => const Divider(height: 1),
                                itemBuilder: (_, index) {
                                  final device = devices[index];
                                  final name =
                                      (device['name']
                                                  ?.toString()
                                                  .trim()
                                                  .isNotEmpty ==
                                              true)
                                          ? device['name'].toString()
                                          : 'RTU 设备';
                                  final address =
                                      device['address']?.toString() ?? '';
                                  return ListTile(
                                    leading: const Icon(Icons.bluetooth),
                                    title: Text(name),
                                    subtitle: Text(
                                      '$address    RSSI ${device['rssi'] ?? '-'}',
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
    await _call('stopScan');
  }

  Future<void> _disconnect() async {
    await _call('disconnect');
    if (!mounted) return;
    setState(() {
      _status = '未连接';
      _basic = null;
      _lot = null;
      _sensor = null;
    });
  }

  Future<void> _connect([String? address]) async {
    if (Platform.isAndroid) {
      final sdk = (await DeviceInfoPlugin().androidInfo).version.sdkInt;
      if (sdk >= 31 &&
          !(await Permission.bluetoothConnect.request()).isGranted) {
        _show('请允许附近设备权限后再连接');
        return;
      }
    }
    final value = (address ?? _addressController.text)
        .trim()
        .replaceAll(':', '')
        .replaceAll('-', '');
    if (value.length != 12 ||
        !value.toUpperCase().startsWith('15') ||
        !RegExp(r'^[0-9A-Fa-f]{12}$').hasMatch(value)) {
      _show('请输入以 15 开头的 12 位 RTU 蓝牙地址');
      return;
    }
    _addressController.text = value.toUpperCase();
    setState(() => _status = '连接中');
    final ok = await _call('connect', {'address': value});
    if (ok == true) {
      if (mounted) setState(() => _status = '已连接');
      await _readAll();
    }
  }

  Future<void> _readAll() async {
    if (!_connected) return;
    // A periodic sensor read uses the same single BLE request/response slot
    // as the three refresh commands below.  Wait for that read to finish
    // before starting a refresh, otherwise Android correctly rejects the
    // second write as an overlapping RTU command.
    await _waitForSensorRead();
    if (!_connected || !mounted) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在读取 RTU 信息…';
    });
    final basic = await _call('readBasic');
    final lot = await _call('readLot');
    final sensor = await _call('readSensor', {'type': _sensorType});
    if (!mounted) return;
    setState(() {
      if (basic is Map) {
        _basic = Map<String, dynamic>.from(basic);
        _mnController.text = basic['mn']?.toString() ?? '';
        final systemTime = basic['systemTime']?.toString().trim() ?? '';
        if (systemTime.isNotEmpty) _systemTimeController.text = systemTime;
        final bleAddress = basic['bleAddress']?.toString().trim() ?? '';
        if (bleAddress.isNotEmpty) _addressController.text = bleAddress;
        _sensorDeviceTypeController.text =
            basic['sensorDeviceType']?.toString() ?? '';
      }
      if (lot is Map) {
        _lot = Map<String, dynamic>.from(lot);
        _lotTypeController.text =
            (lot['moduleType'] as num?)?.toInt().toString().padLeft(2, '0') ?? '';
        _sampleIntervalController.text =
            lot['sampleInterval']?.toString() ?? '';
        _uploadIntervalController.text =
            lot['uploadInterval']?.toString() ?? '';
        _systemStController.text = lot['systemSt']?.toString() ?? '';
        final services = lot['services'];
        if (services is List) {
          for (var i = 0; i < services.length && i < 5; i++) {
            final service = Map<String, dynamic>.from(services[i] as Map);
            _networkControllers[i].load(service);
            _networkAdded[i] =
                (service['enabled'] == true) ||
                (service['encodedHost']?.toString().isNotEmpty == true &&
                    service['encodedHost'] != '000000000000');
          }
        }
      }
      if (sensor is Map) {
        _sensor = Map<String, dynamic>.from(sensor);
        _sensorWaitController.text = sensor['sampleWait']?.toString() ?? '';
      }
      _busy = false;
    });
  }

  // The top-level RTU device switch only changes the RTU work state.  The
  // SampleBox refreshes RtuInf for this action; it does not start a sensor
  // read or refresh the whole sensor panel.  Keeping this path separate also
  // prevents one device-switch click from producing an unnecessary burst of
  // Read LotInf/Read SamInf log entries.
  Future<void> _readBasicOnly() async {
    if (!_connected) return;
    _backgroundRead = false;
    await _waitForSensorRead();
    if (!_connected || !mounted) return;
    setState(() {
      _busy = true;
      _busyMessage = '正在刷新 RTU 状态…';
    });
    try {
      final basic = await _call('readBasic');
      if (!mounted) return;
      setState(() {
        if (basic is Map) {
          _basic = Map<String, dynamic>.from(basic);
          _mnController.text = basic['mn']?.toString() ?? '';
          final systemTime = basic['systemTime']?.toString().trim() ?? '';
          if (systemTime.isNotEmpty) _systemTimeController.text = systemTime;
          _sensorDeviceTypeController.text =
              basic['sensorDeviceType']?.toString() ?? '';
        }
        _busy = false;
      });
    } finally {
      _backgroundRead = false;
    }
  }

  Future<void> _waitForSensorRead() async {
    // Keep the wait bounded by the BLE command timeout.  The in-flight flag
    // is cleared in _readSelectedSensor's finally block even on failures.
    for (var i = 0; i < 320 && _sensorReadInFlight; i++) {
      await Future<void>.delayed(const Duration(milliseconds: 50));
      if (!mounted || !_connected) return;
    }
  }

  String _loadingMessageFor(String method) {
    switch (method) {
      case 'start':
        return '正在开启设备…';
      case 'stop':
        return '正在关闭设备…';
      case 'upload':
        return '正在请求采集上报…';
      case 'zero':
        return '正在清空累计流量…';
      case 'setPower':
        return '正在设置长供电开关…';
      case 'upgradeBin':
        return '正在发送协议升级文件…';
      default:
        return '正在写入 RTU 配置…';
    }
  }

  Future<void> _action(String method, [Map<String, dynamic>? args]) async {
    if (!_connected) {
      _show('请先连接 RTU');
      return;
    }
    final isDeviceSwitch = args?['measurement'] == false;
    // Stop is special: cancel the timer and wait for an in-flight Read
    // SamInf before sending Set Stop.  Calling _call('stop') first would
    // race the existing request and be rejected by the BLE manager.
    if (method == 'stop' && _measuring) {
      final stopped = await _stopActiveMeasurement();
      if (!stopped || !mounted) return;
      if (isDeviceSwitch) {
        await _readBasicOnly();
      } else {
        await _readAll();
      }
      return;
    }
    if (method == 'upload' && _measuring) {
      final stopped = await _stopActiveMeasurement();
      if (!stopped || !mounted) return;
    }
    // 执行其他指令操作时，立即停止持续发送 Read SamInf
    if (_measuring) {
      _measurementTimer?.cancel();
      _measurementTimer = null;
      _measurementStopRequested = true;
      setState(() {
        _measuring = false;
        _measuringType = null;
      });
    }
    await _waitForSensorRead();
    if (!_connected || !mounted) return;
    setState(() {
      _busy = true;
      _busyMessage = _loadingMessageFor(method);
    });
    final result = await _call(method, args);
    if (!mounted) return;
    if (result is! Map || result['ok'] != true) {
      setState(() => _busy = false);
      return;
    }
    final startsSensorMeasurement = !isDeviceSwitch;
    if (method == 'start' && startsSensorMeasurement && mounted) {
      setState(() {
        _measuring = true;
        _measuringType = _sensorType;
        _measurementStopRequested = false;
      });
      _measurementTimer?.cancel();
      _measurementTimer = Timer.periodic(
        const Duration(seconds: 3),
        (_) => unawaited(_readSelectedSensor()),
      );
    } else if (method == 'stop' && mounted) {
      _measurementTimer?.cancel();
      _measurementTimer = null;
      setState(() {
        _measuring = false;
        _measuringType = null;
        _measurementStopRequested = true;
      });
    } else if (method == 'upload' && mounted) {
      setState(() => _updating = true);
      _uploadTimer?.cancel();
      _uploadTimer = Timer(const Duration(seconds: 10), () async {
        if (!mounted) return;
        await _call('readLot');
        if (mounted) setState(() => _updating = false);
      });
    }
    // Keep the loading state active until the follow-up refresh below has
    // completed.  Clearing it here lets a second tap race the refresh
    // request, especially after the top device switch is toggled.
    await Future<void>.delayed(const Duration(milliseconds: 500));
    if (method == 'start' || method == 'stop') {
      // The demo refreshes Read RtuInf after Set Start / Set Stop so the
      // device switch always follows the RTU's returned work-state bits.
      if (isDeviceSwitch) {
        await _readBasicOnly();
      } else {
        await _readAll();
      }
    } else if (mounted && method == 'zero') {
      final sensor = await _call('readSensor', {'type': _sensorType});
      if (mounted && sensor is Map) {
        setState(() {
          _sensor = Map<String, dynamic>.from(sensor);
          _sensorWaitController.text = sensor['sampleWait']?.toString() ?? '';
        });
      }
    }
    if (method == 'setPower') {
      // 长供电开关：Write PwrEn → Read RtuInf，不发送 Read SamInf
      await _readBasicOnly();
    } else if ({
      'setSampleInterval',
      'setUploadInterval',
      'setSystemTime',
      'setMn',
      'setSystemSt',
      'setLotType',
      'setSensorDeviceType',
      'setSensorWait',
      'setNetwork',
      'setNetworkIp',
      'setNetworkPort',
      'setNetworkProtocol',
      'setNetworkEnabled',
      'removeNetwork',
    }.contains(method)) {
      await _readAll();
    }
    if (mounted) setState(() => _busy = false);
  }

  Future<bool> _stopActiveMeasurement() async {
    _measurementStopRequested = true;
    _measurementTimer?.cancel();
    _measurementTimer = null;
    for (var i = 0; i < 300 && _sensorReadInFlight; i++) {
      await Future<void>.delayed(const Duration(milliseconds: 50));
    }
    if (!_measuring) return true;
    if (mounted) {
      setState(() {
        _busy = true;
        _busyMessage = '正在停止测量…';
      });
    }
    final result = await _call('stop');
    final ok = result is Map && result['ok'] == true;
    if (!mounted) return ok;
    setState(() {
      _busy = false;
      if (ok) {
        _measuring = false;
        _measuringType = null;
      }
    });
    return ok;
  }

  Future<void> _switchSensorTab(String target) async {
    if (!_connected) {
      _show('请先连接 RTU');
      return;
    }
    if (_sensorType == target && _activeTab == target) return;
    final currentType = _measuringType ?? _sensorType;
    if (_measuring && currentType != target) {
      final currentLabel = currentType == 'llj' ? '雷达流量计' : '雷达水位计';
      final targetLabel = target == 'llj' ? '雷达流量计' : '雷达水位计';
      final shouldSwitch = await showDialog<bool>(
        context: context,
        builder:
            (dialogContext) => AlertDialog(
              title: const Text('正在测量'),
              content: Text('$currentLabel正在测量，是否停止测量并切换到$targetLabel？'),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(false),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(dialogContext).pop(true),
                  child: const Text('停止并切换'),
                ),
              ],
            ),
      );
      if (shouldSwitch != true || !mounted) return;
      final stopped = await _stopActiveMeasurement();
      if (!stopped || !mounted) return;
    }
    setState(() {
      _sensorType = target;
      _activeTab = target;
    });
    await _readSelectedSensor(showLoading: true);
  }

  int get _serviceEnabledBits =>
      (_lot?['serviceEnabledBits'] as num?)?.toInt() ?? 0;

  Future<void> _removeNetwork(int index) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder:
          (context) => AlertDialog(
            title: const Text('删除通讯服务'),
            content: Text('确认删除通讯服务 ${index + 1} 吗？该操作会停用并清空该服务的 IP。'),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(false),
                child: const Text('取消'),
              ),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pop(true),
                child: const Text('删除'),
              ),
            ],
          ),
    );
    if (confirm != true || !mounted) return;
    await _action('removeNetwork', {
      'index': index,
      'currentEnabledBits': _serviceEnabledBits,
    });
  }

  Future<void> _addNetwork() async {
    if (!_connected) {
      _show('请先连接 RTU');
      return;
    }
    if (_lot == null) {
      _show('请先刷新读取 RTU 通讯服务信息');
      await _readAll();
      return;
    }
    final index = _networkAdded.indexWhere((value) => !value);
    if (index < 0) {
      _show('最多支持 5 组通信服务');
      return;
    }
    await _showNetworkEditor(index, isNew: true);
  }

  Future<void> _showNetworkEditor(int index, {bool isNew = false}) async {
    final current = _networkControllers[index];
    final hostController = TextEditingController(
      text: isNew ? '' : current.host.text,
    );
    final portController = TextEditingController(
      text: isNew ? '30000' : current.port.text,
    );
    var selectedProtocol = int.tryParse(current.protocol.text) == 1 ? 1 : 0;
    var enabled = isNew ? true : current.enabled;
    String? validationMessage;
    final draft = await showDialog<Map<String, dynamic>>(
      context: context,
      builder:
          (dialogContext) => StatefulBuilder(
            builder:
                (context, setDialogState) => AlertDialog(
                  title: Text(
                    isNew ? '新增通信服务 ${index + 1}' : '通信服务 ${index + 1} 配置',
                  ),
                  content: SingleChildScrollView(
                    child: SizedBox(
                      width: 340,
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          TextField(
                            controller: hostController,
                            keyboardType: TextInputType.url,
                            decoration: const InputDecoration(
                              labelText: 'IP 地址 / 域名',
                              hintText: '例如 36.137.24.154',
                            ),
                          ),
                          const SizedBox(height: 16),
                          TextField(
                            controller: portController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '端口号（1-99999）',
                            ),
                          ),
                          const SizedBox(height: 16),
                          DropdownButtonFormField<int>(
                            value: selectedProtocol,
                            decoration: const InputDecoration(
                              labelText: '上传协议',
                            ),
                            items: const [
                              DropdownMenuItem(value: 0, child: Text('212 协议')),
                              DropdownMenuItem(
                                value: 1,
                                child: Text('MQTT 协议'),
                              ),
                            ],
                            onChanged:
                                (value) => setDialogState(
                                  () => selectedProtocol = value ?? 0,
                                ),
                          ),
                          SwitchListTile.adaptive(
                            contentPadding: EdgeInsets.zero,
                            title: const Text('启用通讯服务'),
                            value: enabled,
                            activeColor: const Color(0xff1fc7ae),
                            onChanged:
                                (value) =>
                                    setDialogState(() => enabled = value),
                          ),
                          if (validationMessage != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 4),
                              child: Text(
                                validationMessage!,
                                style: const TextStyle(color: Colors.red),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.of(dialogContext).pop(),
                      child: const Text('取消'),
                    ),
                    FilledButton(
                      onPressed: () {
                        final host = hostController.text.trim();
                        final port = int.tryParse(portController.text.trim());
                        if (host.isEmpty) {
                          setDialogState(
                            () => validationMessage = '请输入服务器 IP 地址或域名',
                          );
                          return;
                        }
                        final ipParts = host.split('.');
                        final isIpv4 =
                            ipParts.length == 4 &&
                            ipParts.every(
                              (part) =>
                                  part.isNotEmpty &&
                                  int.tryParse(part) != null &&
                                  int.parse(part) >= 0 &&
                                  int.parse(part) <= 255,
                            );
                        if (!isIpv4 && host.codeUnits.length > 12) {
                          setDialogState(
                            () =>
                                validationMessage =
                                    '请输入正确 IPv4 地址，或不超过 12 个字符的域名',
                          );
                          return;
                        }
                        if (port == null || port < 1 || port > 99999) {
                          setDialogState(
                            () => validationMessage = '端口号范围为 1-99999',
                          );
                          return;
                        }
                        Navigator.of(dialogContext).pop({
                          'host': host,
                          'port': port,
                          'protocol': selectedProtocol,
                          'enabled': enabled,
                        });
                      },
                      child: const Text('确认配置'),
                    ),
                  ],
                ),
          ),
    );
    hostController.dispose();
    portController.dispose();
    if (draft == null || !mounted) return;
    setState(() {
      _networkAdded[index] = true;
      final item = _networkControllers[index];
      item.host.text = draft['host'] as String;
      item.port.text = '${draft['port']}';
      item.protocol.text = '${draft['protocol']}';
      item.protocolValue = draft['protocol'] as int;
      item.enabled = draft['enabled'] as bool;
    });
    await _action('setNetwork', {
      'index': index,
      'host': draft['host'],
      'port': draft['port'],
      'protocol': draft['protocol'],
      'enabled': draft['enabled'],
      'currentEnabledBits': _serviceEnabledBits,
    });
  }

  Future<void> _readSelectedSensor({bool showLoading = false}) async {
    if (!_connected) return;
    // A periodic callback may already be queued when upload/switch starts.
    // Manual tab refreshes (showLoading=true) are still allowed after stop.
    if (_measurementStopRequested && !showLoading) return;
    // _readAll and configuration actions use the same single-command BLE
    // channel.  Do not let a timer tick compete with one of those commands;
    // wait for the next tick after the current operation has completed.
    if (_busy && !showLoading) return;
    if (_sensorReadInFlight) return;
    _sensorReadInFlight = true;
    try {
      final readType = _measuringType ?? _sensorType;
      if (showLoading) {
        setState(() {
          _busy = true;
          _busyMessage = '正在读取${readType == 'llj' ? '雷达流量计' : '雷达水位计'}数据…';
        });
      }
      // 测量期间持续发送的 Read SamInf 指令需要在指令框中滚动显示
      _backgroundRead = false;
      try {
        final sensor = await _call('readSensor', {'type': readType});
        if (mounted && sensor is Map) {
          setState(() {
            _sensor = Map<String, dynamic>.from(sensor);
            _sensorWaitController.text = sensor['sampleWait']?.toString() ?? '';
            if (showLoading) _busy = false;
          });
        } else if (mounted && showLoading) {
          setState(() => _busy = false);
        }
      } finally {
        _backgroundRead = false;
      }
    } finally {
      _sensorReadInFlight = false;
    }
  }

  Future<void> _upgrade() async {
    final picked = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['bin'],
    );
    final path = picked?.files.single.path;
    if (path != null) await _action('upgradeBin', {'path': path});
  }

  void _show(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _sendManualCommand() async {
    final command = _commandController.text.trim();
    if (command.isEmpty) return;
    await _action('sendCommand', {'command': command});
  }

  bool get _deviceIsOn {
    final state = (_basic?['deviceState'] as num?)?.toInt();
    return state != null && state > 0;
  }

  String _deviceStateText(dynamic value) {
    final state = (value as num?)?.toInt();
    if (state == null || state < 0) return '-';
    if (state == 0) return '空闲';
    if ((state & 0x02) != 0) return '正在采样';
    if ((state & 0x04) != 0) return '正在上传';
    if ((state & 0x01) != 0) return '定时采样';
    return '未知状态($state)';
  }

  int _batteryPercent(dynamic value) {
    final voltage = (value as num?)?.toDouble() ?? 0;
    if (voltage <= 9.8) return 0;
    if (voltage >= 10.6) return 100;
    return ((voltage - 9.8) / 0.008).round().clamp(0, 100);
  }

  String _protocolText(dynamic value) =>
      (value as num?)?.toInt() == 1 ? 'MQTT' : '212';

  String _moduleText(dynamic value) {
    switch ((value as num?)?.toInt()) {
      case 0:
        return 'NB';
      case 1:
        return '4G';
      default:
        return value?.toString() ?? '-';
    }
  }

  String _sensorDeviceTypeText(dynamic value) {
    switch ((value as num?)?.toInt()) {
      case 0:
        return '雷达水位计';
      case 1:
        return '雷达流量计';
      case 2:
        return '未连接设备';
      default:
        return value?.toString() ?? '-';
    }
  }

  Future<void> _showBluetoothActions() async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder:
          (sheetContext) => Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  _connected
                      ? '已连接 ${_basic?['deviceName'] ?? 'RTU'}'
                      : '蓝牙未连接',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text(
                  '状态：$_status${_addressController.text.isEmpty ? '' : '  ${_addressController.text}'}',
                ),
                const SizedBox(height: 14),
                FilledButton.icon(
                  onPressed: () {
                    Navigator.of(sheetContext).pop();
                    _scan();
                  },
                  icon: const Icon(Icons.bluetooth_searching),
                  label: const Text('扫描 RTU 蓝牙设备'),
                ),
                if (!_connected) ...[
                  const SizedBox(height: 10),
                  TextField(
                    controller: _addressController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: const InputDecoration(
                      labelText: '手动输入 RTU 蓝牙地址（15xxxxxxxxxx）',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () {
                      Navigator.of(sheetContext).pop();
                      _connect();
                    },
                    child: const Text('手动连接'),
                  ),
                ],
                if (_connected) ...[
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: () async {
                      Navigator.of(sheetContext).pop();
                      await _disconnect();
                    },
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red,
                    ),
                    icon: const Icon(Icons.bluetooth_disabled),
                    label: const Text('断开蓝牙'),
                  ),
                ],
              ],
            ),
          ),
    );
  }

  int _modbusCrc(List<int> data) {
    var crc = 0xffff;
    for (final byte in data) {
      crc ^= byte & 0xff;
      for (var i = 0; i < 8; i++) {
        crc = (crc & 1) == 1 ? (crc >> 1) ^ 0xa001 : crc >> 1;
      }
    }
    return crc & 0xffff;
  }

  String _hex(List<int> values) =>
      values
          .map((value) => (value & 0xff).toRadixString(16).padLeft(2, '0'))
          .join()
          .toUpperCase();

  String _modbusRead(int address, int count) {
    final register = address - 40001;
    final data = <int>[0x4a, 0x03, register >> 8, register, count >> 8, count];
    final crc = _modbusCrc(data);
    return _hex([...data, crc, crc >> 8]);
  }

  // This deliberately follows ProtocolUtil.getWriteHexData in SampleBox.
  // The RTU's 485 bridge uses its custom count/start-register packet here.
  String _modbusWrite(int address, List<int> values) {
    final register = address > 40000 ? address - 40000 : address;
    final count = values.length + 1;
    final data = <int>[
      0x4a,
      0x10,
      count >> 24,
      count >> 16,
      count >> 8,
      count,
      count * 2,
      register >> 8,
      register,
      for (final value in values) ...[value >> 8, value],
    ];
    final crc = _modbusCrc(data);
    return _hex([...data, crc, crc >> 8]);
  }

  List<int>? _modbusResponseBytes(dynamic response) {
    final text = response?.toString() ?? '';
    final match = RegExp(r'[0-9a-fA-F]{10,}').firstMatch(text);
    final hex = match?.group(0);
    if (hex == null || hex.length.isOdd) return null;
    return List<int>.generate(
      hex.length ~/ 2,
      (index) => int.parse(hex.substring(index * 2, index * 2 + 2), radix: 16),
    );
  }

  void _applyFlowReadResponse(int firstAddress, dynamic response) {
    final bytes = _modbusResponseBytes(response);
    if (bytes == null ||
        bytes.length < 5 ||
        bytes[0] != 0x4a ||
        bytes[1] != 0x03) {
      _show('未识别到流量计 485 读取回包');
      return;
    }
    final byteCount = bytes[2];
    if (byteCount.isOdd || bytes.length < byteCount + 5) {
      _show('流量计 485 回包长度异常');
      return;
    }
    setState(() {
      for (var i = 0; i < byteCount ~/ 2; i++) {
        _flowRegisters[firstAddress + i] =
            (bytes[3 + i * 2] << 8) | bytes[4 + i * 2];
      }
    });
  }

  Future<bool> _readFlowRegisters(
    int address,
    int count, {
    bool showLoading = true,
  }) async {
    if (!_connected) {
      _show('请先连接 RTU');
      return false;
    }
    if (address < 40001 || count < 1 || count > 125) {
      _show('请输入正确的 ModBus 地址和数量（数量 1-125）');
      return false;
    }
    if (showLoading) {
      setState(() {
        _busy = true;
        _busyMessage = '正在读取流量计 485 参数…';
      });
    }
    final result = await _call('sendCommand', {
      'command': 'Write DIY=${_modbusRead(address, count)}',
    });
    if (mounted && showLoading) setState(() => _busy = false);
    if (result is! Map || result['ok'] != true) return false;
    _applyFlowReadResponse(address, result['response']);
    return true;
  }

  Future<bool> _writeFlowRegisters(
    int address,
    List<int> values, {
    String? label,
  }) async {
    if (!_connected) {
      _show('请先连接 RTU');
      return false;
    }
    if (values.isEmpty || values.length > 127) {
      _show('流量计写入寄存器数量必须为 1-127');
      return false;
    }
    setState(() {
      _busy = true;
      _busyMessage = '正在写入${label ?? '流量计 485 参数'}…';
    });
    final result = await _call('sendCommand', {
      'command': 'Write DIY=${_modbusWrite(address, values)}',
    });
    if (mounted) setState(() => _busy = false);
    if (result is! Map || result['ok'] != true) return false;
    _show('${label ?? '流量计参数'}已写入');
    return true;
  }

  Future<void> _readAllFlowConfig() async {
    if (!_connected) {
      _show('请先连接 RTU');
      return;
    }
    setState(() {
      _busy = true;
      _busyMessage = '正在读取流量计基础配置…';
    });
    final ranges = <List<int>>[
      [40054, 6],
      [40085, 39],
      [40155, 1],
      [40200, 40],
    ];
    var success = true;
    for (final range in ranges) {
      final result = await _call('sendCommand', {
        'command': 'Write DIY=${_modbusRead(range[0], range[1])}',
      });
      if (result is Map && result['ok'] == true) {
        _applyFlowReadResponse(range[0], result['response']);
      } else {
        success = false;
        break;
      }
    }
    if (mounted) setState(() => _busy = false);
    if (success) _show('流量计基础配置读取完成');
  }

  List<int> _floatLowHigh(double value) {
    final data = ByteData(4)..setFloat32(0, value, Endian.big);
    return [data.getUint16(2, Endian.big), data.getUint16(0, Endian.big)];
  }

  double _floatFromLowHigh(int low, int high) {
    final data =
        ByteData(4)
          ..setUint16(0, high & 0xffff, Endian.big)
          ..setUint16(2, low & 0xffff, Endian.big);
    return data.getFloat32(0, Endian.big);
  }

  String _flowFieldValue(Map<String, dynamic> field) {
    final address = field['address'] as int;
    final count = field['count'] as int;
    final registers = List<int?>.generate(
      count,
      (index) => _flowRegisters[address + index],
    );
    if (registers.any((value) => value == null)) return '-';
    final values = registers.cast<int>();
    final type = field['type'] as String;
    dynamic value;
    switch (type) {
      case 'scaled':
        value = values.first * (field['scale'] as double);
        break;
      case 'signed32':
        var signed = (values[0] << 16) | values[1];
        if ((signed & 0x80000000) != 0) signed -= 0x100000000;
        value = signed * (field['scale'] as double? ?? 1);
        break;
      case 'float':
        value = _floatFromLowHigh(values[0], values[1]);
        break;
      case 'total':
        value = values[2] * 10000000 + _floatFromLowHigh(values[0], values[1]);
        break;
      default:
        value = values.first;
    }
    final options = field['options'] as Map<int, String>?;
    if (options != null) {
      final index = value as int;
      return options[index] ?? '未知($index)';
    }
    if (value is double) {
      if (!value.isFinite) return '-';
      return value.toStringAsFixed(3).replaceFirst(RegExp(r'\.?0+$'), '');
    }
    return value.toString();
  }

  Future<void> _editFlowField(Map<String, dynamic> field) async {
    final options = field['options'] as Map<int, String>?;
    int? selected;
    if (options != null) {
      selected = await showDialog<int>(
        context: context,
        builder:
            (dialogContext) => SimpleDialog(
              title: Text(field['label'] as String),
              children:
                  options.entries
                      .map(
                        (entry) => SimpleDialogOption(
                          onPressed:
                              () => Navigator.of(dialogContext).pop(entry.key),
                          child: Text(entry.value),
                        ),
                      )
                      .toList(),
            ),
      );
      if (selected == null) return;
    }
    final controller = TextEditingController(
      text: options == null ? _flowFieldValue(field) : '',
    );
    if (options == null) {
      final shouldWrite = await showDialog<bool>(
        context: context,
        builder:
            (dialogContext) => AlertDialog(
              title: Text(field['label'] as String),
              content: TextField(
                controller: controller,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                  signed: true,
                ),
                decoration: InputDecoration(
                  labelText:
                      '数值${field['unit'] == null ? '' : '（${field['unit']}）'}',
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(false),
                  child: const Text('取消'),
                ),
                FilledButton(
                  onPressed: () => Navigator.of(dialogContext).pop(true),
                  child: const Text('写入'),
                ),
              ],
            ),
      );
      if (shouldWrite != true) {
        controller.dispose();
        return;
      }
    }
    final input =
        options == null
            ? double.tryParse(controller.text.trim())
            : selected!.toDouble();
    controller.dispose();
    if (input == null) {
      _show('请输入正确的数值');
      return;
    }
    final type = field['type'] as String;
    List<int> values;
    switch (type) {
      case 'scaled':
        values = [(input / (field['scale'] as double)).round()];
        break;
      case 'signed32':
        final raw = (input / (field['scale'] as double? ?? 1)).round();
        values = [(raw >> 16) & 0xffff, raw & 0xffff];
        break;
      case 'float':
        values = _floatLowHigh(input);
        break;
      case 'total':
        final large = input ~/ 10000000;
        values = [..._floatLowHigh(input - large * 10000000), large];
        break;
      default:
        values = [input.round()];
    }
    final saved = await _writeFlowRegisters(
      field['address'] as int,
      values,
      label: field['label'] as String,
    );
    if (saved)
      await _readFlowRegisters(field['address'] as int, field['count'] as int);
  }

  Future<void> _editFlowCoefficient(int slot) async {
    final address = 40200 + slot * 4;
    final water =
        _flowRegisters.containsKey(address)
            ? _floatFromLowHigh(
              _flowRegisters[address]!,
              _flowRegisters[address + 1]!,
            )
            : 0.0;
    final factor =
        _flowRegisters.containsKey(address + 2)
            ? _floatFromLowHigh(
              _flowRegisters[address + 2]!,
              _flowRegisters[address + 3]!,
            )
            : 0.0;
    final waterController = TextEditingController(text: water.toString());
    final factorController = TextEditingController(text: factor.toString());
    final confirm = await showDialog<bool>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: Text('$slot 号水深-系数'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: waterController,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(labelText: '水深（m）'),
                ),
                TextField(
                  controller: factorController,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(labelText: '系数'),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(false),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(dialogContext).pop(true),
                child: const Text('写入'),
              ),
            ],
          ),
    );
    final waterValue = double.tryParse(waterController.text.trim());
    final factorValue = double.tryParse(factorController.text.trim());
    waterController.dispose();
    factorController.dispose();
    if (confirm != true) return;
    if (waterValue == null || factorValue == null) {
      _show('请输入正确的水深和系数');
      return;
    }
    final saved = await _writeFlowRegisters(40200, [
      slot,
      ..._floatLowHigh(waterValue),
      ..._floatLowHigh(factorValue),
    ], label: '$slot 号水深-系数');
    if (saved) await _readFlowRegisters(40200, 40);
  }

  String _formatDateTime(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')} '
      '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}:${value.second.toString().padLeft(2, '0')}';

  Widget _buildDemoInteraction(Map<String, dynamic> basic) {
    Widget button(String text, VoidCallback? action, Color color) => Expanded(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: SizedBox(
          height: 96,
          child: ElevatedButton(
            onPressed: action,
            style: ElevatedButton.styleFrom(
              backgroundColor: color,
              foregroundColor: Colors.white,
              elevation: 0,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
            ),
            child: Text(
              text,
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w600),
            ),
          ),
        ),
      ),
    );
    return Column(
      children: [
        Container(
          height: 154,
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xff2789cf)),
            borderRadius: BorderRadius.circular(5),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child:
              _logLines.isEmpty
                  ? const Align(
                    alignment: Alignment.topLeft,
                    child: Text(
                      'RTU 指令交互信息将在此显示',
                      style: TextStyle(color: Color(0xff8a8a8a)),
                    ),
                  )
                  : ListView.builder(
                    controller: _commandLogController,
                    itemCount: _logLines.length,
                    itemBuilder: (_, index) {
                      final line = _logLines[index];
                      final color =
                          line.startsWith('发送')
                              ? const Color(0xff1976d2)
                              : line.startsWith('接收')
                              ? const Color(0xff7b4db5)
                              : line.startsWith('成功')
                              ? const Color(0xff249b62)
                              : Colors.red.shade700;
                      return SelectableText(
                        line,
                        style: TextStyle(fontSize: 13, color: color),
                      );
                    },
                  ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: SizedBox(
                height: 48,
                child: TextField(
                  controller: _commandController,
                  textInputAction: TextInputAction.send,
                  onSubmitted: (_) => _sendManualCommand(),
                  decoration: const InputDecoration(
                    hintText: '请输入指令内容',
                    contentPadding: EdgeInsets.symmetric(horizontal: 12),
                    border: OutlineInputBorder(),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            SizedBox(
              height: 48,
              width: 86,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xff1fc7ae),
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                onPressed: _connected && !_busy ? _sendManualCommand : null,
                child: const Text('发送', style: TextStyle(fontSize: 17)),
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            button(
              '雷达\n水位计',
              _connected && !_busy
                  ? () => unawaited(_switchSensorTab('swj'))
                  : null,
              _activeTab == 'swj'
                  ? const Color(0xfff69a2e)
                  : const Color(0xffa7a7a7),
            ),
            button(
              '雷达\n流量计',
              _connected && !_busy
                  ? () => unawaited(_switchSensorTab('llj'))
                  : null,
              _activeTab == 'llj'
                  ? const Color(0xff2789cf)
                  : const Color(0xffa7a7a7),
            ),
            button(
              'RTU\n设置',
              _connected && !_busy
                  ? () {
                    setState(() => _activeTab = 'settings');
                    _readAll();
                  }
                  : null,
              _activeTab == 'settings'
                  ? const Color(0xff24c7ad)
                  : const Color(0xffa7a7a7),
            ),
          ],
        ),
        const SizedBox(height: 12),
      ],
    );
  }

  Widget _buildDemoDeviceInfo(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
  ) {
    final power = (basic['powerEnabled'] as num?)?.toInt() == 1;
    final services =
        (lot['services'] as List?)?.whereType<Map>().toList() ?? const <Map>[];
    final firstService =
        services.isEmpty ? const <dynamic, dynamic>{} : services.first;
    Widget infoCell(String label, dynamic value, {bool editable = false}) =>
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(right: 8, bottom: 8),
            child: RichText(
              overflow: TextOverflow.ellipsis,
              text: TextSpan(
                style: const TextStyle(fontSize: 14, color: Color(0xff333333)),
                children: [
                  TextSpan(text: '$label：'),
                  TextSpan(
                    text:
                        value?.toString().isNotEmpty == true
                            ? value.toString()
                            : '-',
                    style: TextStyle(
                      color:
                          editable
                              ? const Color(0xff1876d1)
                              : const Color(0xff555555),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
    Widget grid(List<Widget> cells) => Column(
      children: List.generate((cells.length / 2).ceil(), (index) {
        final first = cells[index * 2];
        final second =
            index * 2 + 1 < cells.length
                ? cells[index * 2 + 1]
                : const Expanded(child: SizedBox());
        return Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [first, second],
        );
      }),
    );
    final signal = (lot['signal'] as num?)?.toInt() ?? 0;
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.fromLTRB(4, 2, 4, 6),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text(
                '设备信息',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
              ),
              const SizedBox(width: 10),
              Icon(
                _batteryPercent(basic['batteryVoltage']) >= 80
                    ? Icons.battery_full
                    : _batteryPercent(basic['batteryVoltage']) >= 35
                    ? Icons.battery_5_bar
                    : Icons.battery_1_bar,
                color: const Color(0xff2d2d2d),
                size: 22,
              ),
              Text(
                '${basic['batteryVoltage'] ?? '-'}V ${_batteryPercent(basic['batteryVoltage'])}%',
              ),
              const Spacer(),
              InkWell(
                onTap: _busy ? null : _readAll,
                child: const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 4, vertical: 7),
                  child: Row(
                    children: [
                      Icon(Icons.refresh, size: 19),
                      SizedBox(width: 2),
                      Text('刷新'),
                    ],
                  ),
                ),
              ),
              InkWell(
                onTap:
                    () => setState(
                      () => _showDeviceDetails = !_showDeviceDetails,
                    ),
                child: Padding(
                  padding: const EdgeInsets.only(left: 8, top: 7, bottom: 7),
                  child: Row(
                    children: [
                      Text(_showDeviceDetails ? '收起信息' : '更多信息'),
                      Icon(
                        _showDeviceDetails
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const Divider(height: 12),
          grid([
            infoCell('版本信息', basic['version']),
            infoCell('系统时间', basic['systemTime'], editable: true),
            infoCell('工作状态', _deviceStateText(basic['deviceState'])),
            infoCell('设备MN', basic['mn'], editable: true),
          ]),
          Row(
            children: [
              const Text('设备开关'),
              Switch.adaptive(
                value: _deviceIsOn,
                activeColor: const Color(0xff1fc7ae),
                onChanged:
                    _connected && !_busy
                        ? (enabled) => _action(
                          enabled ? 'start' : 'stop',
                          const {'measurement': false},
                        )
                        : null,
              ),
              const SizedBox(width: 18),
              const Text('长供电开关'),
              Switch.adaptive(
                value: power,
                activeColor: const Color(0xff1fc7ae),
                onChanged:
                    _connected && !_busy
                        ? (enabled) => _action('setPower', {'enabled': enabled})
                        : null,
              ),
            ],
          ),
          if (_showDeviceDetails) ...[
            const Divider(height: 12),
            grid([
              infoCell('系统编码', lot['systemSt'], editable: true),
              infoCell('通信模组', _moduleText(lot['moduleType']), editable: true),
              infoCell('物联网卡', lot['ccid']),
              infoCell('IMSI号', lot['imsi']),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.only(right: 8, bottom: 8),
                  child: Row(
                    children: [
                      Icon(
                        Icons.signal_cellular_alt,
                        color:
                            signal >= 15
                                ? const Color(0xff28a66b)
                                : Colors.orange,
                        size: 19,
                      ),
                      const SizedBox(width: 3),
                      Text('网络信号：$signal${signal >= 15 ? ' 中' : ' 弱'}'),
                    ],
                  ),
                ),
              ),
              infoCell('待传数据', lot['pendingCount']),
              infoCell(
                '采集间隔',
                '${lot['sampleInterval'] ?? '-'} 分钟',
                editable: true,
              ),
              infoCell(
                '上传间隔',
                '${lot['uploadInterval'] ?? '-'} 分钟',
                editable: true,
              ),
              infoCell('故障信息', lot['uploadError']),
              infoCell('最近上传', lot['uploadEndTime']),
            ]),
            const Divider(height: 12),
            grid([
              infoCell('通讯IP', firstService['host'], editable: true),
              infoCell('端口', firstService['port'], editable: true),
              infoCell(
                '上传协议',
                _protocolText(firstService['protocol']),
                editable: true,
              ),
              infoCell(
                '通讯状态',
                firstService['enabled'] == true ? '启用' : '停用',
                editable: true,
              ),
            ]),
            if (_activeTab == 'settings')
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  onPressed: _connected ? _addNetwork : null,
                  icon: const Icon(Icons.add),
                  label: const Text('一点多传  新增通讯'),
                ),
              ),
          ],
        ],
      ),
    );
  }

  Widget _sensorGrid(List<MapEntry<String, dynamic>> entries) => Column(
    children: List.generate((entries.length / 2).ceil(), (index) {
      Widget cell(MapEntry<String, dynamic> item) => Expanded(
        child: Padding(
          padding: const EdgeInsets.only(right: 8, bottom: 10),
          child: Text(
            '${item.key}：${item.value ?? '-'}',
            style: const TextStyle(fontSize: 14),
          ),
        ),
      );
      final first = cell(entries[index * 2]);
      final second =
          index * 2 + 1 < entries.length
              ? cell(entries[index * 2 + 1])
              : const Expanded(child: SizedBox());
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [first, second],
      );
    }),
  );

  Widget _demoActionButton(String label, Color color, VoidCallback? action) =>
      Expanded(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 4),
          child: SizedBox(
            height: 46,
            child: ElevatedButton(
              onPressed: action,
              style: ElevatedButton.styleFrom(
                backgroundColor: color,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              child: Text(label, textAlign: TextAlign.center),
            ),
          ),
        ),
      );

  Widget _buildWaterPanel(Map<String, dynamic> sensor) => Container(
    margin: const EdgeInsets.only(bottom: 12),
    padding: const EdgeInsets.only(top: 2),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '雷达水位计',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 10),
        InkWell(
          onTap:
              _connected && !_busy
                  ? () => _editSetting(
                    '水位计上电延时（秒）',
                    _sensorWaitController,
                    () => _action('setSensorWait', {
                      'seconds': int.tryParse(_sensorWaitController.text) ?? 0,
                    }),
                    keyboard: TextInputType.number,
                  )
                  : null,
          child: Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Row(
              children: [
                const Text('上电延时：'),
                Text(
                  '${sensor['sampleWait'] ?? '-'} 秒',
                  style: const TextStyle(color: Color(0xff1876d1)),
                ),
                const Icon(Icons.chevron_right, color: Color(0xff999999)),
              ],
            ),
          ),
        ),
        _sensorGrid([
          MapEntry('俯仰角', sensor['pitch'] ?? sensor['tilt']),
          MapEntry('空高', sensor['height']),
          MapEntry('水位', sensor['waterLevel']),
          MapEntry('水深', sensor['depth']),
          MapEntry('瞬时流量', sensor['rateFlow']),
          MapEntry('时隔流量', sensor['intervalFlow']),
          MapEntry('累计流量', sensor['cumulativeFlowTotal']),
        ]),
        const SizedBox(height: 4),
        Row(
          children: [
            _demoActionButton(
              _measuring ? '停止测量' : '开始测量',
              const Color(0xff2789cf),
              _connected && !_busy
                  ? () => _action(_measuring ? 'stop' : 'start')
                  : null,
            ),
            _demoActionButton(
              _updating ? '上报中…' : '采集上报',
              const Color(0xff22b886),
              _connected && !_updating && !_busy
                  ? () => _action('upload')
                  : null,
            ),
          ],
        ),
      ],
    ),
  );

  Widget _buildFlowPanel(Map<String, dynamic> sensor) => Container(
    margin: const EdgeInsets.only(bottom: 12),
    padding: const EdgeInsets.only(top: 2),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '雷达流量计',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 10),
        InkWell(
          onTap:
              _connected && !_busy
                  ? () => _editSetting(
                    '流量计上电延时（秒）',
                    _sensorWaitController,
                    () => _action('setSensorWait', {
                      'seconds': int.tryParse(_sensorWaitController.text) ?? 0,
                    }),
                    keyboard: TextInputType.number,
                  )
                  : null,
          child: Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Row(
              children: [
                const Text('上电延时：'),
                Text(
                  '${sensor['sampleWait'] ?? '-'} 秒',
                  style: const TextStyle(color: Color(0xff1876d1)),
                ),
                const Icon(Icons.chevron_right, color: Color(0xff999999)),
              ],
            ),
          ),
        ),
        _sensorGrid([
          MapEntry('横滚角', sensor['roll']),
          MapEntry('俯仰角', sensor['pitch']),
          MapEntry('水位', sensor['waterLevel']),
          MapEntry('空高', sensor['height']),
          MapEntry('水深', sensor['depth']),
          MapEntry('流速方向', sensor['direction']),
          MapEntry('表面流速', sensor['surfaceVelocity']),
          MapEntry('断面流速', sensor['sectionVelocity']),
          MapEntry('过水面积', sensor['area']),
          MapEntry('瞬时流量', sensor['rateFlow']),
          MapEntry('淤积厚度', sensor['thickness']),
          MapEntry('时隔流量', sensor['intervalFlow']),
          MapEntry('累计流量', sensor['cumulativeFlowTotal']),
          MapEntry('正向累计流量', sensor['secondaryCumulativeFlowTotal']),
          MapEntry('反向累计流量', sensor['nightCumulativeFlowTotal']),
          MapEntry('绝对值累计流量', sensor['absoluteCumulativeFlowTotal']),
        ]),
        const SizedBox(height: 5),
        Row(
          children: [
            _demoActionButton(
              '清空\n累计流量',
              const Color(0xfff39a28),
              _connected && !_busy ? () => _action('zero') : null,
            ),
            _demoActionButton(
              '基础配置',
              const Color(0xff2789cf),
              _connected && !_busy
                  ? () {
                    setState(() => _flowBaseConfigVisible = true);
                    unawaited(_readAllFlowConfig());
                  }
                  : null,
            ),
            _demoActionButton(
              _measuring ? '关闭\n调试模式' : '打开\n调试模式',
              const Color(0xffe36a9d),
              _connected && !_busy
                  ? () => _action(_measuring ? 'stop' : 'start')
                  : null,
            ),
            _demoActionButton(
              _updating ? '上报中…' : '采集\n上报',
              const Color(0xff22b886),
              _connected && !_updating && !_busy
                  ? () => _action('upload')
                  : null,
            ),
          ],
        ),
      ],
    ),
  );

  Future<void> _editSetting(
    String label,
    TextEditingController target,
    Future<void> Function() save, {
    TextInputType? keyboard,
  }) async {
    final editor = TextEditingController(text: target.text);
    final confirmed = await showDialog<bool>(
      context: context,
      builder:
          (dialogContext) => AlertDialog(
            title: Text('修改$label'),
            content: TextField(
              controller: editor,
              keyboardType: keyboard,
              decoration: InputDecoration(
                labelText: label,
                border: const OutlineInputBorder(),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(dialogContext).pop(false),
                child: const Text('取消'),
              ),
              FilledButton(
                onPressed: () => Navigator.of(dialogContext).pop(true),
                child: const Text('保存'),
              ),
            ],
          ),
    );
    if (confirmed == true) {
      target.text = editor.text.trim();
      await save();
    }
    editor.dispose();
  }

  Future<void> _selectSensorDeviceType() async {
    const options = <int, String>{0: '雷达水位计', 1: '雷达流量计', 2: '未连接设备'};
    final selected = await showDialog<int>(
      context: context,
      builder:
          (dialogContext) => SimpleDialog(
            title: const Text('设置 RTU 当前连接设备'),
            children:
                options.entries
                    .map(
                      (item) => SimpleDialogOption(
                        onPressed:
                            () => Navigator.of(dialogContext).pop(item.key),
                        child: Text(item.value),
                      ),
                    )
                    .toList(),
          ),
    );
    if (selected == null) return;
    _sensorDeviceTypeController.text = '$selected';
    _show('请务必与 RTU 实际连接设备一致，否则无法正常采集测量');
    await _action('setSensorDeviceType', {'value': selected});
  }

  Widget _settingLine(String label, String value, VoidCallback? onTap) =>
      InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 11),
          child: Row(
            children: [
              Expanded(
                child: Text(label, style: const TextStyle(fontSize: 15)),
              ),
              Flexible(
                child: Text(
                  value.isEmpty ? '-' : value,
                  textAlign: TextAlign.end,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color:
                        onTap == null
                            ? const Color(0xff555555)
                            : const Color(0xff1876d1),
                    fontSize: 15,
                  ),
                ),
              ),
              if (onTap != null)
                const Padding(
                  padding: EdgeInsets.only(left: 4),
                  child: Icon(Icons.chevron_right, color: Color(0xff999999)),
                ),
            ],
          ),
        ),
      );

  Widget _buildSettingsPanel(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
  ) => Container(
    margin: const EdgeInsets.only(bottom: 18),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'RTU 设置',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 4),
        _settingLine(
          '系统时间',
          _systemTimeController.text,
          _connected && !_busy
              ? () => _editSetting(
                '系统时间（yyyy-MM-dd HH:mm:ss）',
                _systemTimeController,
                () => _action('setSystemTime', {
                  'value': _systemTimeController.text,
                }),
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '设备MN',
          _mnController.text,
          _connected && !_busy
              ? () => _editSetting(
                '设备MN',
                _mnController,
                () => _action('setMn', {'value': _mnController.text}),
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '系统编码',
          _systemStController.text,
          _connected && !_busy
              ? () => _editSetting(
                '系统编码',
                _systemStController,
                () =>
                    _action('setSystemSt', {'value': _systemStController.text}),
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '采集间隔（分钟）',
          _sampleIntervalController.text,
          _connected && !_busy
              ? () => _editSetting(
                '采集间隔（分钟）',
                _sampleIntervalController,
                () => _action('setSampleInterval', {
                  'minutes': int.tryParse(_sampleIntervalController.text) ?? 0,
                }),
                keyboard: TextInputType.number,
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '上传间隔（分钟）',
          _uploadIntervalController.text,
          _connected && !_busy
              ? () => _editSetting(
                '上传间隔（分钟）',
                _uploadIntervalController,
                () => _action('setUploadInterval', {
                  'minutes': int.tryParse(_uploadIntervalController.text) ?? 0,
                }),
                keyboard: TextInputType.number,
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '通信模组',
          _moduleText(int.tryParse(_lotTypeController.text)),
          _connected && !_busy
              ? () => _editSetting(
                '通信模组（00 NB / 01 4G）',
                _lotTypeController,
                () => _action('setLotType', {
                  'type': int.tryParse(_lotTypeController.text) ?? 0,
                }),
                keyboard: TextInputType.number,
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '连接设备类型',
          _sensorDeviceTypeText(int.tryParse(_sensorDeviceTypeController.text)),
          _connected && !_busy ? _selectSensorDeviceType : null,
        ),
        const Divider(height: 1),
        _settingLine(
          '上电延时（秒）',
          _sensorWaitController.text,
          _connected && !_busy
              ? () => _editSetting(
                '上电延时（秒）',
                _sensorWaitController,
                () => _action('setSensorWait', {
                  'seconds': int.tryParse(_sensorWaitController.text) ?? 0,
                }),
                keyboard: TextInputType.number,
              )
              : null,
        ),
        const Divider(height: 1),
        _settingLine(
          'RTU蓝牙地址',
          _addressController.text,
          _connected && !_busy
              ? () => _editSetting(
                'RTU蓝牙地址（修改后请重新连接）',
                _addressController,
                () => _action('setBleAddress', {
                  'value': _addressController.text,
                }),
              )
              : null,
        ),
        const SizedBox(height: 14),
        Row(
          children: [
            const Text(
              '通讯服务',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            TextButton.icon(
              onPressed: _connected && !_busy ? _addNetwork : null,
              icon: const Icon(Icons.add),
              label: const Text('新增通讯'),
            ),
          ],
        ),
        if (!_networkAdded.any((item) => item))
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 8),
            child: Text('暂无已启用的通讯服务'),
          ),
        ...List.generate(
          5,
          (index) =>
              _networkAdded[index] ? _networkRow(index) : const SizedBox(),
        ),
        const SizedBox(height: 8),
        Center(
          child: OutlinedButton.icon(
            onPressed: _connected && !_busy ? _upgrade : null,
            icon: const Icon(Icons.upload_file),
            label: const Text('选择协议文件并升级'),
          ),
        ),
      ],
    ),
  );

  List<Map<String, dynamic>> _flowConfigFields() {
    final shape = _flowRegisters[40085] ?? 0;
    final shapeLabel = switch (shape) {
      1 => '断面底宽',
      2 => '断面角度',
      3 || 4 => '断面半径',
      _ => '断面宽度',
    };
    final shapeUnit = shape == 2 ? '°' : 'm';
    return [
      {
        'address': 40054,
        'count': 1,
        'type': 'scaled',
        'scale': 0.001,
        'label': '安装高度',
        'unit': 'm',
      },
      {
        'address': 40058,
        'count': 2,
        'type': 'signed32',
        'scale': 0.001,
        'label': '河底高程',
        'unit': 'm',
      },
      {
        'address': 40155,
        'count': 1,
        'type': 'uint',
        'label': '安装方向',
        'options': <int, String>{0: '正向', 1: '反向'},
      },
      {
        'address': 40097,
        'count': 1,
        'type': 'uint',
        'label': '流速方向滤除',
        'options': <int, String>{0: '不滤除', 1: '滤除正向', 2: '滤除反向'},
      },
      {'address': 40105, 'count': 1, 'type': 'uint', 'label': '发射功率'},
      {
        'address': 40094,
        'count': 1,
        'type': 'uint',
        'label': '灵敏度',
        'options': <int, String>{1: '低', 5: '中', 10: '高'},
      },
      {
        'address': 40085,
        'count': 1,
        'type': 'uint',
        'label': '断面形状',
        'options': <int, String>{
          0: '矩形',
          1: '梯形',
          2: 'V形',
          3: 'U形',
          4: '圆形',
          5: '不规则',
        },
      },
      {
        'address': 40086,
        'count': 2,
        'type': 'float',
        'label': shapeLabel,
        'unit': shapeUnit,
      },
      if (shape == 1) ...[
        {
          'address': 40088,
          'count': 2,
          'type': 'float',
          'label': '断面顶宽',
          'unit': 'm',
        },
        {
          'address': 40090,
          'count': 2,
          'type': 'float',
          'label': '断面高度',
          'unit': 'm',
        },
      ],
      {
        'address': 40092,
        'count': 2,
        'type': 'float',
        'label': '淤积厚度',
        'unit': 'm',
      },
      {
        'address': 40100,
        'count': 3,
        'type': 'total',
        'label': '累计流量初始值',
        'unit': 'm³',
      },
      {
        'address': 40109,
        'count': 3,
        'type': 'total',
        'label': '正向累计流量初始值',
        'unit': 'm³',
      },
      {
        'address': 40115,
        'count': 3,
        'type': 'total',
        'label': '反向累计流量初始值',
        'unit': 'm³',
      },
      {
        'address': 40121,
        'count': 3,
        'type': 'total',
        'label': '绝对值累计流量初始值',
        'unit': 'm³',
      },
    ];
  }

  Widget _flowConfigRow(Map<String, dynamic> field) {
    final unit = field['unit'] == null ? '' : ' ${field['unit']}';
    return InkWell(
      onTap: _busy ? null : () => _editFlowField(field),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: Color(0xffeeeeee))),
        ),
        child: Row(
          children: [
            Expanded(child: Text(field['label'] as String)),
            Flexible(
              child: Text(
                '${_flowFieldValue(field)}$unit',
                textAlign: TextAlign.end,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: Color(0xff1876d1)),
              ),
            ),
            const Icon(Icons.chevron_right, color: Color(0xff999999)),
          ],
        ),
      ),
    );
  }

  Widget _buildFlowBaseConfigScaffold() {
    final fields = _flowConfigFields();
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xff1464d9),
        foregroundColor: Colors.white,
        leading: IconButton(
          onPressed: () => setState(() => _flowBaseConfigVisible = false),
          icon: const Icon(Icons.arrow_back),
        ),
        title: const Text('RTU流量计-基础配置'),
        actions: [
          IconButton(
            onPressed: _busy ? null : _readAllFlowConfig,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(14, 12, 14, 28),
            children: [
              const Text(
                '蓝牙透传 - 485 串口通信（从站地址 0x4A）',
                style: TextStyle(color: Color(0xff666666)),
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  border: Border.all(color: const Color(0xffdddddd)),
                  borderRadius: BorderRadius.circular(5),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '手动读取 485 寄存器',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _flowReadAddressController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '起始地址',
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: TextField(
                            controller: _flowReadCountController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '读取数量',
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed:
                              _busy
                                  ? null
                                  : () => _readFlowRegisters(
                                    int.tryParse(
                                          _flowReadAddressController.text,
                                        ) ??
                                        0,
                                    int.tryParse(
                                          _flowReadCountController.text,
                                        ) ??
                                        0,
                                  ),
                          child: const Text('读取'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      '手动写入单个整数寄存器',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _flowWriteAddressController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '写入地址',
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: TextField(
                            controller: _flowWriteValueController,
                            keyboardType: TextInputType.number,
                            decoration: const InputDecoration(
                              labelText: '整数值',
                              isDense: true,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed:
                              _busy
                                  ? null
                                  : () async {
                                    final address =
                                        int.tryParse(
                                          _flowWriteAddressController.text,
                                        ) ??
                                        0;
                                    final value = int.tryParse(
                                      _flowWriteValueController.text,
                                    );
                                    if (address < 40001 || value == null) {
                                      _show('请输入正确的写入地址和整数值');
                                      return;
                                    }
                                    final saved = await _writeFlowRegisters(
                                      address,
                                      [value],
                                      label: '485寄存器',
                                    );
                                    if (saved)
                                      await _readFlowRegisters(address, 1);
                                  },
                          child: const Text('写入'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      '基础配置',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  TextButton.icon(
                    onPressed: _busy ? null : _readAllFlowConfig,
                    icon: const Icon(Icons.download),
                    label: const Text('读取基础配置'),
                  ),
                ],
              ),
              ...fields.map(_flowConfigRow),
              const SizedBox(height: 14),
              InkWell(
                onTap:
                    () => setState(
                      () => _flowConfigMoreVisible = !_flowConfigMoreVisible,
                    ),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Row(
                    children: [
                      const Expanded(
                        child: Text(
                          '水深 - 系数（0 - 9 号）',
                          style: TextStyle(
                            fontSize: 17,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      Icon(
                        _flowConfigMoreVisible
                            ? Icons.keyboard_arrow_up
                            : Icons.keyboard_arrow_down,
                      ),
                    ],
                  ),
                ),
              ),
              if (_flowConfigMoreVisible)
                ...List.generate(10, (slot) {
                  final address = 40200 + slot * 4;
                  final water =
                      _flowRegisters.containsKey(address)
                          ? _floatFromLowHigh(
                            _flowRegisters[address]!,
                            _flowRegisters[address + 1]!,
                          )
                          : null;
                  final factor =
                      _flowRegisters.containsKey(address + 2)
                          ? _floatFromLowHigh(
                            _flowRegisters[address + 2]!,
                            _flowRegisters[address + 3]!,
                          )
                          : null;
                  return InkWell(
                    onTap: _busy ? null : () => _editFlowCoefficient(slot),
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 11),
                      decoration: const BoxDecoration(
                        border: Border(
                          bottom: BorderSide(color: Color(0xffeeeeee)),
                        ),
                      ),
                      child: Row(
                        children: [
                          Expanded(child: Text('$slot 号水深-系数')),
                          Text(
                            '${water?.toStringAsFixed(3) ?? '-'} m  /  ${factor?.toStringAsFixed(3) ?? '-'}',
                            style: const TextStyle(color: Color(0xff1876d1)),
                          ),
                          const Icon(
                            Icons.chevron_right,
                            color: Color(0xff999999),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
            ],
          ),
          if (_busy) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _buildLoadingOverlay() => Positioned.fill(
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

  @override
  Widget build(BuildContext context) {
    if (_flowBaseConfigVisible) return _buildFlowBaseConfigScaffold();
    final basic = _basic ?? const <String, dynamic>{};
    final lot = _lot ?? const <String, dynamic>{};
    final sensor = _sensor ?? const <String, dynamic>{};
    return Scaffold(
      appBar: AppBar(
        backgroundColor: const Color(0xff1464d9),
        foregroundColor: Colors.white,
        leading: IconButton(
          onPressed: () => context.go('/more'),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(
          (basic['deviceName']?.toString().trim().isNotEmpty == true)
              ? basic['deviceName'].toString()
              : 'RTU 采集器',
        ),
        actions: [
          IconButton(
            tooltip: _connected ? '蓝牙已连接，点击管理' : '扫描 RTU 蓝牙设备',
            onPressed: _busy ? null : _showBluetoothActions,
            icon: Icon(
              _connected
                  ? Icons.bluetooth_connected
                  : Icons.bluetooth_searching,
            ),
          ),
          IconButton(
            onPressed: _busy ? null : _readAll,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: Stack(
        children: [
          ListView(
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 24),
            children: [
              _buildDemoInteraction(basic),
              _buildDemoDeviceInfo(basic, lot),
              if (_activeTab == 'swj') _buildWaterPanel(sensor),
              if (_activeTab == 'llj') _buildFlowPanel(sensor),
              if (_activeTab == 'settings') _buildSettingsPanel(basic, lot),
              /* Obsolete pre-migration layout retained in source history only.
                _card(
                  '连接',
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _addressController,
                              decoration: const InputDecoration(
                                labelText: 'RTU 蓝牙地址（15xxxxxxxxxx）',
                                isDense: true,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          ElevatedButton(
                            onPressed: _scan,
                            child: const Text('扫描'),
                          ),
                        ],
                      ),
                      Wrap(
                        spacing: 8,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          Text('状态：$_status'),
                          if (_connected)
                            OutlinedButton(
                              onPressed: _disconnect,
                              child: const Text('断开蓝牙'),
                            )
                          else
                            ElevatedButton(
                              onPressed: _connect,
                              child: const Text('手动连接'),
                            ),
                        ],
                      ),
                    ],
                  ),
                ),
                _card(
                  'RTU 基本信息',
                  Wrap(
                    children: [
                      _kv('固件版本', basic['version']),
                      _kv('系统时间', basic['systemTime']),
                      _kv('设备名称', basic['deviceName']),
                      _kv('蓝牙地址', basic['bleAddress']),
                      _kv('MN', basic['mn']),
                      _kv('电池电压', basic['batteryVoltage']),
                      _kv('设备编号', basic['deviceNumber']),
                      _kv('工作状态', basic['deviceState']),
                      _kv('连接设备位图', basic['sensorDeviceType']),
                      _kv('长供电', basic['powerEnabled']),
                    ],
                  ),
                ),
                _card(
                  '物联网信息',
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        children: [
                          _kv('通信模组', lot['moduleType']),
                          _kv('本地 IP', lot['localIp']),
                          _kv('CCID', lot['ccid']),
                          _kv('IMSI', lot['imsi']),
                          _kv('信号强度', lot['signal']),
                          _kv('待上传数据', lot['pendingCount']),
                          _kv('上传状态', lot['uploadState']),
                          _kv('上传错误', lot['uploadError']),
                          _kv('上传步骤', lot['uploadStep']),
                          _kv('最近上传', lot['uploadEndTime']),
                          _kv('系统 ST', lot['systemSt']),
                        ],
                      ),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          _field(
                            '采样间隔（分钟）',
                            _sampleIntervalController,
                            keyboard: TextInputType.number,
                          ),
                          ElevatedButton(
                            onPressed:
                                () => _action('setSampleInterval', {
                                  'minutes':
                                      int.tryParse(
                                        _sampleIntervalController.text,
                                      ) ??
                                      0,
                                }),
                            child: const Text('设置'),
                          ),
                          _field(
                            '上传间隔（分钟）',
                            _uploadIntervalController,
                            keyboard: TextInputType.number,
                          ),
                          ElevatedButton(
                            onPressed:
                                () => _action('setUploadInterval', {
                                  'minutes':
                                      int.tryParse(
                                        _uploadIntervalController.text,
                                      ) ??
                                      0,
                                }),
                            child: const Text('设置'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Align(
                        alignment: Alignment.centerLeft,
                        child: ElevatedButton.icon(
                          onPressed: _addNetwork,
                          icon: const Icon(Icons.add),
                          label: const Text('一点多传 / 新增通讯'),
                        ),
                      ),
                      ...List.generate(5, (i) => _networkRow(i)),
                    ],
                  ),
                ),
                _card(
                  '传感器',
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          const Text('类型：'),
                          DropdownButton<String>(
                            value: _sensorType,
                            items: const [
                              DropdownMenuItem(
                                value: 'llj',
                                child: Text('流量计 LLJ'),
                              ),
                              DropdownMenuItem(
                                value: 'swj',
                                child: Text('雷达水位计 SWJ'),
                              ),
                            ],
                            onChanged: (v) async {
                              if (v == null) return;
                              setState(() {
                                _sensorType = v;
                                _sensor = null;
                              });
                              await _readSelectedSensor();
                            },
                          ),
                          if (_sensorType != 'llj') ...[
                            OutlinedButton(
                              onPressed: () => _action('start'),
                              child: const Text('开始'),
                            ),
                            OutlinedButton(
                              onPressed: () => _action('stop'),
                              child: const Text('停止'),
                            ),
                          ],
                          if (_sensorType == 'llj') ...[
                            OutlinedButton(
                              onPressed:
                                  () => setState(
                                    () =>
                                        _flowBasicExpanded =
                                            !_flowBasicExpanded,
                                  ),
                              child: const Text('基础配置'),
                            ),
                            OutlinedButton(
                              onPressed:
                                  () => _action(_measuring ? 'stop' : 'start'),
                              child: Text(_measuring ? '关闭调试模式' : '打开调试模式'),
                            ),
                          ],
                          if (_sensorType == 'llj')
                            OutlinedButton(
                              onPressed: () => _action('zero'),
                              child: const Text('累计清零'),
                            ),
                          if (_sensorType == 'llj')
                            OutlinedButton(
                              onPressed:
                                  _updating ? null : () => _action('upload'),
                              child: Text(_updating ? '上报中…' : '采集上报'),
                            ),
                        ],
                      ),
                      if (_sensorType == 'swj')
                        Padding(
                          padding: const EdgeInsets.only(top: 10),
                          child: Row(
                            children: [
                              Expanded(
                                child: ElevatedButton(
                                  onPressed: () => _action('start'),
                                  child: const Text('开始测量'),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: ElevatedButton(
                                  onPressed: () => _action('stop'),
                                  child: const Text('停止测量'),
                                ),
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: ElevatedButton(
                                  onPressed:
                                      _updating
                                          ? null
                                          : () => _action('upload'),
                                  child: Text(_updating ? '上报中…' : '采集上报'),
                                ),
                              ),
                            ],
                          ),
                        ),
                      if (_sensorType == 'llj' && _flowBasicExpanded)
                        Wrap(
                          spacing: 8,
                          children: [
                            _field(
                              '流量计上电延时（秒）',
                              _sensorWaitController,
                              keyboard: TextInputType.number,
                            ),
                            ElevatedButton(
                              onPressed:
                                  () => _action('setSensorWait', {
                                    'seconds':
                                        int.tryParse(
                                          _sensorWaitController.text,
                                        ) ??
                                        0,
                                  }),
                              child: const Text('保存基础配置'),
                            ),
                          ],
                        ),
                      Wrap(
                        children: [
                          _kv('上电延时', sensor['sampleWait']),
                          _kv('水位', sensor['waterLevel']),
                          _kv('深度', sensor['depth']),
                          _kv('高度', sensor['height']),
                          _kv('方向', sensor['direction']),
                          _kv('表面流速', sensor['surfaceVelocity']),
                          _kv('断面流速', sensor['sectionVelocity']),
                          _kv('流量', sensor['rateFlow']),
                          _kv('间隔流量', sensor['intervalFlow']),
                          _kv('累计流量', sensor['cumulativeFlowTotal']),
                          _kv('二次累计', sensor['secondaryCumulativeFlowTotal']),
                          _kv('夜间累计', sensor['nightCumulativeFlowTotal']),
                          _kv('绝对累计', sensor['absoluteCumulativeFlowTotal']),
                          _kv('横滚', sensor['roll']),
                          _kv('俯仰', sensor['pitch']),
                          _kv('电池电压', sensor['batteryVoltage']),
                          _kv('厚度', sensor['thickness']),
                          _kv('过水面积', sensor['area']),
                          _kv('pH', sensor['ph']),
                          _kv('溶解氧', sensor['oxygen']),
                          _kv('电导率', sensor['conductivity']),
                          _kv('浊度', sensor['turbidity']),
                          _kv('ORP', sensor['orp']),
                          _kv('雨量', sensor['rain']),
                        ],
                      ),
                    ],
                  ),
                ),
                _card(
                  'RTU 设置',
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      _field('设备 MN', _mnController),
                      ElevatedButton(
                        onPressed:
                            () =>
                                _action('setMn', {'value': _mnController.text}),
                        child: const Text('写入 MN'),
                      ),
                      _field('RTU 蓝牙地址', _addressController),
                      ElevatedButton(
                        onPressed:
                            () => _action('setBleAddress', {
                              'value': _addressController.text,
                            }),
                        child: const Text('写入地址'),
                      ),
                      _field(
                        '通信模组类型',
                        _lotTypeController,
                        keyboard: TextInputType.number,
                      ),
                      ElevatedButton(
                        onPressed:
                            () => _action('setLotType', {
                              'type':
                                  int.tryParse(_lotTypeController.text) ?? 0,
                            }),
                        child: const Text('写入模组'),
                      ),
                      _field('系统 ST', _systemStController),
                      ElevatedButton(
                        onPressed:
                            () => _action('setSystemSt', {
                              'value': _systemStController.text,
                            }),
                        child: const Text('写入 ST'),
                      ),
                      _field(
                        '连接设备位图',
                        _sensorDeviceTypeController,
                        keyboard: TextInputType.number,
                      ),
                      ElevatedButton(
                        onPressed:
                            () => _action('setSensorDeviceType', {
                              'value':
                                  int.tryParse(
                                    _sensorDeviceTypeController.text,
                                  ) ??
                                  0,
                            }),
                        child: const Text('写入设备类型'),
                      ),
                      _field('系统时间 yyyy-MM-dd HH:mm:ss', _systemTimeController),
                      ElevatedButton(
                        onPressed:
                            () => _action('setSystemTime', {
                              'value': _systemTimeController.text,
                            }),
                        child: const Text('写入时间'),
                      ),
                      _field(
                        '传感器上电延时（秒）',
                        _sensorWaitController,
                        keyboard: TextInputType.number,
                      ),
                      ElevatedButton(
                        onPressed:
                            () => _action('setSensorWait', {
                              'seconds':
                                  int.tryParse(_sensorWaitController.text) ?? 0,
                            }),
                        child: const Text('写入延时'),
                      ),
                      Switch(
                        value: (basic['powerEnabled'] as num?)?.toInt() == 1,
                        onChanged: (v) => _action('setPower', {'enabled': v}),
                      ),
                      const Text('长供电'),
                    ],
                  ),
                ),
                _card(
                  '协议升级与调试',
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          ElevatedButton(
                            onPressed: _upgrade,
                            child: const Text('选择 .bin 并发送'),
                          ),
                          SizedBox(
                            width: 240,
                            child: TextField(
                              controller: _commandController,
                              decoration: const InputDecoration(
                                labelText: '自定义 RTU 命令',
                                isDense: true,
                              ),
                            ),
                          ),
                          OutlinedButton(
                            onPressed:
                                () => _action('sendCommand', {
                                  'command': _commandController.text,
                                }),
                            child: const Text('发送'),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        '升级文件协议需以设备固件实际握手为准；发送前请确认设备处于升级模式。',
                        style: TextStyle(color: Colors.orange),
                      ),
                    ],
                  ),
                ),
              ],*/
            ],
          ),
          if (_busy) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _networkRow(int index) {
    final c = _networkControllers[index];
    Widget infoCell(String label, String value) => Expanded(
      child: Padding(
        padding: const EdgeInsets.only(right: 8, bottom: 8),
        child: RichText(
          overflow: TextOverflow.ellipsis,
          text: TextSpan(
            style: const TextStyle(fontSize: 14, color: Color(0xff333333)),
            children: [
              TextSpan(text: '$label：'),
              TextSpan(
                text: value.isEmpty ? '-' : value,
                style: const TextStyle(color: Color(0xff1876d1)),
              ),
            ],
          ),
        ),
      ),
    );
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: _connected && !_busy ? () => _showNetworkEditor(index) : null,
        borderRadius: BorderRadius.circular(5),
        child: Container(
          decoration: BoxDecoration(
            border: Border.all(color: const Color(0xffd9d9d9)),
            borderRadius: BorderRadius.circular(5),
          ),
          child: Padding(
            padding: const EdgeInsets.fromLTRB(10, 6, 4, 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      '通讯服务 ${index + 1}',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const Spacer(),
                    TextButton(
                      onPressed:
                          _connected && !_busy
                              ? () => _showNetworkEditor(index)
                              : null,
                      child: const Text('配置'),
                    ),
                    TextButton(
                      onPressed:
                          _connected && !_busy
                              ? () => _removeNetwork(index)
                              : null,
                      style: TextButton.styleFrom(foregroundColor: Colors.red),
                      child: const Text('删除'),
                    ),
                  ],
                ),
                const Divider(height: 8),
                Row(
                  children: [
                    infoCell('通讯IP', c.host.text),
                    infoCell('端口', c.port.text),
                  ],
                ),
                Row(
                  children: [
                    infoCell(
                      '上传协议',
                      _protocolText(int.tryParse(c.protocol.text)),
                    ),
                    infoCell('通讯状态', c.enabled ? '已启用' : '已停用'),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NetworkControllers {
  final host = TextEditingController();
  final port = TextEditingController();
  final protocol = TextEditingController(text: '0');
  int protocolValue = 0;
  bool enabled = false;

  void load(Map<String, dynamic> value) {
    host.text = value['host']?.toString() ?? '';
    port.text = value['port']?.toString() ?? '';
    protocol.text = value['protocol']?.toString() ?? '0';
    enabled = value['enabled'] == true;
  }

  void dispose() {
    host.dispose();
    port.dispose();
    protocol.dispose();
  }
}
