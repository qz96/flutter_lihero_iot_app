import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

/// CYX7 扫码工具：复用项目已有的 `/qrCodeScan` 页面（mobile_scanner）。
///
/// 原程序用 ZXing 的 CaptureActivity 扫码，返回内容约定一致：
///   - 采样瓶二维码：12 位，前缀 `0B` / `0C`
///   - 箱体二维码：12 位，前缀 `0D`（RTU 子页要求 `0D07`）
class Cyx7Scan {
  Cyx7Scan._();

  static const int bottleCodeLength = 12;
  static const List<String> bottlePrefixes = ['0B', '0C'];
  static const String boxPrefix = '0D';
  static const String boxRtuPrefix = '0D07';

  /// 打开扫码页，返回二维码原文（取消或空值返回 null）。
  static Future<String?> scan(BuildContext context) async {
    final barcode = await context.push<Barcode>('/qrCodeScan?isProvisioning=false');
    final value = barcode?.rawValue?.trim();
    if (value == null || value.isEmpty) return null;
    return value;
  }

  static bool isBottleCode(String code) =>
      code.length == bottleCodeLength && bottlePrefixes.any(code.startsWith);

  static bool isBoxCode(String code) =>
      code.length == bottleCodeLength && code.startsWith(boxPrefix);
}
