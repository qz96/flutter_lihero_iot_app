import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';

import 'package:thingsboard_app/config/themes/app_colors.dart';

/// RTU 模块专用的几何 token。
/// 取值来源：RTU 交互原型的 CSS（`--r:14px`、`.devs` 13px、`.hero` 18px 等），
/// 与项目全局的 `DesignTokens` 有意解耦，避免污染其它页面。
class RtuTokens {
  RtuTokens._();

  // ===== Radius =====
  static const double radiusCard = 14; // --r，.card / .dlist
  static const double radiusRow = 12; // .mbox / .row
  static const double radiusDevice = 13; // .devs / .qbtn
  static const double radiusHero = 18; // .hero / .dlg
  static const double radiusMetric = 11; // .hero .m
  static const double radiusIconTile = 10; // .row .ic / .qbtn .ic
  static const double radiusIconTileLg = 11; // .devs .ic
  static const double radiusControl = 11; // .seg / .back
  static const double radiusInput = 9; // .inp
  static const double radiusButton = 12; // .btn
  static const double radiusButtonSm = 10; // .btn.sm
  static const double radiusChip = 100; // .chip 胶囊

  // ===== Bar / Track =====
  static const double barTrackHeight = 6; // .bar 轨道高度


  // ===== Spacing =====
  static const double pageInset = 14; // .hd / .body 左右 padding
  static const double pageBottom = 22; // .body 底部 padding
  static const double cardPadding = 14; // .card
  static const double cardPaddingTight = 13; // .card.tight
  static const double rowPadding = 13; // .row / .devs padding
  static const double rowPaddingSm = 12; // .row padding
  static const double gapXs = 4; // .chip gap
  static const double gapSm = 6; // .qbtn gap / .dlist gap
  static const double gapMd = 8; // .quick / .grid3 gap
  static const double gapLg = 9; // .grid2 / .hero .metrics gap
  static const double gapXl = 11; // .row / .field gap
  static const double gapXxl = 12; // .devs / .hd gap
  static const double blockGap = 11; // .card margin-bottom
  static const double sectionGap = 16; // .sec margin-top
  static const double fieldPadding = 11; // .field padding

  // ===== Sizes =====
  static const double iconTileSm = 33; // .row .ic
  static const double iconTile = 36; // .devs .ic
  static const double iconTileNav = 30; // .qbtn .ic
  static const double radarSize = 110; // .radar
  static const double radarCore = 52; // .radar .core
  static const double radarRing = 74; // .radar i
  static const double buttonHeight = 47; // .btn: padding 13 + 13.5*1.55
  static const double buttonHeightSm = 41; // .btn.sm: padding 10 + 12.5*1.55
  static const double signalBarHeight = 13; // .sig
  static const double signalBarWidth = 3; // .sig i
  static const double statusDot = 6; // .chip .d / .hero .st .d
  static const double progressHeight = 5; // .bar
  static const double sparkHeight = 22; // .spark

  // ===== Common EdgeInsets =====
  static const EdgeInsets pageInsets = EdgeInsets.symmetric(
    horizontal: pageInset,
  );
  static const EdgeInsets bodyInsets = EdgeInsets.fromLTRB(
    pageInset,
    0,
    pageInset,
    pageBottom,
  );
  static const EdgeInsets cardInsets = EdgeInsets.all(cardPadding);
  static const EdgeInsets cardInsetsTight = EdgeInsets.all(cardPaddingTight);

  // ===== BorderRadius =====
  static BorderRadius get brCard => BorderRadius.circular(radiusCard);
  static BorderRadius get brRow => BorderRadius.circular(radiusRow);
  static BorderRadius get brDevice => BorderRadius.circular(radiusDevice);
  static BorderRadius get brHero => BorderRadius.circular(radiusHero);
  static BorderRadius get brIconTile => BorderRadius.circular(radiusIconTile);
  static BorderRadius get brIconTileLg =>
      BorderRadius.circular(radiusIconTileLg);
  static BorderRadius get brInput => BorderRadius.circular(radiusInput);
  static BorderRadius get brButton => BorderRadius.circular(radiusButton);
  static BorderRadius get brButtonSm => BorderRadius.circular(radiusButtonSm);
  static BorderRadius get brChip => BorderRadius.circular(radiusChip);

