import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/treatment.dart';
import '../../providers/treatment_provider.dart';
import '../../providers/patient_provider.dart';

class TreatmentFormScreen extends ConsumerStatefulWidget {
  final int? treatmentId;
  final int? preselectedPatientId;
  const TreatmentFormScreen({super.key, this.treatmentId, this.preselectedPatientId});

  @override
  ConsumerState<TreatmentFormScreen> createState() => _TreatmentFormScreenState();
}

class _TreatmentFormScreenState extends ConsumerState<TreatmentFormScreen> {
  int? _patientId;
  final _procedureCtrl = TextEditingController();
  final _toothCtrl = TextEditingController();
  final _descCtrl = TextEditingController();
  final _costCtrl = TextEditingController();
  TreatmentStatus _status = TreatmentStatus.EM_ANDAMENTO;
  DateTime _date = DateTime.now();
  bool _isSaving = false;
  Treatment? _existing;

  @override
  void initState() {
    super.initState();
    _patientId = widget.preselectedPatientId;
    Future.microtask(() {
      ref.read(patientProvider.notifier).loadAll();
      if (widget.treatmentId != null) _load();
    });
  }

  void _load() {
    final all = ref.read(treatmentProvider).treatments;
    _existing = all.where((t) => t.id == widget.treatmentId).firstOrNull;
    if (_existing != null) {
      setState(() {
        _patientId = _existing!.patientId;
        _procedureCtrl.text = _existing!.procedure;
        _toothCtrl.text = _existing!.tooth;
        _descCtrl.text = _existing!.description;
        _costCtrl.text = _existing!.cost.toStringAsFixed(2);
        _status = _existing!.status;
        _date = DateTime.fromMillisecondsSinceEpoch(_existing!.date);
      });
    }
  }

  @override
  void dispose() {
    _procedureCtrl.dispose(); _toothCtrl.dispose();
    _descCtrl.dispose(); _costCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context, initialDate: _date,
      firstDate: DateTime(2020), lastDate: DateTime(2030),
    );
    if (picked != null) setState(() => _date = picked);
  }

  Future<void> _save() async {
    if (_patientId == null || _procedureCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Preencha paciente e procedimento')));
      return;
    }
    setState(() => _isSaving = true);
    final treatment = Treatment(
      id: _existing?.id ?? 0,
      patientId: _patientId!,
      procedure: _procedureCtrl.text.trim(),
      tooth: _toothCtrl.text.trim(),
      description: _descCtrl.text.trim(),
      cost: double.tryParse(_costCtrl.text.replaceAll(',', '.')) ?? 0.0,
      date: _date.millisecondsSinceEpoch,
      status: _status,
    );
    if (_existing == null) await ref.read(treatmentProvider.notifier).insert(treatment);
    else await ref.read(treatmentProvider.notifier).update(treatment);
    if (mounted) { setState(() => _isSaving = false); Navigator.pop(context); }
  }

  @override
  Widget build(BuildContext context) {
    final patients = ref.watch(patientProvider).patients..sort((a, b) => a.name.compareTo(b.name));
    final cs = Theme.of(context).colorScheme;
    final dateStr = '${_date.day.toString().padLeft(2,'0')}/${_date.month.toString().padLeft(2,'0')}/${_date.year}';

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.treatmentId == null ? 'Novo Tratamento' : 'Editar Tratamento'),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isSaving ? null : _save,
        icon: const Icon(Icons.save),
        label: const Text('Salvar'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            DropdownButtonFormField<int>(
              value: _patientId,
              decoration: const InputDecoration(labelText: 'Paciente *'),
              items: patients.map((p) => DropdownMenuItem(value: p.id, child: Text(p.name))).toList(),
              onChanged: (v) => setState(() => _patientId = v),
            ),
            const SizedBox(height: 12),
            TextField(controller: _procedureCtrl, decoration: const InputDecoration(labelText: 'Procedimento *', prefixIcon: Icon(Icons.medical_services_outlined))),
            const SizedBox(height: 12),
            TextField(controller: _toothCtrl, decoration: const InputDecoration(labelText: 'Dente / Região', prefixIcon: Icon(Icons.pin_outlined))),
            const SizedBox(height: 12),
            TextField(controller: _descCtrl, decoration: const InputDecoration(labelText: 'Descrição'), maxLines: 3),
            const SizedBox(height: 12),
            TextField(
              controller: _costCtrl,
              decoration: const InputDecoration(labelText: 'Custo (R\$)', prefixIcon: Icon(Icons.attach_money)),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<TreatmentStatus>(
              value: _status,
              decoration: const InputDecoration(labelText: 'Status'),
              items: TreatmentStatus.values.map((s) => DropdownMenuItem(value: s, child: Text(s.label))).toList(),
              onChanged: (v) => setState(() => _status = v!),
            ),
            const SizedBox(height: 12),
            InkWell(
              onTap: _pickDate,
              child: InputDecorator(
                decoration: const InputDecoration(labelText: 'Data', suffixIcon: Icon(Icons.calendar_month)),
                child: Text(dateStr),
              ),
            ),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }
}
