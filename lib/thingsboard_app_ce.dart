import 'package:country_picker/country_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_localized_locales/flutter_localized_locales.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import 'package:thingsboard_app/config/routes/v2/router_2.dart';
import 'package:thingsboard_app/config/themes/dark_theme.dart';
import 'package:thingsboard_app/config/themes/tb_ce_theme.dart';
import 'package:thingsboard_app/core/auth/login/models/login_state.dart';
import 'package:thingsboard_app/core/auth/login/provider/login_provider.dart';
import 'package:thingsboard_app/generated/l10n.dart';
import 'package:toastification/toastification.dart';

class ThingsboardApp extends HookConsumerWidget {
  const ThingsboardApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    final login = ref.watch(loginProvider);

    // 使用用户在服务端保存的语言偏好作为 App 语言，避免被设备语言覆盖。
    // 未登录/未加载用户时返回 null，由 Flutter 回退到设备语言（登录页等场景）。
    final Locale? appLocale = _resolveLocaleFromLogin(login);

    return ToastificationWrapper(
   child :  ColoredBox(
        color: tbCeTheme.scaffoldBackgroundColor,
        child: MaterialApp.router(
          debugShowCheckedModeBanner: false,
          color: tbCeTheme.scaffoldBackgroundColor,
          locale: appLocale,
          localizationsDelegates: const [
            S.delegate,
            LocaleNamesLocalizationsDelegate(),
            CountryLocalizations.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          supportedLocales: S.delegate.supportedLocales,
          onGenerateTitle: (BuildContext context) => S.of(context).appTitle,
          themeMode: ThemeMode.light,
          theme: tbCeTheme,
          darkTheme: tbDarkTheme,
          routerConfig: router,
        ),
   )
    );
  }
}

Locale? _resolveLocaleFromLogin(LoginState login) {
  if (!login.isUserLoaded || login.user == null) return null;
  final lang = login.user!.additionalInfo?['lang']?.toString();
  if (lang == null || lang.isEmpty) return null;
  for (final l in S.delegate.supportedLocales) {
    if (l.toString() == lang || l.languageCode == lang) return l;
  }
  return null;
}
