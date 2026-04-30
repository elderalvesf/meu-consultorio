import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers/auth_provider.dart';
import '../screens/auth/login_screen.dart';
import '../screens/home/home_screen.dart';
import '../screens/patients/patient_list_screen.dart';
import '../screens/patients/patient_detail_screen.dart';
import '../screens/patients/patient_form_screen.dart';
import '../screens/appointments/appointment_list_screen.dart';
import '../screens/appointments/appointment_form_screen.dart';
import '../screens/financial/financial_screen.dart';
import '../screens/financial/payment_form_screen.dart';
import '../screens/treatments/treatment_form_screen.dart';
import '../screens/prontuario/prontuario_form_screen.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  final authState = ref.watch(authProvider);

  return GoRouter(
    initialLocation: '/',
    redirect: (context, state) {
      final isLoggedIn = authState.status == AuthStatus.authenticated;
      final isLoading = authState.status == AuthStatus.loading;
      final isOnLogin = state.matchedLocation == '/login';

      if (isLoading) return null;
      if (!isLoggedIn && !isOnLogin) return '/login';
      if (isLoggedIn && isOnLogin) return '/';
      return null;
    },
    routes: [
      ShellRoute(
        builder: (context, state, child) => MainShell(child: child),
        routes: [
          GoRoute(path: '/', builder: (_, __) => const HomeScreen()),
          GoRoute(path: '/patients', builder: (_, __) => const PatientListScreen()),
          GoRoute(path: '/appointments', builder: (_, __) => const AppointmentListScreen()),
          GoRoute(path: '/financial', builder: (_, __) => const FinancialScreen()),
        ],
      ),
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(
        path: '/patients/:id',
        builder: (_, state) => PatientDetailScreen(
          patientId: int.parse(state.pathParameters['id']!),
        ),
      ),
      GoRoute(
        path: '/patient-form',
        builder: (_, state) => PatientFormScreen(
          patientId: state.uri.queryParameters['patientId'] != null
              ? int.tryParse(state.uri.queryParameters['patientId']!)
              : null,
        ),
      ),
      GoRoute(
        path: '/appointment-form',
        builder: (_, state) => AppointmentFormScreen(
          appointmentId: state.uri.queryParameters['appointmentId'] != null
              ? int.tryParse(state.uri.queryParameters['appointmentId']!)
              : null,
          preselectedPatientId: state.uri.queryParameters['patientId'] != null
              ? int.tryParse(state.uri.queryParameters['patientId']!)
              : null,
        ),
      ),
      GoRoute(
        path: '/payment-form',
        builder: (_, state) => PaymentFormScreen(
          paymentId: state.uri.queryParameters['paymentId'] != null
              ? int.tryParse(state.uri.queryParameters['paymentId']!)
              : null,
          preselectedPatientId: state.uri.queryParameters['patientId'] != null
              ? int.tryParse(state.uri.queryParameters['patientId']!)
              : null,
        ),
      ),
      GoRoute(
        path: '/treatment-form',
        builder: (_, state) => TreatmentFormScreen(
          treatmentId: state.uri.queryParameters['treatmentId'] != null
              ? int.tryParse(state.uri.queryParameters['treatmentId']!)
              : null,
          preselectedPatientId: state.uri.queryParameters['patientId'] != null
              ? int.tryParse(state.uri.queryParameters['patientId']!)
              : null,
        ),
      ),
      GoRoute(
        path: '/prontuario-form',
        builder: (_, state) => ProntuarioFormScreen(
          patientId: int.parse(state.uri.queryParameters['patientId']!),
          appointmentId: state.uri.queryParameters['appointmentId'] != null
              ? int.tryParse(state.uri.queryParameters['appointmentId']!)
              : null,
          entryId: state.uri.queryParameters['entryId'] != null
              ? int.tryParse(state.uri.queryParameters['entryId']!)
              : null,
        ),
      ),
    ],
  );
});

class MainShell extends StatelessWidget {
  final Widget child;
  const MainShell({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    final location = GoRouterState.of(context).matchedLocation;
    final isTablet = MediaQuery.of(context).size.shortestSide >= 600;

    final destinations = [
      const NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Início'),
      const NavigationDestination(icon: Icon(Icons.people_outlined), selectedIcon: Icon(Icons.people), label: 'Pacientes'),
      const NavigationDestination(icon: Icon(Icons.calendar_month_outlined), selectedIcon: Icon(Icons.calendar_month), label: 'Agenda'),
      const NavigationDestination(icon: Icon(Icons.attach_money), label: 'Financeiro'),
    ];

    final routes = ['/', '/patients', '/appointments', '/financial'];
    final currentIndex = routes.indexWhere((r) => location == r).clamp(0, 3);

    void onDestinationSelected(int index) => context.go(routes[index]);

    if (isTablet) {
      return Scaffold(
        body: Row(
          children: [
            NavigationRail(
              selectedIndex: currentIndex,
              onDestinationSelected: onDestinationSelected,
              labelType: NavigationRailLabelType.all,
              leading: Padding(
                padding: const EdgeInsets.symmetric(vertical: 16),
                child: Icon(Icons.medical_services, color: Theme.of(context).colorScheme.primary, size: 32),
              ),
              destinations: destinations
                  .map((d) => NavigationRailDestination(
                        icon: d.icon,
                        selectedIcon: d.selectedIcon ?? d.icon,
                        label: Text(d.label),
                      ))
                  .toList(),
            ),
            const VerticalDivider(width: 1),
            Expanded(child: child),
          ],
        ),
      );
    }

    return Scaffold(
      body: child,
      bottomNavigationBar: NavigationBar(
        selectedIndex: currentIndex,
        onDestinationSelected: onDestinationSelected,
        destinations: destinations,
      ),
    );
  }
}
