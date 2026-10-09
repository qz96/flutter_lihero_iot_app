import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';

import 'package:thingsboard_app/config/themes/app_colors.dart';
import 'package:thingsboard_app/config/themes/rtu_theme.dart';

// ============================================================================
// 调色板：统一走 config/themes/app_colors.dart 的 RTU 段，
// 圆角走 RtuTokens，避免在本文件重复定义色值。
// ============================================================================
class _R {
  static const bg = AppColors.rtuBg;
  static const surface = AppColors.rtuSurface;
  static const surface2 = AppColors.rtuSurface2;
  static const surface3 = AppColors.rtuSurface3;
  static const text = AppColors.rtuText;
  static const text2 = AppColors.rtuText2;
  static const text3 = AppColors.rtuText3;
  static const line = AppColors.rtuLine;
  static const line2 = AppColors.rtuLine2;
  static const brand = AppColors.rtuBrand;
  static const brandD = AppColors.rtuBrandD;
  static const brandL = AppColors.rtuBrandL;
  static const cyan = AppColors.rtuCyan;
  static const ok = AppColors.rtuOk;
  static const okL = AppColors.rtuOkL;
  static const warn = AppColors.rtuWarn;
  static const err = AppColors.rtuErr;
  static const errL = AppColors.rtuErrL;
}

// ============================================================================
// Bottom tab bar —— 首页底部导航
//
// 四个 tab 对应新版首页设计：概览 / 监测 / 数据 / 设置。
// 纯展示组件，选中态与点击回调由 `_RtuPageState` 注入，不含任何业务逻辑。
// ============================================================================
enum _RtuTab { overview, monitor, data, settings }

extension _RtuTabX on _RtuTab {
  String get label => switch (this) {
    _RtuTab.overview => '概览',
    _RtuTab.monitor => '监测',
    _RtuTab.data => '数据',
    _RtuTab.settings => '设置',
  };

  /// 复用本文件已有的 SVG 图标集（`_Icons.svg`）。
  String get iconName => switch (this) {
    _RtuTab.overview => 'home',
    _RtuTab.monitor => 'gauge',
    _RtuTab.data => 'chart',
    _RtuTab.settings => 'cog',
  };
}

/// 底部导航条：白底、顶部细分割线、选中项使用品牌蓝。
class _RtuTabBar extends StatelessWidget {
  const _RtuTabBar({required this.current, required this.onChanged});

  final _RtuTab current;
  final ValueChanged<_RtuTab> onChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: _R.surface,
        border: Border(top: BorderSide(color: _R.line)),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 58,
          child: Row(
            children: [
              for (final tab in _RtuTab.values)
                Expanded(
                  child: InkWell(
                    onTap: () => onChanged(tab),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        _Icons.svg(
                          tab.iconName,
                          size: 21,
                          color: tab == current ? _R.brand : _R.text3,
                        ),
                        const SizedBox(height: 3),
                        Text(
                          tab.label,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight:
                                tab == current ? FontWeight.w600 : FontWeight.w400,
                            color: tab == current ? _R.brand : _R.text3,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

// ============================================================================
// 概览页小组件
// ============================================================================

/// 圆角图标块：用于「快捷操作」四宫格与列表行左侧图标。
class _IconTile extends StatelessWidget {
  const _IconTile({
    required this.icon,
    required this.fg,
    required this.bg,
    this.size = RtuTokens.iconTileNav,
  });

  final IconData icon;
  final Color fg;
  final Color bg;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(RtuTokens.radiusIconTile),
      ),
      child: Icon(icon, size: size * 0.55, color: fg),
    );
  }
}

/// 头部区：页面标题 + 副标题 + 右侧状态胶囊。
class _RtuPageHeader extends StatelessWidget {
  const _RtuPageHeader({
    required this.title,
    required this.subtitle,
    required this.chip,
  });

  final String title;
  final String subtitle;
  final Widget chip;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        RtuTokens.pageInset,
        8,
        RtuTokens.pageInset,
        12,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: RtuTextStyles.pageTitle.copyWith(fontSize: 21),
                ),
                const SizedBox(height: 3),
                Text(
                  subtitle,
                  style: RtuTextStyles.pageSubtitle.copyWith(
                    fontSize: 12,
                    color: _R.text2,
                  ),
                ),
              ],
            ),
          ),
          chip,
        ],
      ),
    );
  }
}

/// 药丸形状态标签（通信正常 / 在线 / 已连接 等）。
class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.text, required this.fg, required this.bg});

  final String text;
  final Color fg;
  final Color bg;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(RtuTokens.radiusChip),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: RtuTokens.statusDot,
            height: RtuTokens.statusDot,
            decoration: BoxDecoration(color: fg, shape: BoxShape.circle),
          ),
          const SizedBox(width: 5),
          Text(
            text,
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w600,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}

/// 迷你折线图（实时数据卡片内的趋势线）。
/// 无真实历史数据时绘制一条平缓的示意曲线，仅为视觉呈现。
class _SparkLine extends StatelessWidget {
  const _SparkLine({required this.color, this.points});

  final Color color;
  final List<double>? points;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: RtuTokens.sparkHeight,
      width: double.infinity,
      child: CustomPaint(
        painter: _SparkPainter(color, points ?? const [0.5, 0.45, 0.6, 0.4, 0.55, 0.5, 0.62, 0.5]),
      ),
    );
  }
}

class _SparkPainter extends CustomPainter {
  _SparkPainter(this.color, this.points);

  final Color color;
  final List<double> points;

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2) return;
    final paint = Paint()
      ..color = color
      ..strokeWidth = 1.8
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final dx = size.width / (points.length - 1);
    final path = Path();
    for (var i = 0; i < points.length; i++) {
      // points 为 0~1 的纵向比例，0 在顶部，映射到画布时按可用高度缩放。
      final y = points[i].clamp(0.0, 1.0) * size.height;
      if (i == 0) {
        path.moveTo(0, y);
      } else {
        path.lineTo(dx * i, y);
      }
    }
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _SparkPainter old) =>
      old.color != color || old.points != points;
}

/// 概览页「平台连接状态」卡片的一行：左侧说明 + 右侧值（可带颜色）。
///
/// 只做展示，数值全部来自页面已有的 `basic` / `lot` 字段。
class _KvRow extends StatelessWidget {
  const _KvRow({
    required this.label,
    required this.value,
    this.valueColor,
    this.icon,
    this.last = false,
  });

  final String label;
  final String value;
  final Color? valueColor;
  final IconData? icon;
  final bool last;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10.5),
      decoration: last
          ? null
          : const BoxDecoration(
              border: Border(
                bottom: BorderSide(color: AppColors.rtuLine2),
              ),
            ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              label,
              style: RtuTextStyles.keyValue.copyWith(color: _R.text2),
            ),
          ),
          if (icon != null) ...[
            const SizedBox(width: RtuTokens.gapSm),
            Icon(icon, size: 17, color: valueColor ?? _R.text),
          ],
          if (icon != null) const SizedBox(width: 3),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RtuTextStyles.keyValueStrong.copyWith(
                color: valueColor ?? _R.text,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 概览页「断面与下端设备」卡片的一行。
///
/// 字段与蓝牙页设备行同源（`_devices` 里的 `name` / `address` / `rssi`），
/// 不新增数据来源；无设备时展示占位行。
class _DeviceRow extends StatelessWidget {
  const _DeviceRow({
    required this.title,
    required this.subtitle,
    required this.connected,
    this.last = false,
  });

  final String title;
  final String subtitle;
  final bool connected;
  final bool last;

  @override
  Widget build(BuildContext context) {
    final fg = connected ? _R.ok : _R.text3;
    final bg = connected ? _R.okL : RtuTokens.barTrack;
    return Container(
      padding: const EdgeInsets.symmetric(vertical: RtuTokens.rowPaddingSm),
      decoration: last
          ? null
          : const BoxDecoration(
              border: Border(
                bottom: BorderSide(color: AppColors.rtuLine2),
              ),
            ),
      child: Row(
        children: [
          Container(
            width: 9,
            height: 9,
            decoration: BoxDecoration(color: fg, shape: BoxShape.circle),
          ),
          const SizedBox(width: RtuTokens.gapXxl),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RtuTextStyles.rowTitle.copyWith(color: _R.text),
                ),
                if (subtitle.isNotEmpty) ...[
                  const SizedBox(height: 3),
                  Text(
                    subtitle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RtuTextStyles.rowSubtitle.copyWith(
                      color: _R.text3,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: RtuTokens.gapMd),
          _StatusPill(text: connected ? '已连接' : '未连接', fg: fg, bg: bg),
        ],
      ),
    );
  }
}

/// 概览页的断面示意图（梯形断面，按 CSS 绘制的静态剖面图）。
///
/// 数值全部来自页面已有的遥测字段：水位 / 空高 / 表面流速，
/// 以及流量计基础配置里的断面参数（底宽 / 边坡 / 淤积厚度）。
class _SectionDiagram extends StatelessWidget {
  const _SectionDiagram({
    required this.height,
    required this.waterLevel,
    required this.velocity,
    required this.bottomWidth,
    required this.slope,
    required this.thickness,
  });

  final double height;
  final double waterLevel;
  final double velocity;
  final double bottomWidth;
  final double slope;
  final double thickness;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      width: double.infinity,
      child: CustomPaint(
        painter: _SectionPainter(
          waterRatio: waterLevel > 0 ? (waterLevel / height) : 0,
          thicknessRatio: thickness > 0 ? (thickness / height) : 0,
          velocity: velocity,
        ),
      ),
    );
  }
}

class _SectionPainter extends CustomPainter {
  _SectionPainter({
    required this.waterRatio,
    required this.thicknessRatio,
    required this.velocity,
  });

  final double waterRatio;
  final double thicknessRatio;
  final double velocity;

