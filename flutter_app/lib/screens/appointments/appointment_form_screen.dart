import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/appointment.dart';
import '../../providers/appointment_provider.dart';
import '../../providers/patient_provider.dart';
import '../../data/services/calendar_service.dart';

const _procedures = [
  'Consulta de avaliação', 'Limpeza e profilaxia', 'Clareamento dental',
  'Restauração (resina)', 'Restauração (amálgama)', 'Extração simples',
  'Extração de siso', 'Canal (endodontia)', 'Prótese dentária',
  'Implante dentário', 'Ortodontia (aparelho)', 'Periodontia (gengiva)',
  'Radiografia', 'Urgência / Dor', 'Retorno', 'Outro',
];

class AppointmentFormScreen extends ConsumerStatefulWidget {
  final int? appointmentId;
  final int? preselectedPatientId;
  const AppointmentFormScreen({super.key, this.appointmentId, this.preselectedPatientId});

  @override
  ConsumerState<AppointmentFormScreen> createState() => _AppointmentFormScreenState();
}

class _AppointmentFormScreenState extends ConsumerState<AppointmentFormScreen> {
  int? _patientId;
  String _procedure = '';
  AppointmentStatus _status = AppointmentStatus.AGENDADA;
  int _duration = 60;
  String _notes = '';
  DateTime _dateTime = DateTime.now();
  int _calendarEventId = -1;
  bool _syncCalendar = false;
  bool _isSaving = false;
  bool _patientError = false;
  bool _procedureError = false;
  Appointment? _existing;

  @override
  void initState() {
    super.initState();
    _patientId = widget.preselectedPatientId;
    Future.microtask(() {
      ref.read(patientProvider.notifier).loadAll();
      if (widget.appointmentId != null) _loadAppointment();
    });
  }

