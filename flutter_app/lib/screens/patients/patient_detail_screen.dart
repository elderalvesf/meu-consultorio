import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/patient_provider.dart';
import '../../providers/appointment_provider.dart';
import '../../providers/treatment_provider.dart';
import '../../providers/payment_provider.dart';
import '../../providers/prontuario_provider.dart';
import '../../data/models/appointment.dart';
import '../../widgets/common_widgets.dart';

class PatientDetailScreen extends ConsumerStatefulWidget {
  final int patientId;
  const PatientDetailScreen({super.key, required this.patientId});

  @override
  ConsumerState<PatientDetailScreen> createState() => _PatientDetailScreenState();
}

class _PatientDetailScreenState extends ConsumerState<PatientDetailScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    Future.microtask(() {
      ref.read(appointmentProvider.notifier).loadByPatient(widget.patientId);
      ref.read(treatmentProvider.notifier).loadByPatient(widget.patientId);
      ref.read(paymentProvider.notifier).loadByPatient(widget.patientId);
      ref.read(prontuarioProvider.notifier).loadByPatient(widget.patientId);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final patients = ref.watch(patientProvider).patients;
    final patient = patients.where((p) => p.id == widget.patientId).firstOrNull;
    final apptState = ref.watch(appointmentProvider);
    final treatState = ref.watch(treatmentProvider);
    final payState = ref.watch(paymentProvider);
    final pronState = ref.watch(prontuarioProvider);

    if (patient == null) return Scaffold(appBar: AppBar(title: const Text('Paciente')), body: const LoadingIndicator());

    final cs = Theme.of(context).colorScheme;
    final patientAppointments = apptState.patientAppointments;
    final patientTreatments = treatState.patientTreatments;
    final patientPayments = payState.patientPayments;
    final patientProntuario = pronState.patientEntries;

    return Scaffold(
      appBar: AppBar(
        title: Text(patient.name),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () => context.push('/patient-form?patientId=${patient.id}'),
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: () => _confirmDelete(context, patient.id),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          labelColor: cs.onPrimary,
          unselectedLabelColor: cs.onPrimary.withOpacity(0.6),
          indicatorColor: cs.onPrimary,
          tabs: const [
            Tab(text: 'Consultas'),
            Tab(text: 'Tratamentos'),
            Tab(text: 'Pagamentos'),
            Tab(text: 'Prontuário'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // Consultas
          _listOrEmpty(
            items: patientAppointments,
            empty: 'Nenhuma consulta',
            icon: Icons.calendar_month_outlined,
            fab: () => context.push('/appointment-form?patientId=${patient.id}'),
            itemBuilder: (i) {
              final a = patientAppointments[i];
              return Card(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                child: ListTile(
                  leading: Icon(Icons.calendar_today, color: cs.primary),
                  title: Text(a.procedureType),
                  subtitle: Text(formatDateTime(a.dateTime)),
                  trailing: StatusChip(label: a.status.label, color: _apptColor(a.status, cs)),
                  onTap: () => context.push('/appointment-form?appointmentId=${a.id}'),
                ),
              );
            },
          ),
          // Tratamentos
          _listOrEmpty(
            items: patientTreatments,
            empty: 'Nenhum tratamento',
            icon: Icons.medical_services_outlined,
            fab: () => context.push('/treatment-form?patientId=${patient.id}'),
            itemBuilder: (i) {
              final t = patientTreatments[i];
              return Card(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                child: ListTile(
                  leading: Icon(Icons.healing, color: cs.secondary),
                  title: Text(t.procedure),
                  subtitle: Text(t.tooth.isNotEmpty ? 'Dente: ${t.tooth}' : t.description),
                  trailing: Text(formatCurrency(t.cost), style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                  onTap: () => context.push('/treatment-form?treatmentId=${t.id}&patientId=${patient.id}'),
                ),
              );
            },
          ),
          // Pagamentos
          _listOrEmpty(
            items: patientPayments,
            empty: 'Nenhum pagamento',
            icon: Icons.attach_money,
            fab: () => context.push('/payment-form?patientId=${patient.id}'),
            itemBuilder: (i) {
              final p = patientPayments[i];
              return Card(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                child: ListTile(
                  leading: Icon(Icons.payment, color: cs.tertiary),
                  title: Text(p.description),
                  subtitle: Text('${p.method.label} • ${formatDate(p.date)}'),
                  trailing: Text(formatCurrency(p.amount),
                      style: TextStyle(color: cs.primary, fontWeight: FontWeight.bold)),
                  onTap: () => context.push('/payment-form?paymentId=${p.id}&patientId=${patient.id}'),
                ),
              );
            },
          ),
          // Prontuário
          _listOrEmpty(
            items: patientProntuario,
            empty: 'Nenhum registro',
            icon: Icons.description_outlined,
            fab: () => context.push('/prontuario-form?patientId=${patient.id}'),
            itemBuilder: (i) {
              final e = patientProntuario[i];
              return Card(
                margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                child: ListTile(
                  leading: Icon(Icons.article_outlined, color: cs.primary),
                  title: Text(e.text.length > 60 ? '${e.text.substring(0, 60)}...' : e.text),
                  subtitle: Text(formatDate(e.createdAt)),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => context.push('/prontuario-form?patientId=${patient.id}&entryId=${e.id}'),
                ),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _listOrEmpty({
    required List items,
    required String empty,
    required IconData icon,
    required VoidCallback fab,
    required Widget Function(int) itemBuilder,
  }) {
    return Stack(
      children: [
        items.isEmpty
            ? EmptyState(message: empty, icon: icon)
            : ListView.builder(
                padding: const EdgeInsets.symmetric(vertical: 8),
                itemCount: items.length,
                itemBuilder: (_, i) => itemBuilder(i),
              ),
        Positioned(
          bottom: 16, right: 16,
          child: FloatingActionButton(
            onPressed: fab,
            heroTag: null,
            child: const Icon(Icons.add),
          ),
        ),
      ],
    );
  }

  Future<void> _confirmDelete(BuildContext context, int id) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Excluir paciente'),
        content: const Text('Todos os dados do paciente serão removidos. Deseja continuar?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text('Excluir', style: TextStyle(color: Theme.of(context).colorScheme.error)),
          ),
        ],
      ),
    );
    if (confirm == true && mounted) {
      await ref.read(patientProvider.notifier).delete(id);
      if (mounted) Navigator.pop(context);
    }
  }

  Color _apptColor(AppointmentStatus s, ColorScheme cs) => switch (s) {
        AppointmentStatus.AGENDADA => cs.primary,
        AppointmentStatus.CONFIRMADA => cs.secondary,
        AppointmentStatus.CONCLUIDA => cs.tertiary,
        AppointmentStatus.CANCELADA => cs.error,
        AppointmentStatus.NAO_COMPARECEU => cs.errorContainer,
      };
}