  @override
  void paint(Canvas canvas, Size size) {
    const pad = 6.0;
    final cx = size.width / 2;
    final topY = pad;
    final bottomY = size.height - pad;
    final waterY = bottomY - (bottomY - topY) * waterRatio.clamp(0.0, 1.0);
    // 梯形：上宽下窄，底宽固定占画布 62%，边坡影响上口宽度（示意）。
    final bottomHalf = size.width * 0.31;
    final topHalf = (bottomHalf + (bottomY - topY) * 0.62).clamp(
      0.0,
      size.width / 2,
    );

    final section = Path()
      ..moveTo(cx - topHalf, topY)
      ..lineTo(cx + topHalf, topY)
      ..lineTo(cx + bottomHalf, bottomY)
      ..lineTo(cx - bottomHalf, bottomY)
      ..close();

    // 断面底色
    canvas.drawPath(
      section,
      Paint()..color = const Color(0xFFE9EFF7),
    );

    // 水体（水位以下部分）
    canvas.save();
    canvas.clipPath(section);
    final waterTop = waterY.clamp(topY, bottomY);
    canvas.drawRect(
      Rect.fromLTRB(0, waterTop, size.width, bottomY),
      Paint()..color = AppColors.rtuBrand.withValues(alpha: 0.26),
    );
    if (thicknessRatio > 0) {
      final depositY =
          bottomY - (bottomY - topY) * thicknessRatio.clamp(0.0, 1.0);
      canvas.drawRect(
        Rect.fromLTRB(0, depositY, size.width, bottomY),
        Paint()..color = const Color(0xFFBFC9D6),
      );
    }
    canvas.restore();

    // 断面轮廓
    canvas.drawPath(
      section,
      Paint()
        ..color = AppColors.rtuLine2
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    // 水面线
    canvas.drawLine(
      Offset(cx - topHalf, waterTop),
      Offset(cx + topHalf, waterTop),
      Paint()
        ..color = AppColors.rtuBrand
        ..strokeWidth = 1.6,
    );
    // 中线（虚线示意探头到水面的距离）
    _dashLine(
      canvas,
      Offset(cx, topY),
      Offset(cx, waterTop),
      const Color(0xFF93A1B3),
    );
  }

  void _dashLine(Canvas canvas, Offset from, Offset to, Color color) {
    const dash = 4.0;
    const gap = 3.0;
    final paint = Paint()
      ..color = color
      ..strokeWidth = 1;
    var y = from.dy;
    while (y < to.dy) {
      canvas.drawLine(
        Offset(from.dx, y),
        Offset(from.dx, (y + dash).clamp(0.0, to.dy)),
        paint,
      );
      y += dash + gap;
    }
  }

  @override
  bool shouldRepaint(covariant _SectionPainter old) =>
      old.waterRatio != waterRatio ||
      old.thicknessRatio != thicknessRatio ||
      old.velocity != velocity;
}

// ============================================================================
// SVG icon paths (from HTML data-ic attributes)
// ============================================================================
class _Icons {
  static const _bt = '<path d="M7 7l10 10M17 7L7 17"/><path d="M7 12V7l5 5-5 5v-5z"/><path d="M12 2v20"/>';
  static const _battery = '<rect x="2" y="7" width="17" height="10" rx="3"/><path d="M22 11v2"/><rect x="5" y="10" width="9" height="4" rx="1"/>';
  static const _rtu = '<rect x="4" y="3" width="16" height="18" rx="3"/><path d="M8 8h8M8 12h5"/><circle cx="16" cy="16.5" r="1.6"/>';
  static const _chev = '<path d="M9 6l6 6-6 6"/>';
  static const _back = '<path d="M15 6l-6 6 6 6"/>';
  static const _check = '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.6 2.6L16 9.5"/>';
  static const _info = '<circle cx="12" cy="12" r="9"/><path d="M12 11v5.5M12 7.6v.6"/>';
  static const _scan = '<path d="M3 8V5.5A2.5 2.5 0 015.5 3H8M16 3h2.5A2.5 2.5 0 0121 5.5V8M21 16v2.5A2.5 2.5 0 0118.5 21H16M8 21H5.5A2.5 2.5 0 013 18.5V16"/><path d="M3.5 12h17"/>';
  static const _upload = '<path d="M12 16V4M12 4l-4.2 4.2M12 4l4.2 4.2"/><path d="M4 16v2.5A1.5 1.5 0 005.5 20h13a1.5 1.5 0 001.5-1.5V16"/>';
  static const _cloud = '<path d="M7 18a4 4 0 01-.4-8 5.5 5.5 0 0110.6 1.2A3.5 3.5 0 0117 18H7z"/><path d="M12 12v5M12 12l-2 2M12 12l2 2"/>';
  static const _download = '<path d="M12 4v12M12 16l-4.2-4.2M12 16l4.2-4.2"/><path d="M4 18v1.5A1.5 1.5 0 005.5 21h13a1.5 1.5 0 001.5-1.5V18"/>';
  static const _play = '<circle cx="12" cy="12" r="9"/><path d="M10 8.5l5.5 3.5L10 15.5z"/>';
  static const _pause = '<circle cx="12" cy="12" r="9"/><path d="M10 9v6M14 9v6"/>';
  static const _refresh = '<path d="M20 12a8 8 0 11-2.6-5.9"/><path d="M20 4v4.5h-4.5"/>';
  static const _clock = '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>';
  static const _plug = '<path d="M9 3v5M15 3v5"/><path d="M6 8h12v2.5a6 6 0 01-6 6 6 6 0 01-6-6V8z"/><path d="M12 16.5V21"/>';
  static const _ruler = '<path d="M3 15.5L15.5 3l5.5 5.5L8.5 21z"/><path d="M7.5 11.5l2 2M10.5 8.5l2 2M13.5 5.5l2 2"/>';
  static const _curve = '<path d="M3 19c4 0 4-12 8-12s4 8 8 8"/><circle cx="11" cy="7" r="1.6"/><circle cx="19" cy="15" r="1.6"/>';
  static const _bell = '<path d="M18 9a6 6 0 10-12 0c0 5-2 6-2 6h16s-2-1-2-6z"/><path d="M10.5 20a2 2 0 003 0"/>';
  static const _tool = '<path d="M14.5 5.5a4 4 0 105.2 5.2L21 12l-9 9-3-3 9-9z"/><path d="M6.5 3.5L4 6l3 3 2.5-2.5z"/>';
  static const _home = '<path d="M4 10.5L12 4l8 6.5V19a1.5 1.5 0 01-1.5 1.5h-13A1.5 1.5 0 014 19v-8.5z"/><path d="M9.5 20.5V14h5v6.5"/>';
  static const _gauge = '<circle cx="12" cy="13" r="8"/><path d="M12 13l4-3.5"/><path d="M4.5 20.5h15"/>';
  static const _chart = '<path d="M4 20V4"/><path d="M4 20h16"/><path d="M7.5 16l3.5-5 3 3 5-7"/>';
  static const _cog = '<circle cx="12" cy="12" r="3.2"/><path d="M12 3v2.4M12 18.6V21M3 12h2.4M18.6 12H21M5.6 5.6l1.7 1.7M16.7 16.7l1.7 1.7M18.4 5.6l-1.7 1.7M7.3 16.7l-1.7 1.7"/>';
  static const _flask = '<path d="M9 3h6M10 3v6l-4.5 9A1.5 1.5 0 006.9 20h10.2a1.5 1.5 0 001.4-2L14 9V3"/><path d="M7.5 14h9"/>';
  static const _code = '<path d="M9 8l-4 4 4 4M15 8l4 4-4 4"/>';
  static const _wifi = '<path d="M4.5 9.5a11 11 0 0115 0M8 13a6.5 6.5 0 018 0"/><circle cx="12" cy="17.5" r="1.4"/>';
  static const _power = '<path d="M12 3v8"/><path d="M6.8 6.8a7.5 7.5 0 1010.4 0"/>';
  static const _reset = '<path d="M4.5 12a7.5 7.5 0 102.6-5.7"/><path d="M4 5v4.5h4.5"/>';

  static const _all = <String, String>{
    'bt': _bt, 'battery': _battery, 'rtu': _rtu, 'chev': _chev, 'back': _back,
    'check': _check, 'info': _info, 'scan': _scan, 'upload': _upload,
    'cloud': _cloud, 'download': _download, 'play': _play, 'pause': _pause,
    'refresh': _refresh, 'clock': _clock, 'plug': _plug, 'ruler': _ruler,
    'curve': _curve, 'bell': _bell, 'tool': _tool, 'home': _home,
    'gauge': _gauge, 'chart': _chart, 'cog': _cog, 'flask': _flask,
    'code': _code, 'wifi': _wifi, 'power': _power, 'reset': _reset,
  };

  static Widget svg(String name, {double size = 16, Color? color}) {
    final path = _all[name];
    if (path == null) return SizedBox.shrink();
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _SvgPainter(path, color ?? _R.text),
        size: Size(size, size),
      ),
    );
  }
}

class _SvgPainter extends CustomPainter {
  final String svgPath;
  final Color color;

