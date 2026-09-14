import 'package:go_router/go_router.dart';
import 'package:thingsboard_app/modules/rtu/rtu_page.dart';

class RtuRoutes {
  static const rtu = '/rtu';
}

final rtuRoutes = <GoRoute>[
  GoRoute(path: RtuRoutes.rtu, builder: (context, state) => const RtuPage()),
];
