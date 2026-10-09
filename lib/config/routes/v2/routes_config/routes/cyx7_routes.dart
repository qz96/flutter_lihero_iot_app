import 'package:go_router/go_router.dart';
import 'package:thingsboard_app/modules/cyx7/cyx7_page.dart';
import 'package:thingsboard_app/modules/cyx7/cyx7_param_set_page.dart';
import 'package:thingsboard_app/modules/cyx7/cyx7_rtu_page.dart';

class Cyx7Routes {
  static const cyx7 = '/cyx7';
  static const cyx7Rtu = '/cyx7/rtu';
  static const cyx7ParamSet = '/cyx7/param-set';
}

final cyx7Routes = <GoRoute>[
  GoRoute(
    path: Cyx7Routes.cyx7,
    builder: (context, state) => const Cyx7Page(),
    routes: [
      GoRoute(
        path: 'rtu',
        builder: (context, state) => const Cyx7RtuPage(),
      ),
      GoRoute(
        path: 'param-set',
        builder: (context, state) => const Cyx7ParamSetPage(),
      ),
    ],
  ),
];