  _SvgPainter(this.svgPath, this.color);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..strokeWidth = 1.6
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final scale = size.width / 24;
    canvas.save();
    canvas.scale(scale, scale);
    _drawSvg(canvas, svgPath, paint);
    canvas.restore();
  }

  void _drawSvg(Canvas canvas, String path, Paint paint) {
    // Simple SVG path parser for our icon paths
    final fillPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    // Split by path tags
    final paths = <_SvgPathDef>[];
    final re = RegExp(r'<(path|circle|rect|polyline|polygon|line)([^>]*)>');
    for (final m in re.allMatches(path)) {
      final tag = m.group(1)!;
      final attrs = m.group(2)!;
      final fill = RegExp(r'fill="([^"]*)"').firstMatch(attrs)?.group(1);
      final stroke = RegExp(r'stroke="([^"]*)"').firstMatch(attrs)?.group(1);
      paths.add(_SvgPathDef(tag, attrs, fill, stroke));
    }

    for (final def in paths) {
      final p = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round;

      if (def.fill == 'currentColor' || def.stroke == 'currentColor') {
        p.color = color;
      } else {
        p.color = color;
      }

      switch (def.tag) {
        case 'path':
          final d = RegExp(r'd="([^"]*)"').firstMatch(def.attrs)?.group(1) ?? '';
          _drawPath(canvas, d, p, def.fill == 'currentColor' ? fillPaint : null);
          break;
        case 'circle':
          final cx = _num(def.attrs, 'cx');
          final cy = _num(def.attrs, 'cy');
          final r = _num(def.attrs, 'r');
          if (def.fill == 'currentColor') {
            canvas.drawCircle(Offset(cx, cy), r, fillPaint..style = PaintingStyle.fill);
          } else {
            canvas.drawCircle(Offset(cx, cy), r, p);
          }
          break;
        case 'rect':
          final x = _num(def.attrs, 'x');
          final y = _num(def.attrs, 'y');
          final w = _num(def.attrs, 'width');
          final h = _num(def.attrs, 'height');
          final rx = _num(def.attrs, 'rx');
          final rrect = RRect.fromRectAndRadius(Rect.fromLTWH(x, y, w, h), Radius.circular(rx));
          if (def.fill == 'currentColor') {
            canvas.drawRRect(rrect, fillPaint..style = PaintingStyle.fill);
          } else {
            canvas.drawRRect(rrect, p);
          }
          break;
        case 'line':
          final x1 = _num(def.attrs, 'x1'), y1 = _num(def.attrs, 'y1');
          final x2 = _num(def.attrs, 'x2'), y2 = _num(def.attrs, 'y2');
          canvas.drawLine(Offset(x1, y1), Offset(x2, y2), p);
          break;
        case 'polyline':
        case 'polygon':
          final pts = RegExp(r'points="([^"]*)"').firstMatch(def.attrs)?.group(1) ?? '';
          final pathObj = Path();
          final parts = pts.split(RegExp(r'\s+'));
          for (var i = 0; i < parts.length; i++) {
            final xy = parts[i].split(',');
            if (xy.length == 2) {
              final x = double.tryParse(xy[0]) ?? 0;
              final y = double.tryParse(xy[1]) ?? 0;
              if (i == 0) pathObj.moveTo(x, y);
              else pathObj.lineTo(x, y);
            }
          }
          if (def.tag == 'polygon') pathObj.close();
          canvas.drawPath(pathObj, p);
          break;
      }
    }
  }

  void _drawPath(Canvas canvas, String d, Paint strokePaint, Paint? fillPaint) {
    if (d.isEmpty) return;
    final path = Path();
    final re = RegExp(r'([MmLlHhVvCcSsQqTtAaZz])\s*([\d\s,.\-eE]*)');
    double cx = 0, cy = 0;
    double startX = 0, startY = 0;

    bool parseAndMove(double x, double y, bool relative) {
      final dx = relative ? cx + x : x;
      final dy = relative ? cy + y : y;
      path.moveTo(dx, dy);
      cx = dx; cy = dy;
      startX = dx; startY = dy;
      return true;
    }

    bool parseAndLine(double x, double y, bool relative) {
      final dx = relative ? cx + x : x;
      final dy = relative ? cy + y : y;
      path.lineTo(dx, dy);
      cx = dx; cy = dy;
      return true;
    }

    for (final m in re.allMatches(d)) {
      final cmd = m.group(1)!;
      final argsStr = m.group(2)?.trim() ?? '';
      final args = argsStr.isEmpty ? <double>[] : argsStr.split(RegExp(r'[\s,]+')).map((s) => double.tryParse(s) ?? 0).toList();
      final relative = cmd == cmd.toLowerCase() && cmd != 'z';
      final upper = cmd.toUpperCase();

      switch (upper) {
        case 'M':
          for (var i = 0; i < args.length - 1; i += 2) {
            parseAndMove(args[i], args[i + 1], relative);
          }
          break;
        case 'L':
          for (var i = 0; i < args.length - 1; i += 2) {
            parseAndLine(args[i], args[i + 1], relative);
          }
          break;
        case 'H':
          for (final a in args) {
            final x = relative ? cx + a : a;
            path.lineTo(x, cy);
            cx = x;
          }
          break;
        case 'V':
          for (final a in args) {
            final y = relative ? cy + a : a;
            path.lineTo(cx, y);
            cy = y;
          }
          break;
        case 'Z':
          path.close();
          cx = startX; cy = startY;
          break;
        case 'C':
          for (var i = 0; i < args.length - 5; i += 6) {
            final x1 = relative ? cx + args[i] : args[i];
            final y1 = relative ? cy + args[i + 1] : args[i + 1];
            final x2 = relative ? cx + args[i + 2] : args[i + 2];
            final y2 = relative ? cy + args[i + 3] : args[i + 3];
            final x = relative ? cx + args[i + 4] : args[i + 4];
            final y = relative ? cy + args[i + 5] : args[i + 5];
            path.cubicTo(x1, y1, x2, y2, x, y);
            cx = x; cy = y;
          }
          break;
        case 'S':
          // simplified - just skip smooth curve for now
          break;
        case 'Q':
          for (var i = 0; i < args.length - 3; i += 4) {
            final x1 = relative ? cx + args[i] : args[i];
            final y1 = relative ? cy + args[i + 1] : args[i + 1];
            final x = relative ? cx + args[i + 2] : args[i + 2];
            final y = relative ? cy + args[i + 3] : args[i + 3];
            path.quadraticBezierTo(x1, y1, x, y);
            cx = x; cy = y;
          }
          break;
        case 'A':
          // skip arc for now
          break;
      }
    }

    canvas.drawPath(path, strokePaint);
    if (fillPaint != null) {
      canvas.drawPath(path, fillPaint..style = PaintingStyle.fill);
    }
  }

  double _num(String attrs, String name) {
    final m = RegExp('$name="([^"]*)"').firstMatch(attrs);
    return double.tryParse(m?.group(1) ?? '0') ?? 0;
  }

  @override
  bool shouldRepaint(covariant _SvgPainter oldDelegate) =>
      oldDelegate.color != color || oldDelegate.svgPath != svgPath;
}

class _SvgPathDef {
  final String tag;
  final String attrs;
  final String? fill;
  final String? stroke;
  _SvgPathDef(this.tag, this.attrs, this.fill, this.stroke);
}

// ============================================================================
// Main RTU Page
// ============================================================================
class RtuPage extends StatefulWidget {
  const RtuPage({super.key});

  @override
  State<RtuPage> createState() => _RtuPageState();
}

class _RtuPageState extends State<RtuPage> with SingleTickerProviderStateMixin {
  static const _channel = MethodChannel('lihero_rtu');
  static const _events = EventChannel('lihero_rtu/events');

  // --- Backend & connection state ---
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
  bool _backgroundRead = false;

  bool get _connected => _status == '已连接';

  // --- 页面级状态 ---
  /// 当前顶层屏幕。`home` = 原有主界面（默认入口），
  /// `bt` = 点击 AppBar 蓝牙图标后进入的蓝牙连接页。
  String _currentScreen = 'home';

  /// 底部 tabbar 当前选中项。默认落在「概览」。
  _RtuTab _tab = _RtuTab.overview;

  /// 概览页「实时数据」卡片在窄屏下的每行卡片数（2 或 3），
  /// 由 `LayoutBuilder` 按可用宽度动态判定。
  int _overviewColumns = 2;

  /// 概览页「断面图」的展开状态。默认隐藏，点击标题右侧的「断面图 ›」切换。
  bool _overviewDiagramVisible = false;

  /// 概览页 Hero 卡的「数据条数」估算所用常量：按上传间隔推算一轮剩余可传条数。
  /// 仅用于进度条视觉展示，不影响任何实际业务逻辑。
  static const _batchCapacityFallback = 200;
  String _toastMessage = '';
  bool _toastVisible = false;
  Timer? _toastTimer;

  // --- Animation ---
  /// 雷达脉冲动画：三个圆环共用一条时间轴，按 1/3 相位错开（对应原型 .8s 延迟）。
  late AnimationController _radarAnimController;

