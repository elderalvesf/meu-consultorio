import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import '../../providers/auth_provider.dart';
import '../../providers/appointment_provider.dart';
import '../../providers/patient_provider.dart';
import '../../providers/payment_provider.dart';
import '../../data/models/appointment.dart';
import '../../widgets/common_widgets.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(patientProvider.notifier).loadAll();
      ref.read(appointmentProvider.notifier).loadAll();
      ref.read(paymentProvider.notifier).loadAll();
    });
  }

  @override
  Widget build(BuildContext context) {
    final patientState = ref.watch(patientProvider);
    final appointmentState = ref.watch(appointmentProvider);
    final paymentState = ref.watch(paymentProvider);
    final cs = Theme.of(context).colorScheme;
    final isTablet = MediaQuery.of(context).size.shortestSide >= 600;

    final today = DateFormat("EEEE, dd 'de' MMMM", 'pt_BR').format(DateTime.now());
    final todayStart = DateTime.now().copyWith(hour: 0, minute: 0, second: 0, millisecond: 0);
    final todayEnd = todayStart.add(const Duration(days: 1));
    final todayAppointments = appointmentState.allAppointments.where((a) {
      final dt = DateTime.fromMillisecondsSinceEpoch(a.dateTime);
      return dt.isAfter(todayStart) && dt.isBefore(todayEnd);
    }).toList()
      ..sort((a, b) => a.dateTime.compareTo(b.dateTime));

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Meu Consultório'),
            Text(today.replaceFirstMapped(RegExp(r'^\w'), (m) => m.group(0)!.toUpperCase()),
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: cs.onPrimary.withOpacity(0.8))),
          ],
        ),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Nova consulta',
            onPressed: () => context.push('/appointment-form'),
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              if (value == 'signout') {
                showDialog(
                  context: context,
                  builder: (_) => AlertDialog(
                    title: const Text('Sair'),
                    content: const Text('Deseja sair da sua conta?'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
                      TextButton(
                        onPressed: () {
                          Navigator.pop(context);
                          ref.read(authProvider.notifier).signOut();
                        },
                        child: const Text('Sair'),
                      ),
                    ],
                  ),
                );
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: 'signout', child: Row(
                children: [Icon(Icons.logout, size: 18), SizedBox(width: 8), Text('Sair')],
              )),
            ],
          ),
        ],
      ),
      body: isTablet
          ? _tabletLayout(context, patientState, todayAppointments, paymentState, cs)
          : _phoneLayout(context, patientState, todayAppointments, paymentState, cs),
    );
  }

  Widget _tabletLayout(context, patientState, todayAppointments, paymentState, cs) {
    return Row(
      children: [
        SizedBox(
          width: 340,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SectionTitle(text: 'Resumo'),
                _statsGrid(context, patientState, todayAppointments, paymentState, cs),
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: () => context.push('/appointment-form'),
                  icon: const Icon(Icons.add),
                  label: const Text('Nova consulta'),
                  style: OutlinedButton.styleFrom(minimumSize: const Size(double.infinity, 48)),
                ),
              ],
            ),
          ),
        ),
        const VerticalDivider(width: 1),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SectionTitle(text: 'Consultas de hoje'),
              Expanded(child: _appointmentsList(todayAppointments)),
            ],
          ),
        ),
      ],
    );
  }

  Widget _phoneLayout(context, patientState, todayAppointments, paymentState, cs) {
    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.all(16),
          sliver: SliverList(
            delegate: SliverChildListDelegate([
              const SectionTitle(text: 'Resumo'),
              const SizedBox(height: 8),
              _statsGrid(context, patientState, todayAppointments, paymentState, cs),
              const SizedBox(height: 16),
              const SectionTitle(text: 'Consultas de hoje'),
              const SizedBox(height: 8),
            ]),
          ),
        ),
        todayAppointments.isEmpty
            ? SliverFillRemaining(
                child: EmptyState(message: 'Nenhuma consulta para hoje', icon: Icons.calendar_today_outlined),
              )
            : SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (_, i) => Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: _TodayAppointmentCard(appointment: todayAppointments[i]),
                    ),
                    childCount: todayAppointments.length,
                  ),
                ),
              ),
      ],
    );
  }

  Widget _statsGrid(context, patientState, todayAppointments, paymentState, cs) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(child: _StatCard(icon: Icons.people, label: 'Pacientes',
                value: '${patientState.patients.length}', color: cs.primary,
                onTap: () => context.go('/patients'))),
            const SizedBox(width: 12),
            Expanded(child: _StatCard(icon: Icons.calendar_month, label: 'Hoje',
                value: '${todayAppointments.length}', color: cs.secondary,
                onTap: () => context.go('/appointments'))),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(child: _StatCard(icon: Icons.attach_money, label: 'Mês atual',
                value: formatCurrency(paymentState.monthReceived), color: cs.tertiary)),
            const SizedBox(width: 12),
            Expanded(child: _StatCard(icon: Icons.savings_outlined, label: 'Total recebido',
                value: formatCurrency(paymentState.totalReceived), color: const Color(0xFF7B1FA2))),
          ],
        ),
      ],
    );
  }

  Widget _appointmentsList(List<Appointment> appointments) {
    if (appointments.isEmpty) return EmptyState(message: 'Nenhuma consulta para hoje');
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: appointments.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (_, i) => _TodayAppointmentCard(appointment: appointments[i]),
    );
  }
}

class _StatCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final Color color;
  final VoidCallback? onTap;

  const _StatCard({required this.icon, required this.label, required this.value, required this.color, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: color, size: 28),
              const SizedBox(height: 6),
              Text(value,
                  style: Theme.of(context).textTheme.titleLarge?.copyWith(
                        color: color, fontWeight: FontWeight.bold)),
              Text(label,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurface.withOpacity(0.6))),
            ],
          ),
        ),
      ),
    );
  }
}

class _TodayAppointmentCard extends StatelessWidget {
  final Appointment appointment;
  const _TodayAppointmentCard({required this.appointment});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Container(
              width: 56, height: 56,
              decoration: BoxDecoration(color: cs.primary, borderRadius: BorderRadius.circular(8)),
              child: Center(
                child: Text(formatTime(appointment.dateTime),
                    style: TextStyle(color: cs.onPrimary, fontWeight: FontWeight.bold, fontSize: 13)),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(appointment.procedureType, style: Theme.of(context).textTheme.titleSmall),
                  Text('${appointment.durationMinutes} min',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: cs.onSurface.withOpacity(0.6))),
                ],
              ),
            ),
            StatusChip(label: appointment.status.label, color: _statusColor(appointment.status, cs)),
          ],
        ),
      ),
    );
  }

  Color _statusColor(AppointmentStatus s, ColorScheme cs) => switch (s) {
        AppointmentStatus.AGENDADA => cs.primary,
        AppointmentStatus.CONFIRMADA => cs.secondary,
        AppointmentStatus.CONCLUIDA => cs.tertiary,
        AppointmentStatus.CANCELADA => cs.error,
        AppointmentStatus.NAO_COMPARECEU => cs.errorContainer,
      };
}
