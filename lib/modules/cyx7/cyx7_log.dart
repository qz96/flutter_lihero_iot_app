import 'package:flutter/services.dart';

/// 采样操作日志上报（原工程 `RetrofitUtils.getApiService().insertLog(...)`）。
///
/// 原程序三个页面都会在 `sendLHBleData` 里对**每条下发指令**调一次 `insertLog`：
///   `LiherCYX7Activity`（井下采样器主页）  sampleId = ""（页面字段初值，始终为空）
///   `LiherCYXParamSetActivity`（参数校准）  sampleId = "LhRTULLJ"（固定）
///   `LiherCYXRTUActivity`（RTU 功能入口）   sampleId = "LhCYXRTU"（固定）
///
/// 报文与接口与原工程一致：
///   `POST http://city.lihero.com:3001/sample/bottle/log/insertLog`
///   body `{blueMac, sampleId, operator, logContent}`，`Authorization: Bearer <token>`
/// 其中 `blueMac` 为空时不上报（对应原程序 `StringUtils.isEmpty(blueMac)` 分支），
/// token 为空也照常上报（只是不带鉴权头），失败静默（原程序 onError 为空实现）。
class Cyx7Log {
  Cyx7Log._();

  static const _channel = MethodChannel('lihero_cyx7');

  /// 三个页面共用同一份配置（在井下采样器页面的「采样日志上报设置」里配置）。
  static String url = 'http://city.lihero.com:3001/sample/bottle/log/insertLog';
  static String token = '';
  static String operator = '';

  /// 当前连接设备的蓝牙地址（主页面连接/刷新时维护）。
  static String blueMac = '';

  /// 上报一条指令日志；`sampleId` 由各页面按原程序传入。
  static Future<void> upload({
    required String sampleId,
    required String content,
  }) async {
    if (content.isEmpty || blueMac.isEmpty) return;
    try {
      await _channel.invokeMethod('insertLog', {
        'url': url,
        'token': token,
        'blueMac': blueMac,
        'sampleId': sampleId,
        'operator': operator,
        'logContent': content,
      });
    } on PlatformException {
      // 与原程序一致：上报失败静默处理
    }
  }
}