  // ===== Common colors（页面私有 _R 之外的兜底中性色） =====
  /// .bar / 空状态轨道的浅灰底色。
  static const Color barTrack = AppColors.rtuBarTrack;
}

/// RTU 模块专用阴影，与原型 CSS 的 box-shadow 一一对应。
class RtuShadows {
  RtuShadows._();

  /// .hero → 0 10px 22px rgba(10,111,216,.24)
  static List<BoxShadow> get hero => const [
    BoxShadow(
      color: AppColors.rtuShadowHero,
      offset: Offset(0, 10),
      blurRadius: 22,
    ),
  ];

  /// .radar .core → 0 8px 20px rgba(10,111,216,.3)
  static List<BoxShadow> get radarCore => const [
    BoxShadow(
      color: AppColors.rtuShadowCore,
      offset: Offset(0, 8),
      blurRadius: 20,
    ),
  ];

  /// .dlg → 0 20px 50px rgba(14,23,33,.28)
  static List<BoxShadow> get dialog => const [
    BoxShadow(
      color: AppColors.rtuShadowDialog,
      offset: Offset(0, 20),
      blurRadius: 50,
    ),
  ];

  /// .seg button.on → 0 1px 3px rgba(16,32,46,.09)
  static List<BoxShadow> get segment => const [
    BoxShadow(
      color: Color(0x1710202E),
      offset: Offset(0, 1),
      blurRadius: 3,
    ),
  ];

  /// .tabbar 顶部分隔线替代阴影（backdrop 卡片）
  static List<BoxShadow> get tabBar => const [
    BoxShadow(
      color: Color(0x080E1721),
      offset: Offset(0, -1),
      blurRadius: 0,
    ),
  ];
}

/// RTU 模块专用字号。
/// 字重 / 字号 / 行高 / 字间距取自原型 CSS，
/// 字体仍统一使用项目的 Roboto（`TbTextStyles`），保证与 App 字体系一致。
class RtuTextStyles {
  RtuTextStyles._();

  static TextStyle _style({
    required double fontSize,
    FontWeight fontWeight = FontWeight.w400,
    double? height,
    double? letterSpacing,
  }) =>
      GoogleFonts.roboto(
        fontSize: fontSize,
        fontWeight: fontWeight,
        height: height,
        letterSpacing: letterSpacing,
      );

  // 头部 .hd .ttl 16/700 ls-.2 / .hd .sub 10.5 ls.2
  static final TextStyle pageTitle = _style(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    height: 1.2,
    letterSpacing: -0.2,
  );
  static final TextStyle pageSubtitle = _style(
    fontSize: 10.5,
    height: 1.2,
    letterSpacing: 0.2,
  );

  // 区块标题 .sec h3 12/700 ls.4 / .sec a 11.5/600
  static final TextStyle sectionTitle = _style(
    fontSize: 12,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.4,
  );
  static final TextStyle sectionAction = _style(
    fontSize: 11.5,
    fontWeight: FontWeight.w600,
  );

  // Hero 卡 .nm 17/700 ls-.3 / .meta 11 ls.2 / .m .k 10 / .m .v 16/700
  static final TextStyle heroName = _style(
    fontSize: 17,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.3,
  );
  static final TextStyle heroMeta = _style(
    fontSize: 11,
    letterSpacing: 0.2,
  );
  static final TextStyle heroMetricKey = _style(
    fontSize: 10,
    letterSpacing: 0.3,
  );
  static final TextStyle heroMetricValue = _style(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.3,
  );
  static final TextStyle heroFoot = _style(fontSize: 10.5);
  static final TextStyle heroStatus = _style(
    fontSize: 11,
    fontWeight: FontWeight.w600,
  );

  // 设备行 .devs .a 13.5/700 / .devs .b 10.5 mono
  static final TextStyle deviceName = _style(
    fontSize: 13.5,
    fontWeight: FontWeight.w700,
  );
  static final TextStyle deviceMeta = _style(fontSize: 10.5);

  // 通用行 .row .tx .a 13.5/600 / .b 11 / .val 12/600
  static final TextStyle rowTitle = _style(
    fontSize: 13.5,
    fontWeight: FontWeight.w600,
    letterSpacing: -0.1,
  );
  static final TextStyle rowSubtitle = _style(fontSize: 11);
  static final TextStyle rowValue = _style(
    fontSize: 12,
    fontWeight: FontWeight.w600,
  );