  void _loadAppointment() {
    final all = ref.read(appointmentProvider).allAppointments;
    _existing = all.where((a) => a.id == widget.appointmentId).firstOrNull;
    if (_existing != null) {
      setState(() {
        _patientId = _existing!.patientId;
        _procedure = _existing!.procedureType;
        _status = _existing!.status;
        _duration = _existing!.durationMinutes;
        _notes = _existing!.notes;
        _dateTime = DateTime.fromMillisecondsSinceEpoch(_existing!.dateTime);
        _calendarEventId = _existing!.calendarEventId;
        _syncCalendar = _existing!.calendarEventId > 0;
      });
    }
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context, initialDate: _dateTime,
      firstDate: DateTime(2020), lastDate: DateTime(2030),
    );
    if (picked != null) setState(() => _dateTime = DateTime(picked.year, picked.month, picked.day, _dateTime.hour, _dateTime.minute));
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context, initialTime: TimeOfDay(hour: _dateTime.hour, minute: _dateTime.minute),
    );
    if (picked != null) setState(() => _dateTime = DateTime(_dateTime.year, _dateTime.month, _dateTime.day, picked.hour, picked.minute));
  }

  Future<void> _save() async {
    _patientError = _patientId == null;
    _procedureError = _procedure.isEmpty;
    setState(() {});
    if (_patientError || _procedureError) return;

    setState(() => _isSaving = true);
    final patientName = ref.read(patientProvider).patients.where((p) => p.id == _patientId).firstOrNull?.name ?? '';

    final appointment = Appointment(
      id: _existing?.id ?? 0,
      patientId: _patientId!,
      dateTime: _dateTime.millisecondsSinceEpoch,
      durationMinutes: _duration,
      procedureType: _procedure,
      status: _status,
      notes: _notes,
      createdAt: _existing?.createdAt ?? DateTime.now().millisecondsSinceEpoch,
      calendarEventId: _calendarEventId,
    );

    int savedId;
    if (_existing == null) {
      savedId = await ref.read(appointmentProvider.notifier).insert(appointment);
    } else {
      await ref.read(appointmentProvider.notifier).update(appointment);
      savedId = appointment.id;
    }

    // Sincronizar com calendário
    if (_syncCalendar && patientName.isNotEmpty) {
      final saved = appointment.copyWith(id: savedId);
      final success = await CalendarService.addEvent(saved, patientName);
      if (mounted) {
        final msg = success ? 'Consulta adicionada ao calendário!' : 'Não foi possível abrir o calendário.';
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
      }
    } else if (!_syncCalendar && _calendarEventId > 0) {
      // Remover sync (marca como -1 localmente)
      await ref.read(appointmentProvider.notifier).update(appointment.copyWith(id: savedId, calendarEventId: -1));
    }

    if (mounted) {
      setState(() => _isSaving = false);
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    final patients = ref.watch(patientProvider).patients;
    patients.sort((a, b) => a.name.compareTo(b.name));
    final cs = Theme.of(context).colorScheme;
    final dateStr = '${_dateTime.day.toString().padLeft(2,'0')}/${_dateTime.month.toString().padLeft(2,'0')}/${_dateTime.year}';
    final timeStr = '${_dateTime.hour.toString().padLeft(2,'0')}:${_dateTime.minute.toString().padLeft(2,'0')}';

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.appointmentId == null ? 'Nova Consulta' : 'Editar Consulta'),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isSaving ? null : _save,
        icon: _isSaving
            ? const SizedBox(w: 18, h: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.save),
        label: Text(_isSaving ? 'Salvando...' : 'Salvar'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // Paciente
            DropdownButtonFormField<int>(
              value: _patientId,
              decoration: InputDecoration(labelText: 'Paciente *', errorText: _patientError ? 'Selecione um paciente' : null),
              items: patients.map((p) => DropdownMenuItem(value: p.id, child: Text(p.name))).toList(),
              onChanged: (v) => setState(() { _patientId = v; _patientError = false; }),
            ),
            const SizedBox(height: 12),
            // Procedimento
            DropdownButtonFormField<String>(
              value: _procedures.contains(_procedure) ? _procedure : null,
              decoration: InputDecoration(labelText: 'Procedimento *', errorText: _procedureError ? 'Informe o procedimento' : null),
              items: _procedures.map((p) => DropdownMenuItem(value: p, child: Text(p))).toList(),
              onChanged: (v) => setState(() { _procedure = v ?? ''; _procedureError = false; }),
            ),
            const SizedBox(height: 12),
            // Data e hora
            Row(
              children: [
                Expanded(
                  child: InkWell(
                    onTap: _pickDate,
                    child: InputDecorator(
                      decoration: const InputDecoration(labelText: 'Data', suffixIcon: Icon(Icons.calendar_month)),
                      child: Text(dateStr),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: InkWell(
                    onTap: _pickTime,
                    child: InputDecorator(
                      decoration: const InputDecoration(labelText: 'Horário', suffixIcon: Icon(Icons.schedule)),
                      child: Text(timeStr),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            // Status
            DropdownButtonFormField<AppointmentStatus>(
              value: _status,
              decoration: const InputDecoration(labelText: 'Status'),
              items: AppointmentStatus.values.map((s) => DropdownMenuItem(value: s, child: Text(s.label))).toList(),
              onChanged: (v) => setState(() => _status = v!),
            ),
            const SizedBox(height: 12),
            // Duração
            TextField(
              controller: TextEditingController(text: '$_duration')..selection = TextSelection.collapsed(offset: '$_duration'.length),
              decoration: const InputDecoration(labelText: 'Duração (minutos)', suffixText: 'min'),
              keyboardType: TextInputType.number,
              onChanged: (v) { final n = int.tryParse(v); if (n != null && n >= 10 && n <= 480) _duration = n; },
            ),
            const SizedBox(height: 12),
            // Observações
            TextField(
              controller: TextEditingController(text: _notes),
              decoration: const InputDecoration(labelText: 'Observações'),
              maxLines: 3,
              onChanged: (v) => _notes = v,
            ),
            const SizedBox(height: 12),
            // Google Calendar toggle
            Card(
              color: _syncCalendar ? cs.primaryContainer : cs.surfaceVariant,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Row(
                  children: [
                    Icon(Icons.calendar_month, color: _syncCalendar ? cs.primary : cs.onSurfaceVariant),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Google Calendar', style: TextStyle(fontWeight: FontWeight.w600)),
                          Text(
                            _syncCalendar ? 'Será aberto o app de calendário ao salvar' : 'Não sincronizado',
                            style: TextStyle(fontSize: 12, color: cs.onSurfaceVariant),
                          ),
                        ],
                      ),
                    ),
                    Switch(value: _syncCalendar, onChanged: (v) => setState(() => _syncCalendar = v)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }
}

// Helper para SizedBox com w/h named params
extension on SizedBox {
  static SizedBox create({double? w, double? h, Widget? child}) =>
      SizedBox(width: w, height: h, child: child);
}
