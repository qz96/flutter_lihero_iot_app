import 'package:flutter/material.dart';

class AppColors {
  AppColors._();
  
  // Primary Brand Colors (CE Blue Theme)
  static const primaryBlue = Color(0xFF305680);
  static const secondaryBlue = Color(0xFF527dad);
  static const darkPrimaryBlue = Color(0xFF9fa8da);
  
  // Surface Colors
  static const white = Color(0xFFFFFFFF);
  static const scaffoldBackground = Color(0xFFFAFAFA);
  static const surfaceLight = Color(0xFFF4F6F8);
  static const black = Color(0xFF000000);
  
  // Text Colors
  static const textPrimary = Color(0xDE000000);
  static const textSecondary = Color(0xC2000000);
  static const textTertiary = Color(0x8A000000);
  static const textDisabled = Color(0x61000000);
  static const textWhite = Color(0xFFFFFFFF);
  static const textDark = Color(0xFF282828);
  
  // Icon Colors
  static const iconSecondary = Color(0xC2000000);
  static const iconTertiary = Color(0x8a000000);
  static const iconDisabled = Color(0x61000000);
  
  // Border Colors
  static const bordersLight = Color(0x1F000000);
  static const borderError = Color(0xFFD12730);
  
  // State Colors
  static const textError = Color(0xFFD12730);
  static const notificationInfo = Color(0xFF323232);
  static const notificationWarning = Color(0xFFdc6d1b);
  static const notificationSuccess = Color(0xFF008000);
  static const notificationError = Color(0xFFD12730);
  
  // Overlay Colors
  static const bgOverlay = Color(0x61000000);
  static const cameraBackground = Color(0xFF828282);
  
  // Selection and Focus Colors
  static const selectionColor = Color(0x33305680); // primaryBlue with 20% opacity
  static const focusColor = primaryBlue;
  
  // Material Color Swatch for Primary Blue
  static const MaterialColor primarySwatch = MaterialColor(0xFF305680, <int, Color>{
    50: Color(0xFFE8EAF6),
    100: Color(0xFFC5CAE9),
    200: Color(0xFF9FA8DA),
    300: Color(0xFF7986CB),
    400: Color(0xFF5C6BC0),
    500: primaryBlue,
    600: secondaryBlue,
    700: Color(0xFF303F9F),
    800: Color(0xFF283593),
    900: Color(0xFF1A237E),
  });
  
  // Dark Theme Material Color Swatch
  static const MaterialColor darkPrimarySwatch = MaterialColor(0xFF305680, <int, Color>{
    50: Color(0xFFE8EAF6),
    100: Color(0xFFC5CAE9),
    200: Color(0xFF9FA8DA),
    300: Color(0xFF7986CB),
    400: Color(0xFF5C6BC0),
    500: darkPrimaryBlue,
    600: secondaryBlue,
    700: Color(0xFF303F9F),
    800: primaryBlue,
    900: Color(0xFF1A237E),
  });
  
  // Getters for dynamic color access
  static Color get appPrimaryColor => primaryBlue;

  // ==========================================================================
  // RTU palette — RTU 模块专用
  //
  // 取值与 RTU 交互原型的 CSS 自定义属性 1:1 对应（例如 --brand:#0A6FD8）。
  // 这部分色值与上面的全局 token 有意保持不同：
  //  - 上面的 token 驱动项目其余所有页面，禁止改动；
  //  - 下面的 token 只允许被 RTU 模块使用，
  //    并通过 lib/config/themes/rtu_theme.dart 以局部 Theme 注入，
  //    不会影响其它页面的任何像素。
  // 对应的间距 / 圆角 / 阴影 / 字号见 RtuTokens、RtuShadows、RtuTextStyles。
  // ==========================================================================

  // Surfaces
  static const rtuBg = Color(0xFFEDF1F6); // --bg
  static const rtuSurface = Color(0xFFFFFFFF); // --surface
  static const rtuSurface2 = Color(0xFFF6F8FB); // --surface-2
  static const rtuSurface3 = Color(0xFFEDF1F7); // --surface-3

  // Text
  static const rtuText = Color(0xFF16202E); // --text
  static const rtuText2 = Color(0xFF5C6B7E); // --text-2
  static const rtuText3 = Color(0xFF93A1B3); // --text-3

  // Lines
  static const rtuLine = Color(0xFFE3E9F1); // --line
  static const rtuLine2 = Color(0xFFD2DBE6); // --line-2
  static const rtuBarTrack = Color(0xFFE8EDF4); // .bar 轨道 / 空状态底色

  // Brand & accent
  static const rtuBrand = Color(0xFF0A6FD8); // --brand
  static const rtuBrandD = Color(0xFF04509F); // --brand-d
  static const rtuBrandL = Color(0xFFE8F1FC); // --brand-l
  static const rtuBrandL2 = Color(0xFFCFE2F8); // --brand-l2
  static const rtuCyan = Color(0xFF0EAFBE); // --cyan
  static const rtuCyanD = Color(0xFF0A8C99); // .devs .ic(cy) 图标色
  static const rtuCyanL = Color(0xFFE2F6F8); // --cyan-l

  // Status
  static const rtuOk = Color(0xFF10A56A); // --ok
  static const rtuOkL = Color(0xFFE4F6ED); // --ok-l
  static const rtuOkFg = Color(0xFF0A7A4E); // .alert.ok 文字
  static const rtuWarn = Color(0xFFE08A00); // --warn
  static const rtuWarnL = Color(0xFFFDF2DF); // --warn-l
  static const rtuWarnFg = Color(0xFF8A5A00); // .alert.w 文字
  static const rtuErr = Color(0xFFE04B4B); // --err
  static const rtuErrL = Color(0xFFFCEAEA); // --err-l

  // Interactive
  static const rtuHover = Color(0xFFFBFDFF); // .row:hover / .navbtn:hover
  static const rtuRowBad = Color(0xFFFFF9F9); // .drow.bad

  // Overlays & highlights
  static const rtuMask = Color(0x6B0E1721); // rgba(14,23,33,.42) .mask
  static const rtuToast = Color(0xE616202E); // rgba(22,32,46,.9) .toast
  static const rtuHeroDot = Color(0xFF7BF3B4); // .hero .st .d
  static const rtuHeroDotGlow = Color(0x487BF3B4); // rgba(123,243,180,.28)
  static const rtuPhoneFrame = Color(0xFF0E1721); // 原型机壳，仅截图/占位用

  // Gradients（渐变 hero 卡 / 雷达核心）
  static const List<Color> rtuHeroGradient = <Color>[
    Color(0xFF0A6FD8), // 0%
    Color(0xFF0B86C9), // 55%
    Color(0xFF0EAFBE), // 100%
  ];
  static const List<Color> rtuRadarCoreGradient = <Color>[rtuBrand, rtuCyan];

  // Shadow colors（形状见 RtuShadows）
  static const rtuShadowHero = Color(0x3D0A6FD8); // rgba(10,111,216,.24)
  static const rtuShadowCore = Color(0x4C0A6FD8); // rgba(10,111,216,.3)
  static const rtuShadowDialog = Color(0x470E1721); // rgba(14,23,33,.28)
  static const rtuRadarRing = Color(0x660A6FD8); // rgba(10,111,216,.4)
}