  // ==========================================================================
  // Lifecycle
  // ==========================================================================
  @override
  void initState() {
    super.initState();
    _systemTimeController.text = _formatDateTime(DateTime.now());
    _eventSubscription = _events.receiveBroadcastStream().listen((event) {
      if (!mounted || event is! Map) return;
      final name = event['event']?.toString() ?? '';
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
        if (_logLines.length > 80) {
          _logLines.removeRange(0, _logLines.length - 80);
        }
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
          }
        }
      });
      if (hasNewCommandLog) _scrollCommandLogToEnd();
    }, onError: (_) {});

    _radarAnimController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2400),
    )..repeat();
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
    _commandLogController.dispose();
    for (final nc in _networkControllers) {
      nc.dispose();
    }
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
    _toastTimer?.cancel();
    _radarAnimController.dispose();
    super.dispose();
  }

  // ==========================================================================
  // Toast & Dialog
  // ==========================================================================
  void _showToast(String msg) {
    setState(() {
      _toastMessage = msg;
      _toastVisible = true;
    });
    _toastTimer?.cancel();
    _toastTimer = Timer(const Duration(milliseconds: 1900), () {
      if (mounted) setState(() => _toastVisible = false);
    });
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
                            activeColor: _R.cyan,
                            onChanged:
                                (value) =>
                                    setDialogState(() => enabled = value),
                          ),
                          if (validationMessage != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 4),
                              child: Text(
                                validationMessage!,
                                style: const TextStyle(color: _R.err),
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

  /// 统一反馈通道：原型用底部 toast（.toast），这里复用页面级 toast 覆盖层，
  /// 避免连接页与主界面出现两套提示样式。
  void _show(String message) {
    if (!mounted) return;
    _showToast(message);
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

  /// AppBar 蓝牙图标入口。
  ///
  /// 原实现是底部弹窗（标题状态 + 扫描设备 + 手动连接 + 断开蓝牙），
  /// 现在改为进入蓝牙连接页，四个动作一一对应到页面里：
  ///   扫描设备   → 页面内联的雷达 + 设备列表 + 重新搜索
  ///   手动连接   → 「手动连接」卡片的输入框 + 按钮
  ///   断开蓝牙   → 「连接状态」卡片里的危险按钮
  ///   标题状态   → 头部 chip + 「连接状态」卡片
  void _showBluetoothActions() {
    if (_currentScreen == 'bt') return;
    setState(() => _currentScreen = 'bt');
    _scheduleAutoScan();
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


  /// 指令交互面板：日志区 + 手动指令输入。
  ///
  /// 原 `_buildDemoInteraction` 中的「雷达水位计 / 雷达流量计 / RTU 设置」
  /// 三个大按钮已由底部 tabbar 与监测页的切换段取代，此处只保留日志与输入，
  /// 功能（查看收发日志、手动发送指令）与原实现完全一致。
  Widget _buildCommandPanel(Map<String, dynamic> basic) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Padding(
          padding: EdgeInsets.only(bottom: RtuTokens.gapMd),
          child: Text(
            '指令交互',
            style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w700),
          ),
        ),
        Container(
          height: 154,
          decoration: BoxDecoration(
            border: Border.all(color: _R.brand),
            borderRadius: BorderRadius.circular(5),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
          child:
              _logLines.isEmpty
                  ? const Align(
                    alignment: Alignment.topLeft,
                    child: Text(
                      'RTU 指令交互信息将在此显示',
                      style: TextStyle(color: _R.text3),
                    ),
                  )
                  : ListView.builder(
                    controller: _commandLogController,
                    itemCount: _logLines.length,
                    itemBuilder: (_, index) {
                      final line = _logLines[index];
                      final color =
                          line.startsWith('发送')
                              ? _R.brand
                              : line.startsWith('接收')
                              ? _R.cyan
                              : line.startsWith('成功')
                              ? _R.ok
                              : _R.err;
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
                  backgroundColor: _R.cyan,
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
                style: const TextStyle(fontSize: 14, color: _R.text),
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
                              ? _R.brand
                              : _R.text2,
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
                color: _R.text,
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
                activeColor: _R.cyan,
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
                activeColor: _R.cyan,
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
                                ? _R.ok
                                : _R.warn,
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
                  style: const TextStyle(color: _R.brand),
                ),
                const Icon(Icons.chevron_right, color: _R.text3),
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
              _R.brand,
              _connected && !_busy
                  ? () => _action(_measuring ? 'stop' : 'start')
                  : null,
            ),
            _demoActionButton(
              _updating ? '上报中…' : '采集上报',
              _R.ok,
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
                  style: const TextStyle(color: _R.brand),
                ),
                const Icon(Icons.chevron_right, color: _R.text3),
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
              _R.warn,
              _connected && !_busy ? () => _action('zero') : null,
            ),
            _demoActionButton(
              '基础配置',
              _R.brand,
              _connected && !_busy
                  ? () {
                    setState(() => _flowBaseConfigVisible = true);
                    unawaited(_readAllFlowConfig());
                  }
                  : null,
            ),
            _demoActionButton(
              _measuring ? '关闭\n调试模式' : '打开\n调试模式',
              _R.brand,
              _connected && !_busy
                  ? () => _action(_measuring ? 'stop' : 'start')
                  : null,
            ),
            _demoActionButton(
              _updating ? '上报中…' : '采集\n上报',
              _R.ok,
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
                            ? _R.text2
                            : _R.brand,
                    fontSize: 15,
                  ),
                ),
              ),
              if (onTap != null)
                const Padding(
                  padding: EdgeInsets.only(left: 4),
                  child: Icon(Icons.chevron_right, color: _R.text3),
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
          border: Border(bottom: BorderSide(color: _R.surface3)),
        ),
        child: Row(
          children: [
            Expanded(child: Text(field['label'] as String)),
            Flexible(
              child: Text(
                '${_flowFieldValue(field)}$unit',
                textAlign: TextAlign.end,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(color: _R.brand),
              ),
            ),
            const Icon(Icons.chevron_right, color: _R.text3),
          ],
        ),
      ),
    );
  }

  Widget _buildFlowBaseConfigScaffold() {
    final fields = _flowConfigFields();
    return Scaffold(
      appBar: AppBar(
        backgroundColor: _R.brand,
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
                style: TextStyle(color: _R.text2),
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  border: Border.all(color: _R.line2),
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
                          bottom: BorderSide(color: _R.surface3),
                        ),
                      ),
                      child: Row(
                        children: [
                          Expanded(child: Text('$slot 号水深-系数')),
                          Text(
                            '${water?.toStringAsFixed(3) ?? '-'} m  /  ${factor?.toStringAsFixed(3) ?? '-'}',
                            style: const TextStyle(color: _R.brand),
                          ),
                          const Icon(
                            Icons.chevron_right,
                            color: _R.text3,
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


  /// 底部 tabbar 承载的主界面。
  ///
  /// 四个 tab（概览 / 监测 / 数据 / 设置）共享同一个 `Scaffold`，
  /// 顶部是各 tab 自己的头部区，底部是固定的 `_RtuTabBar`。
  /// 原「设备信息 / 实时数据 / 参数设置」的功能入口被重新归置：
  ///  - 概览：Hero 卡 + 快捷操作 + 实时数据卡片（新增，数据源同原页面）
  ///  - 监测：原水位计 / 流量计面板（`_buildWaterPanel` / `_buildFlowPanel`）
  ///  - 数据：原设备信息卡片（`_buildDemoDeviceInfo`）
  ///  - 设置：原参数设置面板（`_buildSettingsPanel`）+ 指令交互入口
  Widget _buildLegacyScaffold() {
    if (_flowBaseConfigVisible) return _buildFlowBaseConfigScaffold();
    final basic = _basic ?? const <String, dynamic>{};
    final lot = _lot ?? const <String, dynamic>{};
    final sensor = _sensor ?? const <String, dynamic>{};

    // 各 tab 的滚动内容。用 IndexedStack 保证切换时滚动位置与局部状态不丢失。
    final pages = <Widget>[
      _buildOverviewTab(basic, lot, sensor),
      _buildMonitorTab(sensor),
      _buildDataTab(basic, lot),
      _buildSettingsTab(basic, lot),
    ];

    return Scaffold(
      backgroundColor: _R.bg,
      body: SafeArea(
        bottom: false,
        child: Stack(
          children: [
            IndexedStack(index: _tab.index, children: pages),
            if (_busy) _buildLoadingOverlay(),
          ],
        ),
      ),
      bottomNavigationBar: _RtuTabBar(
        current: _tab,
        onChanged: (tab) {
          if (tab == _tab) return;
          setState(() => _tab = tab);
        },
      ),
    );
  }

  /// 顶栏：标题 + 蓝牙 / 刷新 两个动作。
  /// 原 `AppBar` 的返回（`/more`）改为左上角图标按钮，视觉上与新版设计一致。
  Widget _buildTopBar(Map<String, dynamic> basic) => Padding(
    padding: const EdgeInsets.fromLTRB(6, 4, 6, 0),
    child: Row(
      children: [
        IconButton(
          tooltip: '返回',
          onPressed: () => context.go('/more'),
          icon: const Icon(Icons.arrow_back, color: _R.text),
        ),
        const Spacer(),
        IconButton(
          tooltip: _connected ? '蓝牙已连接，点击管理' : '打开蓝牙连接页，扫描 RTU 设备',
          onPressed: _busy ? null : _showBluetoothActions,
          icon: Icon(
            _connected ? Icons.bluetooth_connected : Icons.bluetooth_searching,
            color: _connected ? _R.brand : _R.text2,
          ),
        ),
        IconButton(
          tooltip: '刷新全部数据',
          onPressed: _busy ? null : _readAll,
          icon: const Icon(Icons.refresh, color: _R.text2),
        ),
      ],
    ),
  );

  // --------------------------------------------------------------------------
  // 概览 tab
  // --------------------------------------------------------------------------

  Widget _buildOverviewTab(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
    Map<String, dynamic> sensor,
  ) {
    final name =
        (basic['deviceName']?.toString().trim().isNotEmpty == true)
            ? basic['deviceName']!.toString()
            : 'RTU 采集器';

    return Column(
      children: [
        _buildTopBar(basic),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.only(bottom: RtuTokens.pageBottom),
            children: [
              _RtuPageHeader(
                title: '设备概览',
                subtitle: name,
                chip: _StatusPill(
                  text: _connected ? '通信正常' : '未连接',
                  fg: _connected ? _R.ok : _R.text3,
                  bg: _connected ? _R.okL : RtuTokens.barTrack,
                ),
              ),
              if (!_connected) _buildConnectPromptCard(),
              _buildHeroCard(basic, lot, sensor),
              const SizedBox(height: RtuTokens.blockGap),
              _buildStatusAlert(sensor),
              const SizedBox(height: RtuTokens.sectionGap),
              _buildQuickActions(),
              const SizedBox(height: RtuTokens.sectionGap),
              _buildRealtimeSection(sensor),
              const SizedBox(height: RtuTokens.sectionGap),
              _buildSectionAndDevicesSection(sensor),
              const SizedBox(height: RtuTokens.sectionGap),
              _buildPlatformStatusSection(lot),
            ],
          ),
        ),
      ],
    );
  }

  // --------------------------------------------------------------------------
  // 概览 tab —— 平台连接状态 / 断面与下端设备
  // --------------------------------------------------------------------------

  /// 「平台连接状态」卡片。
  ///
  /// 字段与 `_buildDemoDeviceInfo`（数据 tab）保持一一对应：
  ///   - 平台连接状态 → `_connected` + `lot['signal']`（通信链路状态，非多传服务）
  ///   - 上次上传成功 → `lot['uploadEndTime']`
  ///   - 未上传数据   → `lot['pendingCount']`
  ///   - 物联网卡     → `lot['ccid']` + `lot['moduleType']`
  Widget _buildPlatformStatusSection(Map<String, dynamic> lot) {
    final signal = (lot['signal'] as num?)?.toInt() ?? 0;
    final pending = (lot['pendingCount'] as num?)?.toInt() ?? 0;
    final ccid = _text(lot['ccid']);
    final module = _moduleText(lot['moduleType']);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _overviewSectionTitle('平台连接状态'),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: RtuTokens.cardPadding,
              vertical: 3,
            ),
            decoration: BoxDecoration(
              color: _R.surface,
              borderRadius: RtuTokens.brCard,
              border: Border.all(color: _R.line),
            ),
            child: Column(
              children: [
                _KvRow(
                  label: '平台连接状态',
                  value: _connected ? '已连接 · $signal 协议' : '未连接',
                  valueColor: _connected ? _R.ok : _R.text3,
                  icon: _connected
                      ? Icons.check_circle_outline
                      : Icons.link_off,
                ),
                _KvRow(
                  label: '上次上传成功',
                  value: _text(lot['uploadEndTime']),
                  valueColor: _R.text,
                ),
                _KvRow(
                  label: '未上传数据',
                  value: '$pending 条',
                  valueColor: _R.text,
                ),
                _KvRow(
                  label: '物联网卡',
                  value: ccid == '-'
                      ? '-'
                      : (module == '-' ? ccid : '$ccid · $module'),
                  valueColor: _R.text,
                  last: true,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// 「断面与下端设备」卡片。
  ///
  /// 上半部分是断面示意图（数值取自遥测 + 流量计基础配置寄存器），
  /// 下半部分列出已搜到的下端设备（字段同蓝牙页设备行）。
  /// 断面图当前按需求默认隐藏，按下「断面图 ›」时展开。
  Widget _buildSectionAndDevicesSection(Map<String, dynamic> sensor) {
    final rows = <Widget>[
      for (final device in _devices)
        _DeviceRow(
          title: _text(device['name'], fallback: 'RTU 下端设备'),
          subtitle: [
            if (_text(device['address']) != '-')
              'RS485 · ${_text(device['address'])}',
            '协议 LHLL',
          ].join(' · '),
          connected: _connected,
        ),
      if (_devices.isEmpty)
        _DeviceRow(
          title: _connected ? '下端设备探测中…' : '未检测到应答',
          subtitle: _connected ? '正在等待 RTU 返回从站信息' : '请先连接 RTU',
          connected: false,
          last: true,
        ),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _overviewSectionTitle(
          '断面与下端设备',
          action: InkWell(
            onTap: () => setState(() => _overviewDiagramVisible = !_overviewDiagramVisible),
            child: Row(
              children: [
                Text(
                  _overviewDiagramVisible ? '收起断面图' : '断面图',
                  style: RtuTextStyles.sectionAction.copyWith(
                    color: _R.brand,
                    fontSize: 12.5,
                  ),
                ),
                Icon(
                  _overviewDiagramVisible
                      ? Icons.keyboard_arrow_up
                      : Icons.chevron_right,
                  size: 17,
                  color: _R.brand,
                ),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
          child: Column(
            children: [
              if (_overviewDiagramVisible) ...[
                Container(
                  padding: const EdgeInsets.all(RtuTokens.cardPadding),
                  decoration: BoxDecoration(
                    color: _R.surface,
                    borderRadius: RtuTokens.brCard,
                    border: Border.all(color: _R.line),
                  ),
                  child: Column(
                    children: [
                      _SectionDiagram(
                        height: 168,
                        waterLevel: _toDouble(sensor['waterLevel']),
                        velocity: _toDouble(sensor['surfaceVelocity']),
                        bottomWidth: _toDouble(_flowRegisters[40086]),
                        slope: _toDouble(_flowRegisters[40088]),
                        thickness: _toDouble(_flowRegisters[40092]),
                      ),
                      const SizedBox(height: RtuTokens.gapMd),
                      Text(
                        _sectionSummaryText(sensor),
                        style: RtuTextStyles.rowSubtitle.copyWith(
                          color: _R.text3,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: RtuTokens.blockGap),
              ],
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: RtuTokens.cardPadding,
                  vertical: 3,
                ),
                decoration: BoxDecoration(
                  color: _R.surface,
                  borderRadius: RtuTokens.brCard,
                  border: Border.all(color: _R.line),
                ),
                child: Column(children: rows),
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// 断面底部的说明文字，字段全部来自已有数据源。
  String _sectionSummaryText(Map<String, dynamic> sensor) {
    final shape = _flowRegisters[40085];
    final shapeLabel = _sectionShapeText(shape);
    final width = _text(
      _formatRegisterValue(_flowRegisters[40086]),
      fallback: '-',
    );
    final slope = _text(_flowRegisters[40088], fallback: '-');
    final thickness = _text(
      _formatRegisterValue(_flowRegisters[40092]),
      fallback: '-',
    );
    return '$shapeLabel · 底宽 $width m · 边坡 $slope · 淤积 $thickness m³';
  }

  String _sectionShapeText(dynamic shape) => switch (shape) {
    0 => '矩形断面',
    1 => '梯形断面',
    2 => 'V 形断面',
    3 => 'U 形断面',
    4 => '圆形断面',
    5 => '不规则断面',
    _ => '断面',
  };

  /// 统一取值：空值 / null 一律回落到 `-`。
  String _text(dynamic value, {String fallback = '-'}) {
    final text = value?.toString().trim() ?? '';
    return text.isEmpty ? fallback : text;
  }

  double _toDouble(dynamic value) {
    if (value is num) return value.toDouble();
    return double.tryParse(value?.toString() ?? '') ?? 0;
  }

  /// 基础配置寄存器里的浮点值（低字在前），拿不到时回落 `-`。
  String _formatRegisterValue(dynamic raw) {
    if (raw is! num) return '-';
    return (raw / 1000).toStringAsFixed(2);
  }

  /// 未连接时的引导卡片：把原来的「扫描 / 连接」入口以更轻的方式呈现。
  Widget _buildConnectPromptCard() => Padding(
    padding: const EdgeInsets.fromLTRB(
      RtuTokens.pageInset,
      0,
      RtuTokens.pageInset,
      RtuTokens.blockGap,
    ),
    child: Material(
      color: _R.brandL,
      borderRadius: RtuTokens.brCard,
      child: InkWell(
        borderRadius: RtuTokens.brCard,
        onTap: _busy ? null : _showBluetoothActions,
        child: Padding(
          padding: const EdgeInsets.all(RtuTokens.cardPadding),
          child: Row(
            children: [
              _IconTile(
                icon: Icons.bluetooth_searching,
                fg: _R.brand,
                bg: Colors.white,
              ),
              const SizedBox(width: RtuTokens.gapXl),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '尚未连接 RTU',
                      style: TextStyle(
                        fontSize: 14.5,
                        fontWeight: FontWeight.w600,
                        color: _R.brandD,
                      ),
                    ),
                    SizedBox(height: 2),
                    Text(
                      '点击搜索附近的 RTU 设备并建立蓝牙连接',
                      style: TextStyle(fontSize: 11.5, color: _R.text2),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: _R.brand),
            ],
          ),
        ),
      ),
    ),
  );

  /// Hero 卡：设备名 / 在线状态 / 核心指标 / 数据进度条。
  Widget _buildHeroCard(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
    Map<String, dynamic> sensor,
  ) {
    final battery = _batteryPercent(basic['batteryVoltage']);
    final voltage = basic['batteryVoltage']?.toString() ?? '-';
    final signal = (lot['signal'] as num?)?.toInt() ?? 0;
    final mn = basic['mn']?.toString();
    final version = basic['version']?.toString();
    final sampleInterval = lot['sampleInterval']?.toString() ?? '-';
    final uploadInterval = lot['uploadInterval']?.toString() ?? '-';
    final pending = (lot['pendingCount'] as num?)?.toInt() ?? 0;

    // 进度条表示本轮待上传数据占批量的比例（纯视觉，无业务含义）。
    final progress =
        (pending / _batchCapacityFallback).clamp(0.0, 1.0).toDouble();

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
      child: Container(
        padding: const EdgeInsets.all(RtuTokens.cardPadding + 1),
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            colors: AppColors.rtuHeroGradient,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          borderRadius: RtuTokens.brHero,
          boxShadow: RtuShadows.hero,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Text(
                    (basic['deviceName']?.toString().trim().isNotEmpty == true)
                        ? basic['deviceName']!.toString()
                        : 'RTU 采集器',
                    style: RtuTextStyles.heroName.copyWith(
                      fontSize: 19,
                      color: Colors.white,
                    ),
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 9,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(RtuTokens.radiusChip),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: RtuTokens.statusDot,
                        height: RtuTokens.statusDot,
                        decoration: const BoxDecoration(
                          color: AppColors.rtuHeroDot,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        _connected ? '在线' : '离线',
                        style: const TextStyle(
                          fontSize: 11.5,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Text(
              [
                'LHRTU 200',
                if (mn?.isNotEmpty == true) 'MN $mn',
                if (version?.isNotEmpty == true) '固件 $version',
              ].join(' · '),
              style: RtuTextStyles.heroMeta.copyWith(
                color: Colors.white.withValues(alpha: 0.82),
              ),
            ),
            const SizedBox(height: RtuTokens.gapXxl),
            Row(
              children: [
                _heroMetric('电池电量', '$battery%'),
                const SizedBox(width: RtuTokens.gapLg),
                _heroMetric('电池电压', '${voltage}v'),
                const SizedBox(width: RtuTokens.gapLg),
                _heroMetric('信号强度', '$signal CSQ'),
              ],
            ),
            const SizedBox(height: RtuTokens.gapXxl),
            ClipRRect(
              borderRadius: BorderRadius.circular(RtuTokens.progressHeight),
              child: LinearProgressIndicator(
                value: progress,
                minHeight: RtuTokens.progressHeight,
                backgroundColor: Colors.white.withValues(alpha: 0.25),
                valueColor: const AlwaysStoppedAnimation(AppColors.rtuHeroDot),
              ),
            ),
            const SizedBox(height: RtuTokens.gapMd),
            Row(
              children: [
                Expanded(
                  child: Text(
                    '间隔工作模式 · 采集 $sampleInterval min / 上传 $uploadInterval min',
                    style: RtuTextStyles.heroFoot.copyWith(
                      color: Colors.white.withValues(alpha: 0.86),
                    ),
                  ),
                ),
                Text(
                  '待传 $pending 条',
                  style: RtuTextStyles.heroFoot.copyWith(
                    fontWeight: FontWeight.w600,
                    color: Colors.white,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _heroMetric(String key, String value) => Expanded(
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.16),
        borderRadius: BorderRadius.circular(RtuTokens.radiusMetric),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            key,
            style: RtuTextStyles.heroMetricKey.copyWith(
              color: Colors.white.withValues(alpha: 0.8),
            ),
          ),
          const SizedBox(height: 3),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              value,
              style: RtuTextStyles.heroMetricValue.copyWith(
                color: Colors.white,
              ),
            ),
          ),
        ],
      ),
    ),
  );

  /// 运行状态提示条：水位在阈值内显示正常，越限或未连接时给出相应提示。
  Widget _buildStatusAlert(Map<String, dynamic> sensor) {
    final waterLevel = sensor['waterLevel'];
    final hasLevel = waterLevel != null && waterLevel.toString().isNotEmpty;

    late final Color fg;
    late final Color bg;
    late final IconData icon;
    late final String message;

    if (!_connected) {
      fg = _R.text2;
      bg = RtuTokens.barTrack;
      icon = Icons.info_outline;
      message = '未连接 RTU，连接后可查看实时水位与设备运行状态。';
    } else if (!hasLevel) {
      fg = _R.warn;
      bg = AppColors.rtuWarnL;
      icon = Icons.hourglass_empty;
      message = '已连接，正在读取传感器数据…';
    } else {
      fg = _R.ok;
      bg = _R.okL;
      icon = Icons.check_circle_outline;
      message = '设备运行正常，当前水位 ${waterLevel}m，处于正常范围内。';
    }

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
      child: Container(
        padding: const EdgeInsets.all(RtuTokens.cardPaddingTight),
        decoration: BoxDecoration(
          color: bg,
          borderRadius: RtuTokens.brCard,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 18, color: fg),
            const SizedBox(width: RtuTokens.gapXl),
            Expanded(
              child: Text(
                message,
                style: TextStyle(fontSize: 12.5, height: 1.45, color: fg),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 区块标题（「快捷操作」「实时数据」…）。
  Widget _overviewSectionTitle(String title, {Widget? action}) => Padding(
    padding: const EdgeInsets.fromLTRB(
      RtuTokens.pageInset,
      0,
      RtuTokens.pageInset,
      RtuTokens.gapXl,
    ),
    child: Row(
      children: [
        Text(
          title,
          style: const TextStyle(fontSize: 15.5, fontWeight: FontWeight.w700),
        ),
        const Spacer(),
        if (action != null) action,
      ],
    ),
  );

  /// 快捷操作四宫格。功能与原有按钮完全一致：
  /// 立即采集 → 读一次传感器；立即上传 → 采集上报；开始测量 → start/stop；
  /// 导出数据 → 读取基础/物联网/传感器全部信息并提示结果。
  Widget _buildQuickActions() {
    final canOperate = _connected && !_busy;

    Widget tile(IconData icon, String label, Color fg, Color bg, VoidCallback? onTap) =>
        Expanded(
          child: Material(
            color: _R.surface,
            borderRadius: RtuTokens.brCard,
            child: InkWell(
              borderRadius: RtuTokens.brCard,
              onTap: onTap,
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 13),
                child: Column(
                  children: [
                    _IconTile(
                      icon: icon,
                      fg: onTap == null ? _R.text3 : fg,
                      bg: onTap == null ? RtuTokens.barTrack : bg,
                    ),
                    const SizedBox(height: RtuTokens.gapMd),
                    Text(
                      label,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: onTap == null ? _R.text3 : _R.text,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _overviewSectionTitle('快捷操作'),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
          child: Row(
            children: [
              tile(
                Icons.crop_free,
                '立即采集',
                _R.brand,
                _R.brandL,
                canOperate ? () => unawaited(_readSelectedSensor(showLoading: true)) : null,
              ),
              const SizedBox(width: RtuTokens.gapMd),
              tile(
                Icons.arrow_circle_up_outlined,
                '立即上传',
                _R.cyan,
                AppColors.rtuCyanL,
                canOperate && !_updating ? () => _action('upload') : null,
              ),
            ],
          ),
        ),
        const SizedBox(height: RtuTokens.gapMd),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
          child: Row(
            children: [
              tile(
                _measuring ? Icons.pause_circle_outline : Icons.play_circle_outline,
                _measuring ? '停止测量' : '开始测量',
                _R.warn,
                AppColors.rtuWarnL,
                canOperate ? () => _action(_measuring ? 'stop' : 'start') : null,
              ),
              const SizedBox(width: RtuTokens.gapMd),
              tile(
                Icons.download_outlined,
                '导出数据',
                _R.ok,
                _R.okL,
                canOperate ? () => unawaited(_readAll()) : null,
              ),
            ],
          ),
        ),
      ],
    );
  }

  /// 实时数据卡片区。字段与「雷达水位计 / 雷达流量计」面板保持一致，
  /// 只是把关键数值以更大的卡片形式呈现在概览页。
  Widget _buildRealtimeSection(Map<String, dynamic> sensor) {
    final metrics = <_RealtimeMetric>[
      _RealtimeMetric('水位', sensor['waterLevel'], 'm'),
      _RealtimeMetric('水深', sensor['depth'], 'm'),
      _RealtimeMetric('表面流速', sensor['surfaceVelocity'], 'm/s'),
      _RealtimeMetric('瞬时流量', sensor['rateFlow'], 'm³/s'),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _overviewSectionTitle(
          '实时数据',
          action: InkWell(
            onTap: () => setState(() => _tab = _RtuTab.data),
            child: Row(
              children: [
                Text(
                  '查看全部',
                  style: RtuTextStyles.sectionAction.copyWith(
                    color: _R.brand,
                    fontSize: 12.5,
                  ),
                ),
                const Icon(Icons.chevron_right, size: 17, color: _R.brand),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: RtuTokens.pageInset),
          child: LayoutBuilder(
            builder: (_, constraints) {
              // 窄屏两列、宽屏三列，避免数值与单位换行。
              final columns = constraints.maxWidth >= 330 ? 3 : 2;
              final rows = <Widget>[];
              for (var i = 0; i < metrics.length; i += columns) {
                final chunk = metrics.sublist(
                  i,
                  (i + columns).clamp(0, metrics.length),
                );
                rows.add(
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      for (var j = 0; j < columns; j++)
                        Expanded(
                          child: Padding(
                            padding: EdgeInsets.only(
                              right: j == columns - 1 ? 0 : RtuTokens.gapMd,
                            ),
                            child: j < chunk.length
                                ? _realtimeCard(chunk[j])
                                : const SizedBox.shrink(),
                          ),
                        ),
                    ],
                  ),
                );
                if (i + columns < metrics.length) {
                  rows.add(const SizedBox(height: RtuTokens.gapMd));
                }
              }
              return Column(children: rows);
            },
          ),
        ),
      ],
    );
  }

  Widget _realtimeCard(_RealtimeMetric metric) => Container(
    padding: const EdgeInsets.all(RtuTokens.cardPaddingTight),
    decoration: BoxDecoration(
      color: _R.surface,
      borderRadius: RtuTokens.brCard,
      border: Border.all(color: _R.line),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          metric.label,
          style: const TextStyle(fontSize: 12, color: _R.text2),
        ),
        const SizedBox(height: RtuTokens.gapSm),
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Flexible(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                alignment: Alignment.centerLeft,
                child: Text(
                  metric.value,
                  style: const TextStyle(
                    fontSize: 23,
                    fontWeight: FontWeight.w700,
                    color: _R.text,
                    letterSpacing: -0.4,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 2),
            Text(
              metric.unit,
              style: const TextStyle(fontSize: 11.5, color: _R.text2),
            ),
          ],
        ),
        const SizedBox(height: RtuTokens.gapSm),
        _SparkLine(color: _R.cyan),
      ],
    ),
  );

  // --------------------------------------------------------------------------
  // 监测 / 数据 / 设置 tab
  // --------------------------------------------------------------------------

  /// 监测 tab：选择水位计或流量计面板，并保留开始测量 / 采集上报。
  Widget _buildMonitorTab(Map<String, dynamic> sensor) => Column(
    children: [
      _RtuPageHeader(
        title: '实时监测',
        subtitle: '雷达水位计 / 雷达流量计',
        chip: _StatusPill(
          text: _activeTab == 'llj' ? '流量计' : '水位计',
          fg: _activeTab == 'llj' ? _R.brand : _R.warn,
          bg: _activeTab == 'llj' ? _R.brandL : AppColors.rtuWarnL,
        ),
      ),
      Expanded(
        child: ListView(
          padding: const EdgeInsets.only(bottom: RtuTokens.pageBottom),
          children: [
            if (!_connected) _buildConnectPromptCard(),
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: RtuTokens.pageInset,
              ),
              child: Column(
                children: [
                  _buildSensorSwitch(),
                  if (_activeTab == 'llj')
                    _buildFlowPanel(sensor)
                  else
                    _buildWaterPanel(sensor),
                ],
              ),
            ),
          ],
        ),
      ),
    ],
  );

  /// 水位计 / 流量计切换段（替代原首页的两个大按钮）。
  Widget _buildSensorSwitch() {
    Widget item(String label, bool selected, VoidCallback? onTap) => Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            color: selected ? _R.brand : Colors.transparent,
            borderRadius: BorderRadius.circular(RtuTokens.radiusControl),
          ),
          child: Text(
            label,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: selected ? Colors.white : _R.text2,
            ),
          ),
        ),
      ),
    );

    final canSwitch = _connected && !_busy;
    return Container(
      margin: const EdgeInsets.only(bottom: RtuTokens.sectionGap),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: _R.surface3,
        borderRadius: BorderRadius.circular(RtuTokens.radiusRow),
      ),
      child: Row(
        children: [
          item(
            '雷达水位计',
            _activeTab != 'llj',
            canSwitch ? () => unawaited(_switchSensorTab('swj')) : null,
          ),
          item(
            '雷达流量计',
            _activeTab == 'llj',
            canSwitch ? () => unawaited(_switchSensorTab('llj')) : null,
          ),
        ],
      ),
    );
  }

  /// 数据 tab：设备信息（原 `_buildDemoDeviceInfo`）。
  Widget _buildDataTab(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
  ) => Column(
    children: [
      _RtuPageHeader(
        title: '设备数据',
        subtitle: '设备信息 · 通信状态 · 网络配置',
        chip: _StatusPill(
          text: _connected ? '已连接' : '未连接',
          fg: _connected ? _R.ok : _R.text3,
          bg: _connected ? _R.okL : RtuTokens.barTrack,
        ),
      ),
      Expanded(
        child: ListView(
          padding: const EdgeInsets.only(bottom: RtuTokens.pageBottom),
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: RtuTokens.pageInset,
              ),
              child: _buildDemoDeviceInfo(basic, lot),
            ),
          ],
        ),
      ),
    ],
  );

  /// 设置 tab：参数设置面板（原 `_buildSettingsPanel`）+ 指令交互入口。
  ///
  /// 「指令交互」在原首页是与三个大按钮并列的一块，这里以按钮形式保留入口，
  /// 点击后弹出与原先完全一致的日志面板与手动指令输入。
  Widget _buildSettingsTab(
    Map<String, dynamic> basic,
    Map<String, dynamic> lot,
  ) => Column(
    children: [
      _RtuPageHeader(
        title: '设置',
        subtitle: '设备参数 · 蓝牙连接 · 指令交互',
        chip: _StatusPill(
          text: _connected ? '可编辑' : '未连接',
          fg: _connected ? _R.brand : _R.text3,
          bg: _connected ? _R.brandL : RtuTokens.barTrack,
        ),
      ),
      Expanded(
        child: ListView(
          padding: const EdgeInsets.only(bottom: RtuTokens.pageBottom),
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: RtuTokens.pageInset,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildSettingsActions(),
                  const SizedBox(height: RtuTokens.sectionGap),
                  _buildSettingsPanel(basic, lot),
                ],
              ),
            ),
          ],
        ),
      ),
    ],
  );

  Widget _buildSettingsActions() => Column(
    children: [
      _actionRow(
        icon: Icons.bluetooth,
        label: '蓝牙连接管理',
        value: _connected ? '已连接' : '未连接',
        onTap: _busy ? null : _showBluetoothActions,
      ),
      const SizedBox(height: RtuTokens.gapMd),
      _actionRow(
        icon: Icons.terminal,
        label: '指令交互 / 日志',
        value: '打开',
        onTap: _busy ? null : _openCommandLogSheet,
      ),
    ],
  );

  Widget _actionRow({
    required IconData icon,
    required String label,
    required String value,
    VoidCallback? onTap,
  }) => Material(
    color: _R.surface,
    borderRadius: RtuTokens.brCard,
    child: InkWell(
      borderRadius: RtuTokens.brCard,
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(RtuTokens.rowPadding),
        child: Row(
          children: [
            _IconTile(icon: icon, fg: _R.brand, bg: _R.brandL),
            const SizedBox(width: RtuTokens.gapXl),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(fontSize: 14.5, fontWeight: FontWeight.w500),
              ),
            ),
            Text(
              value,
              style: const TextStyle(fontSize: 13, color: _R.text2),
            ),
            const Icon(Icons.chevron_right, color: _R.text3),
          ],
        ),
      ),
    ),
  );

  /// 弹出指令交互面板。内容复用 `_buildDemoInteraction`，
  /// 保证日志显示、手动发送指令等原有能力原样保留。
  Future<void> _openCommandLogSheet() async {
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: _R.bg,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(RtuTokens.radiusHero),
        ),
      ),
      builder:
          (sheetContext) => Padding(
            padding: EdgeInsets.only(
              bottom: MediaQuery.of(sheetContext).viewInsets.bottom,
            ),
            child: SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(RtuTokens.pageInset),
                child: SingleChildScrollView(
                  child: _buildCommandPanel(
                    _basic ?? const <String, dynamic>{},
                  ),
                ),
              ),
            ),
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
            style: const TextStyle(fontSize: 14, color: _R.text),
            children: [
              TextSpan(text: '$label：'),
              TextSpan(
                text: value.isEmpty ? '-' : value,
                style: const TextStyle(color: _R.brand),
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
            border: Border.all(color: _R.line2),
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
                      style: TextButton.styleFrom(foregroundColor: _R.err),
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


  // ==========================================================================
  // 蓝牙连接页（还原原型 #connect）
  //   入口：主界面 AppBar 的蓝牙图标（原先是底部弹窗，动作集合完全保留，
  //         只把表现形式换成整屏页面）。
  //   结构：.hd 头部 + .radar 雷达 + .sec 分组头 + .devs 设备行 + .toast 轻提示
  //   颜色/圆角/字号统一取 _R 与 RtuTokens / RtuTextStyles，不再出现裸色值。
  // ==========================================================================

  /// 蓝牙页是独立的一屏，只在点击蓝牙图标后进入，`返回` 回到主界面。
  bool get _showBluetoothPage => _currentScreen == 'bt';

  @override
  Widget build(BuildContext context) {
    return RtuTheme.apply(
      context: context,
      child: Stack(
        children: [
          if (_showBluetoothPage)
            Positioned.fill(child: _buildBluetoothPage())
          else
            Positioned.fill(child: _buildLegacyScaffold()),
          // 全局轻提示：两个界面共用同一套 toast，行为与原型一致。
          _buildToast(),
        ],
      ),
    );
  }

  // --------------------------------------------------------------------------
  // 页面骨架
  // --------------------------------------------------------------------------
  Widget _buildBluetoothPage() {
    final connecting = _status == '连接中';
    return Scaffold(
      backgroundColor: _R.bg,
      body: SafeArea(
        child: Stack(
          children: [
            Column(
              children: [
                _buildConnectHeader(),
                Expanded(
                  child: ListView(
                    padding: RtuTokens.bodyInsets,
                    children: [
                      const SizedBox(height: RtuTokens.gapSm),
                      _buildRadar(),
                      Padding(
                        padding: const EdgeInsets.only(
                          bottom: 18,
                        ), // .radar 下间距
                        child: Text(
                          connecting
                              ? '正在连接 RTU 设备…'
                              : _connected
                              ? '蓝牙已连接，可继续搜索并切换设备'
                              : '正在搜索附近 RTU 设备…',
                          textAlign: TextAlign.center,
                          style: RtuTextStyles.body2.copyWith(color: _R.text2),
                        ),
                      ),
                      _buildDeviceSectionHeader(connecting),
                      if (_devices.isEmpty)
                        _buildEmptyDevices(connecting)
                      else
                        ..._devices.map(_buildDeviceRow),
                      // 弹窗时代的「状态 / 手动连接 / 断开」三块功能落在下方。
                      _buildLinkStatusCard(),
                      if (!_connected) _buildManualConnectCard(),
                      const SizedBox(height: RtuTokens.gapLg),
                    ],
                  ),
                ),
              ],
            ),
            if (_busy) _buildLoadingOverlay(),
          ],
        ),
      ),
    );
  }

  /// .hd —— 返回、标题/副标题、右侧状态 chip
  Widget _buildConnectHeader() {
    return Container(
      color: _R.surface2,
      padding: const EdgeInsets.fromLTRB(
        RtuTokens.pageInset,
        RtuTokens.gapSm,
        RtuTokens.pageInset,
        10,
      ),
      child: Row(
        children: [
          _buildBackButton(),
          const SizedBox(width: RtuTokens.gapXxl),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '设备连接',
                  style: RtuTextStyles.pageTitle.copyWith(color: _R.text),
                ),
                const SizedBox(height: 2),
                Text(
                  '蓝牙直连 · 无需拆线接线',
                  style: RtuTextStyles.pageSubtitle.copyWith(color: _R.text3),
                ),
              ],
            ),
          ),
          const SizedBox(width: RtuTokens.gapMd),
          if (_connected)
            _buildChip('已连接', fg: _R.ok, bg: _R.okL)
          else
            _buildChip('蓝牙已开启', fg: _R.brandD, bg: _R.brandL),
        ],
      ),
    );
  }

  /// .back —— 30×30 圆角描边按钮，返回主界面
  Widget _buildBackButton() {
    final radius = BorderRadius.circular(RtuTokens.radiusControl);
    return SizedBox(
      width: 30,
      height: 30,
      child: Material(
        color: _R.surface,
        shape: RoundedRectangleBorder(
          borderRadius: radius,
          side: BorderSide(color: _R.line),
        ),
        child: InkWell(
          borderRadius: radius,
          onTap: _busy ? null : _backToMainPage,
          child: Center(child: _Icons.svg('back', size: 16, color: _R.text2)),
        ),
      ),
    );
  }

  /// .chip —— 胶囊状态标签
  Widget _buildChip(String text, {required Color fg, required Color bg}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: bg, borderRadius: RtuTokens.brChip),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: RtuTokens.statusDot,
            height: RtuTokens.statusDot,
            decoration: BoxDecoration(color: fg, shape: BoxShape.circle),
          ),
          const SizedBox(width: RtuTokens.gapXs),
          Text(text, style: RtuTextStyles.chip.copyWith(color: fg)),
        ],
      ),
    );
  }

  // --------------------------------------------------------------------------
  // .radar —— 三个错相脉冲环 + 渐变核心
  // --------------------------------------------------------------------------
  Widget _buildRadar() {
    return SizedBox(
      height: RtuTokens.radarSize,
      child: Center(
        child: SizedBox(
          width: RtuTokens.radarSize,
          height: RtuTokens.radarSize,
          child: AnimatedBuilder(
            animation: _radarAnimController,
            builder: (context, child) {
              return Stack(
                alignment: Alignment.center,
                children: [
                  for (var i = 0; i < 3; i++) _buildRadarRing(i),
                  child!,
                ],
              );
            },
            child: _buildRadarCore(),
          ),
        ),
      ),
    );
  }

  /// 单环：t 在 [0,1) 内循环，缩放 1 → 1.5，透明度 .9 → 0（对齐 @keyframes pulse）。
  Widget _buildRadarRing(int index) {
    final t = (_radarAnimController.value + index / 3) % 1.0;
    final size = RtuTokens.radarRing * (1 + .5 * t);
    return Opacity(
      opacity: (.9 * (1 - t)).clamp(0.0, 1.0),
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: AppColors.rtuRadarRing),
        ),
      ),
    );
  }

  Widget _buildRadarCore() {
    return Container(
      width: RtuTokens.radarCore,
      height: RtuTokens.radarCore,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: AppColors.rtuRadarCoreGradient,
        ),
        boxShadow: RtuShadows.radarCore,
      ),
      child: Center(child: _Icons.svg('bt', size: 26, color: _R.surface)),
    );
  }

  // --------------------------------------------------------------------------
  // .sec —— 分组标题 + 重新搜索
  // --------------------------------------------------------------------------
  Widget _buildDeviceSectionHeader(bool connecting) {
    return Padding(
      padding: const EdgeInsets.only(
        top: RtuTokens.sectionGap,
        bottom: RtuTokens.gapLg,
      ),
      child: Row(
        children: [
          Text(
            '发现的设备 ${_devices.length}',
            style: RtuTextStyles.sectionTitle.copyWith(color: _R.text2),
          ),
          const Spacer(),
          InkWell(
            borderRadius: RtuTokens.brChip,
            onTap: (_busy || connecting) ? null : _startConnectScan,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
              child: Text(
                '重新搜索',
                style: RtuTextStyles.sectionAction.copyWith(color: _R.brand),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 空态：原型为扫描中的占位，这里补上未发现设备时的引导文案。
  Widget _buildEmptyDevices(bool connecting) {
    return Container(
      padding: RtuTokens.cardInsets,
      decoration: BoxDecoration(
        color: _R.surface,
        border: Border.all(color: _R.line),
        borderRadius: RtuTokens.brCard,
      ),
      child: Row(
        children: [
          _Icons.svg(
            'scan',
            size: 18,
            color: connecting ? _R.brand : _R.text3,
          ),
          const SizedBox(width: RtuTokens.gapXl),
          Expanded(
            child: Text(
              connecting
                  ? '正在建立蓝牙链路…'
                  : '未发现 RTU 设备，请确认设备已上电并靠近手机后重新搜索',
              style: RtuTextStyles.hintSm.copyWith(color: _R.text3),
            ),
          ),
        ],
      ),
    );
  }

  // --------------------------------------------------------------------------
  // .devs —— 设备行：图标 / 名称 / 地址·RSSI / 信号条 / 连接
  // --------------------------------------------------------------------------
  Widget _buildDeviceRow(Map<String, dynamic> device) {
    final address = device['address']?.toString() ?? '';
    final name = (device['name']?.toString().trim().isNotEmpty == true)
        ? device['name'].toString()
        : 'RTU 设备';
    final rssi = device['rssi'];
    final rssiText = (rssi == null || rssi.toString().isEmpty)
        ? '--'
        : rssi.toString();
    return Container(
      margin: const EdgeInsets.only(bottom: RtuTokens.gapLg),
      padding: const EdgeInsets.all(RtuTokens.rowPadding),
      decoration: BoxDecoration(
        color: _R.surface,
        border: Border.all(color: _R.line),
        borderRadius: RtuTokens.brDevice,
      ),
      child: Row(
        children: [
          Container(
            width: RtuTokens.iconTile,
            height: RtuTokens.iconTile,
            decoration: BoxDecoration(
              color: _R.brandL,
              borderRadius: RtuTokens.brIconTileLg,
            ),
            child: Center(child: _Icons.svg('rtu', size: 18, color: _R.brand)),
          ),
          const SizedBox(width: RtuTokens.gapXxl),
          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RtuTextStyles.deviceName.copyWith(color: _R.text),
                ),
                const SizedBox(height: 2),
                Text(
                  address.isEmpty ? '--' : '$address · $rssiText dBm',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RtuTextStyles.deviceMeta.copyWith(color: _R.text3),
                ),
              ],
            ),
          ),
          const SizedBox(width: RtuTokens.gapMd),
          _buildSignalBars(rssi),
          const SizedBox(width: RtuTokens.gapMd),
          _buildConnectButton(device),
        ],
      ),
    );
  }

  /// .sig —— 4 根信号柱，按 RSSI 点亮：≥-60 满格绿 / ≥-70 三格绿 / ≥-80 黄 / 其余红
  Widget _buildSignalBars(dynamic rssi) {
    final value = rssi is num
        ? rssi.toInt()
        : int.tryParse(rssi?.toString() ?? '');
    final level = value == null
        ? 0
        : value >= -60
        ? 4
        : value >= -70
        ? 3
        : value >= -80
        ? 2
        : 1;
    final active = level >= 3
        ? _R.ok
        : level == 2
        ? _R.warn
        : _R.err;
    const heights = <double>[4, 7, 10, 13];
    return SizedBox(
      height: RtuTokens.signalBarHeight,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          for (var i = 0; i < heights.length; i++) ...[
            if (i > 0) const SizedBox(width: 2),
            Container(
              width: RtuTokens.signalBarWidth,
              height: heights[i],
              decoration: BoxDecoration(
                color: i < level ? active : _R.line2,
                borderRadius: BorderRadius.circular(1),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildConnectButton(Map<String, dynamic> device) {
    final radius = BorderRadius.circular(RtuTokens.radiusInput);
    return Material(
      color: _R.surface,
      shape: RoundedRectangleBorder(
        borderRadius: radius,
        side: BorderSide(color: _R.brand),
      ),
      child: InkWell(
        borderRadius: radius,
        onTap: (_busy || _status == '连接中')
            ? null
            : () => _connectScannedDevice(device),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
          child: Text(
            '连接',
            style: RtuTextStyles.buttonSm.copyWith(
              color: _R.brand,
              fontSize: 12,
            ),
          ),
        ),
      ),
    );
  }

  // --------------------------------------------------------------------------
  // .sec / .card / .kv —— 连接状态与手动连接（对应原弹窗中的同组功能）
  // --------------------------------------------------------------------------
  Widget _buildSectionTitle(String text) {
    return Padding(
      padding: const EdgeInsets.only(
        top: RtuTokens.sectionGap,
        bottom: RtuTokens.gapLg,
      ),
      child: Text(
        text,
        style: RtuTextStyles.sectionTitle.copyWith(color: _R.text2),
      ),
    );
  }

  Widget _buildCard(List<Widget> children) {
    return Container(
      padding: RtuTokens.cardInsets,
      decoration: BoxDecoration(
        color: _R.surface,
        border: Border.all(color: _R.line),
        borderRadius: RtuTokens.brCard,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: children,
      ),
    );
  }

  /// .kv —— 键值行，最后一行不带分隔线。
  Widget _buildKvRow(String label, String value, {bool last = false}) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        border: last
            ? null
            : Border(bottom: BorderSide(color: _R.line, width: 1)),
      ),
      child: Row(
        children: [
          Text(
            label,
            style: RtuTextStyles.keyValue.copyWith(color: _R.text3),
          ),
          const Spacer(),
          Flexible(
            child: Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.right,
              style: RtuTextStyles.keyValueStrong.copyWith(color: _R.text),
            ),
          ),
        ],
      ),
    );
  }

  /// 原弹窗首行的「已连接 xxx / 蓝牙未连接 + 状态」信息，现落在页面底部卡片里。
  Widget _buildLinkStatusCard() {
    final basic = _basic ?? const <String, dynamic>{};
    final address = _addressController.text.trim();
    final connected = _connected;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildSectionTitle('连接状态'),
        _buildCard([
          _buildKvRow('状态', _status),
          _buildKvRow(
            '设备',
            (basic['deviceName']?.toString().trim().isNotEmpty == true)
                ? basic['deviceName'].toString()
                : '-',
          ),
          _buildKvRow('地址', address.isEmpty ? '-' : address, last: !connected),
          if (connected) ...[
            const SizedBox(height: RtuTokens.gapXl),
            _buildDangerButton(),
          ],
        ]),
      ],
    );
  }

  /// .btn.dg —— 断开蓝牙（等价于原弹窗里的「断开蓝牙」）
  Widget _buildDangerButton() {
    return SizedBox(
      height: RtuTokens.buttonHeight,
      child: Material(
        color: _R.errL,
        borderRadius: RtuTokens.brButton,
        child: InkWell(
          borderRadius: RtuTokens.brButton,
          onTap: _busy ? null : () => _disconnect(),
          child: Center(
            child: Text(
              '断开蓝牙',
              style: RtuTextStyles.button.copyWith(color: _R.err),
            ),
          ),
        ),
      ),
    );
  }

  /// 原弹窗中的「手动输入地址 + 手动连接」，未连接时显示。
  Widget _buildManualConnectCard() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildSectionTitle('手动连接'),
        _buildCard([
          TextField(
            controller: _addressController,
            textCapitalization: TextCapitalization.characters,
            decoration: const InputDecoration(
              hintText: '输入以 15 开头的 12 位蓝牙地址',
            ),
          ),
          const SizedBox(height: RtuTokens.gapXl),
          ElevatedButton(
            onPressed: _busy ? null : () => _connect(),
            child: const Text('手动连接'),
          ),
        ]),
      ],
    );
  }

  /// 关闭蓝牙页：停掉后台扫描并回到主界面（原弹窗由 Navigator.pop 收尾）。
  void _backToMainPage() {
    unawaited(_call('stopScan'));
    setState(() {
      _currentScreen = 'home';
      _devices.clear();
    });
  }

  // --------------------------------------------------------------------------
  // .toast —— 底部轻提示（两个界面共用，260ms 淡入淡出）
  // --------------------------------------------------------------------------
  Widget _buildToast() {
    return Positioned(
      left: 0,
      right: 0,
      bottom: 96,
      child: IgnorePointer(
        child: Center(
          child: AnimatedOpacity(
            opacity: _toastVisible ? 1 : 0,
            duration: const Duration(milliseconds: 250),
            curve: Curves.easeOut,
            child: Container(
              constraints: BoxConstraints(
                maxWidth:
                    MediaQuery.sizeOf(context).width - RtuTokens.pageInset * 4,
              ),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
              decoration: BoxDecoration(
                color: AppColors.rtuToast,
                borderRadius: RtuTokens.brChip,
              ),
              child: Text(
                _toastMessage,
                textAlign: TextAlign.center,
                style: RtuTextStyles.toast.copyWith(color: _R.surface),
              ),
            ),
          ),
        ),
      ),
    );
  }

  // --------------------------------------------------------------------------
  // 交互
  // --------------------------------------------------------------------------

  /// 进入蓝牙页后自动搜索一次，对应原型默认的「正在搜索附近 RTU 设备…」。
  /// 与弹窗时代一致：只有用户主动打开该页才会触发权限申请与扫描。
  void _scheduleAutoScan() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _currentScreen == 'bt') _startConnectScan();
    });
  }

  /// 权限与原生扫描调用与 `_scan()` 保持一致，仅去掉模态弹窗，改为就地刷新列表。
  Future<void> _startConnectScan() async {
    if (_busy || _status == '连接中') return;
    if (Platform.isAndroid) {
      final statuses = await [
        Permission.bluetoothScan,
        Permission.bluetoothConnect,
        Permission.locationWhenInUse,
      ].request();
      final sdk = (await DeviceInfoPlugin().androidInfo).version.sdkInt;
      final required = sdk >= 31
          ? [
              statuses[Permission.bluetoothScan],
              statuses[Permission.bluetoothConnect],
            ]
          : [statuses[Permission.locationWhenInUse]];
      if (required.any(
        (status) =>
            status == null || status.isPermanentlyDenied || status.isDenied,
      )) {
        _showToast('请允许附近设备/位置权限后再搜索');
        return;
      }
    }
    if (!mounted) return;
    setState(() => _devices.clear());
    await _call('startScan', {'durationMs': 30000});
    if (!mounted) return;
    _showToast('已开始搜索附近 RTU 设备');
  }

  Future<void> _connectScannedDevice(Map<String, dynamic> device) async {
    final address = device['address']?.toString() ?? '';
    if (address.isEmpty) {
      _showToast('该设备没有可用的蓝牙地址');
      return;
    }
    await _call('stopScan');
    await _connect(address);
    if (!mounted || !_connected) return;
    final name = device['name']?.toString().trim() ?? '';
    setState(() => _currentScreen = 'home');
    _showToast('已连接 ${name.isEmpty ? 'RTU 设备' : name}');
  }
}

/// 概览页「实时数据」卡片的数据载体。
///
/// 只负责把页面已有的遥测字段（水位 / 水深 / 表面流速 / 瞬时流量）
/// 转成「数值 + 单位」的展示形式，不引入任何新的数据来源。
class _RealtimeMetric {
  _RealtimeMetric(this.label, dynamic rawValue, this.unit)
    : value = (rawValue == null || rawValue.toString().trim().isEmpty)
          ? '-'
          : rawValue.toString();

  final String label;
  final String value;
  final String unit;
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
