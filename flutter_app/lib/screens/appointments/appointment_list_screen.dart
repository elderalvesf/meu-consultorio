import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/appointment_provider.dart';
import '../../providers/patient_provider.dart';
import '../../data/models/appointment.dart';
import '../../widgets/common_widgets.dart';

class AppointmentListScreen extends ConsumerStatefulWidget {
  const AppointmentListScreen({super.key});

  @override
  ConsumerState<AppointmentListScreen> createState() => _AppointmentListScreenState();
}

class _AppointmentListScreenState extends ConsumerState<AppointmentListScreen> {
  AppointmentStatus? _filterStatus;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(appointmentProvider.notifier).loadAll();
      ref.read(patientProvider.notifier).loadAll();
    });
  }

  Future<void> _pickDate(BuildContext context) async {
    final state = ref.read(appointmentProvider);
    final current = DateTime.fromMillisecondsSinceEpoch(state.selectedDate);
    final picked = await showDatePicker(
      context: context,
      initialDate: current,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked != null) {
      ref.read(appointmentProvider.notifier).selectDate(picked.millisecondsSinceEpoch);
    }
  }

  @override
  Widget build(BuildContext context) {
    final apptState = ref.watch(appointmentProvider);
    final patientState = ref.watch(patientProvider);
    final patientMap = {for (final p in patientState.patients) p.id: p};
    final cs = Theme.of(context).colorScheme;

    final selectedDate = DateTime.fromMillisecondsSinceEpoch(apptState.selectedDate);
    final dayStart = DateTime(selectedDate.year, selectedDate.month, selectedDate.day);
    final dayEnd = dayStart.add(const Duration(days: 1));

    var displayed = apptState.allAppointments.where((a) {
      final dt = DateTime.fromMillisecondsSinceEpoch(a.dateTime);
      return dt.isAfter(dayStart) && dt.isBefore(dayEnd);
    }).toList()
      ..sort((a, b) => a.dateTime.compareTo(b.dateTime));

    if (_filterStatus != null) displayed = displayed.where((a) => a.status == _filterStatus).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Agenda'),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.push('/appointment-form'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          // Seletor de data
          Card(
            margin: const EdgeInsets.all(16),
            color: cs.primaryContainer,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Data selecionada', style: TextStyle(color: cs.onPrimaryContainer.withOpacity(0.7), fontSize: 12)),
                        Text(formatDate(apptState.selectedDate),
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                        Text('${displayed.length} consulta(s)', style: Theme.of(context).textTheme.bodySmall),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: Icon(Icons.calendar_month, color: cs.primary),
                    onPressed: () => _pickDate(context),
                  ),
                ],
              ),
            ),
          ),
          // Filtro de status
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                _FilterChip(label: 'Todas', selected: _filterStatus == null, onTap: () => setState(() => _filterStatus = null)),
                ...AppointmentStatus.values.map((s) => _FilterChip(
                  label: s.label,
                  selected: _filterStatus == s,
                  onTap: () => setState(() => _filterStatus = _filterStatus == s ? null : s),
                )),
              ],
            ),
          ),
          const SizedBox(height: 8),
          // Lista
          Expanded(
            child: displayed.isEmpty
                ? EmptyState(message: 'Nenhuma consulta para esta data', icon: Icons.calendar_today_outlined)
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
                    itemCount: displayed.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, i) {
                      final a = displayed[i];
                      final patientName = patientMap[a.patientId]?.name ?? 'Paciente desconhecido';
                      return _AppointmentCard(
                        appointment: a,
                        patientName: patientName,
                        onEdit: () => context.push('/appointment-form?appointmentId=${a.id}'),
                        onPatientTap: patientMap[a.patientId] != null
                            ? () => context.push('/patients/${a.patientId}')
                            : null,
                        onStatusChange: (s) => ref.read(appointmentProvider.notifier).updateStatus(a, s),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChip({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: FilterChip(
        label: Text(label),
        selected: selected,
        onSelected: (_) => onTap(),
        selectedColor: cs.primaryContainer,
      ),
    );
  }
}

class _AppointmentCard extends StatelessWidget {
  final Appointment appointment;
  final String patientName;
  final VoidCallback onEdit;
  final VoidCallback? onPatientTap;
  final ValueChanged<AppointmentStatus> onStatusChange;

  const _AppointmentCard({
    required this.appointment,
    required this.patientName,
    required this.onEdit,
    required this.onStatusChange,
    this.onPatientTap,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      GestureDetector(
                        onTap: onPatientTap,
                        child: Text(patientName,
                            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                  color: onPatientTap != null ? cs.primary : null)),
                      ),
                      Text(appointment.procedureType, style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: cs.onSurface.withOpacity(0.6))),
                    ],
                  ),
                ),
                _StatusDropdown(
                  status: appointment.status,
                  onChanged: onStatusChange,
                  cs: cs,
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.access_time, size: 14, color: cs.onSurface.withOpacity(0.5)),
                const SizedBox(width: 4),
                Text(formatTime(appointment.dateTime), style: Theme.of(context).textTheme.bodySmall),
                const SizedBox(width: 12),
                Icon(Icons.schedule, size: 14, color: cs.onSurface.withOpacity(0.5)),
                const SizedBox(width: 4),
                Text('${appointment.durationMinutes} min', style: Theme.of(context).textTheme.bodySmall),
                const Spacer(),
                IconButton(
                  onPressed: onEdit,
                  icon: const Icon(Icons.edit, size: 16),
                  style: IconButton.styleFrom(tapTargetSize: MaterialTapTargetSize.shrinkWrap),
                ),
              ],
            ),
            if (appointment.notes.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(appointment.notes, style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: cs.onSurface.withOpacity(0.6))),
              ),
            if (appointment.calendarEventId > 0)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Row(
                  children: [
                    Icon(Icons.calendar_month, size: 12, color: cs.primary),
                    const SizedBox(width: 4),
                    Text('Google Calendar', style: TextStyle(color: cs.primary, fontSize: 11)),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _StatusDropdown extends StatefulWidget {
  final AppointmentStatus status;
  final ValueChanged<AppointmentStatus> onChanged;
  final ColorScheme cs;

  const _StatusDropdown({required this.status, required this.onChanged, required this.cs});

  @override
  State<_StatusDropdown> createState() => _StatusDropdownState();
}

class _StatusDropdownState extends State<_StatusDropdown> {
  bool _open = false;

  Color _color(AppointmentStatus s) => switch (s) {
        AppointmentStatus.AGENDADA => widget.cs.primary,
        AppointmentStatus.CONFIRMADA => widget.cs.secondary,
        AppointmentStatus.CONCLUIDA => widget.cs.tertiary,
        AppointmentStatus.CANCELADA => widget.cs.error,
        AppointmentStatus.NAO_COMPARECEU => widget.cs.errorContainer,
      };

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<AppointmentStatus>(
      initialValue: widget.status,
      onSelected: widget.onChanged,
      child: StatusChip(label: widget.status.label, color: _color(widget.status)),
      itemBuilder: (_) => AppointmentStatus.values
          .map((s) => PopupMenuItem(value: s, child: Text(s.label)))
          .toList(),
    );
  }
}