  // 数值格 .mbox .k 10.5 / .v 19/700 ls-.5
  static final TextStyle metricKey = _style(fontSize: 10.5);
  static final TextStyle metricValue = _style(
    fontSize: 19,
    fontWeight: FontWeight.w700,
    letterSpacing: -0.5,
  );

  // 表单 .field .lb 13 / .inp 13 / .unit 11.5
  static final TextStyle fieldLabel = _style(fontSize: 13);
  static final TextStyle fieldLabelHint = _style(fontSize: 10.5);
  static final TextStyle fieldValue = _style(fontSize: 13);
  static final TextStyle unit = _style(fontSize: 11.5);

  // 键值行 .kv 12.5 / .v 600
  static final TextStyle keyValue = _style(fontSize: 12.5);
  static final TextStyle keyValueStrong = _style(
    fontSize: 12.5,
    fontWeight: FontWeight.w600,
  );

  // 数据表格 .dhead 11/700 ls.3 / .drow 11
  static final TextStyle dataHead = _style(
    fontSize: 11,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.3,
  );
  static final TextStyle dataRow = _style(fontSize: 11);

  // 传感器 .sens .nm 13/600 / .ds 10.5
  static final TextStyle sensorName = _style(
    fontSize: 13,
    fontWeight: FontWeight.w600,
  );
  static final TextStyle sensorDesc = _style(fontSize: 10.5);

  // 提示 / 说明 / 警告
  static final TextStyle hint = _style(fontSize: 11, height: 1.7);
  static final TextStyle hintSm = _style(fontSize: 10.5, height: 1.6);
  static final TextStyle body2 = _style(fontSize: 12.5);
  static final TextStyle alert = _style(fontSize: 12, height: 1.6);

  // 其它控件
  static final TextStyle chip = _style(
    fontSize: 10.5,
    fontWeight: FontWeight.w600,
  );
  static final TextStyle button = _style(
    fontSize: 13.5,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.2,
  );
  static final TextStyle buttonSm = _style(
    fontSize: 12.5,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.2,
  );
  static final TextStyle quickLabel = _style(
    fontSize: 10.5,
    fontWeight: FontWeight.w600,
  );
  static final TextStyle tabLabel = _style(
    fontSize: 10,
    fontWeight: FontWeight.w600,
  );
  static final TextStyle dialogTitle = _style(
    fontSize: 15,
    fontWeight: FontWeight.w700,
  );
  static final TextStyle dialogBody = _style(fontSize: 12.5, height: 1.65);
  static final TextStyle toast = _style(fontSize: 12);
}

/// RTU 主题的装配入口。
///
/// 关键点：**只在 RTU 页面子树生效**。
/// 通过 `Theme(data: ...)` 局部覆盖，而不是改全局 MaterialApp theme，
/// 因此项目其余页面的配色完全不受影响。
class RtuTheme {
  RtuTheme._();

  /// 在 RTU 页面最外层包裹，使页面内所有 Material 组件继承 RTU 配色。
  static Widget apply({required BuildContext context, required Widget child}) {
    return Theme(data: of(context), child: child);
  }

  /// 生成 RTU 局部 ThemeData（基于全局主题 copy，保留 useMaterial3 等设置）。
  static ThemeData of(BuildContext context) {
    final base = Theme.of(context);
    return base.copyWith(
      scaffoldBackgroundColor: AppColors.rtuBg,
      canvasColor: AppColors.rtuSurface2,
      cardColor: AppColors.rtuSurface,
      dividerColor: AppColors.rtuLine,
      hoverColor: AppColors.rtuHover,
      splashColor: AppColors.rtuBrand.withValues(alpha: .08),
      highlightColor: AppColors.rtuBrandL.withValues(alpha: .6),
      focusColor: AppColors.rtuBrand,
      disabledColor: AppColors.rtuText3,
      colorScheme: base.colorScheme.copyWith(
        primary: AppColors.rtuBrand,
        onPrimary: AppColors.rtuSurface,
        secondary: AppColors.rtuCyan,
        onSecondary: AppColors.rtuSurface,
        surface: AppColors.rtuSurface,
        onSurface: AppColors.rtuText,
        surfaceContainerHighest: AppColors.rtuSurface3,
        outline: AppColors.rtuLine,
        outlineVariant: AppColors.rtuLine2,
        error: AppColors.rtuErr,
        onError: AppColors.rtuSurface,
      ),
      iconTheme: const IconThemeData(color: AppColors.rtuText2, size: 18),
      primaryIconTheme: const IconThemeData(
        color: AppColors.rtuSurface,
        size: 18,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.rtuSurface2,
        foregroundColor: AppColors.rtuText,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        iconTheme: const IconThemeData(color: AppColors.rtuText2, size: 16),
        actionsIconTheme: const IconThemeData(
          color: AppColors.rtuText2,
          size: 16,
        ),
        titleTextStyle: RtuTextStyles.pageTitle.copyWith(
          color: AppColors.rtuText,
        ),
      ),
      cardTheme: CardThemeData(
        color: AppColors.rtuSurface,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: RtuTokens.brCard,
          side: const BorderSide(color: AppColors.rtuLine),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.rtuLine,
        thickness: 1,
        space: 1,
      ),
      listTileTheme: const ListTileThemeData(
        dense: true,
        contentPadding: EdgeInsets.zero,
        visualDensity: VisualDensity.compact,
        iconColor: AppColors.rtuText2,
        textColor: AppColors.rtuText,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.rtuBrand,
        linearTrackColor: AppColors.rtuLine,
        circularTrackColor: AppColors.rtuLine,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.rtuBrand,
          foregroundColor: AppColors.rtuSurface,
          disabledBackgroundColor: AppColors.rtuSurface3,
          disabledForegroundColor: AppColors.rtuText3,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 13),
          minimumSize: const Size(0, RtuTokens.buttonHeight),
          textStyle: RtuTextStyles.button,
          shape: RoundedRectangleBorder(borderRadius: RtuTokens.brButton),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          backgroundColor: AppColors.rtuSurface,
          foregroundColor: AppColors.rtuText,
          elevation: 0,
          side: const BorderSide(color: AppColors.rtuLine2),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 13),
          minimumSize: const Size(0, RtuTokens.buttonHeight),
          textStyle: RtuTextStyles.button,
          shape: RoundedRectangleBorder(borderRadius: RtuTokens.brButton),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.rtuBrand,
          textStyle: RtuTextStyles.sectionAction,
          padding: EdgeInsets.zero,
          minimumSize: Size.zero,
          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        isDense: true,
        filled: true,
        fillColor: AppColors.rtuSurface2,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 11,
          vertical: 7,
        ),
        hintStyle: RtuTextStyles.fieldValue.copyWith(color: AppColors.rtuText3),
        labelStyle: RtuTextStyles.fieldLabel.copyWith(
          color: AppColors.rtuText2,
        ),
        helperStyle: RtuTextStyles.fieldLabelHint.copyWith(
          color: AppColors.rtuText3,
        ),
        errorStyle: RtuTextStyles.fieldLabelHint.copyWith(
          color: AppColors.rtuErr,
        ),
        border: OutlineInputBorder(
          borderRadius: RtuTokens.brInput,
          borderSide: const BorderSide(color: AppColors.rtuLine2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: RtuTokens.brInput,
          borderSide: const BorderSide(color: AppColors.rtuLine2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: RtuTokens.brInput,
          borderSide: const BorderSide(color: AppColors.rtuBrand, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: RtuTokens.brInput,
          borderSide: const BorderSide(color: AppColors.rtuErr),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: RtuTokens.brInput,
          borderSide: const BorderSide(color: AppColors.rtuErr, width: 1.5),
        ),
      ),
      switchTheme: SwitchThemeData(
        thumbColor: WidgetStateProperty.all(AppColors.rtuSurface),
        trackColor: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return AppColors.rtuBrand;
          }
          return AppColors.rtuLine2;
        }),
        trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: AppColors.rtuSurface,
        selectedItemColor: AppColors.rtuBrand,
        unselectedItemColor: AppColors.rtuText3,
        selectedLabelStyle: RtuTextStyles.tabLabel.copyWith(
          color: AppColors.rtuBrand,
        ),
        unselectedLabelStyle: RtuTextStyles.tabLabel.copyWith(
          color: AppColors.rtuText3,
        ),
        showSelectedLabels: true,
        showUnselectedLabels: true,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
      ),
    );
  }
}
