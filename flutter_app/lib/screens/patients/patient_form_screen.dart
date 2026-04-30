import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/patient.dart';
import '../../providers/patient_provider.dart';

class PatientFormScreen extends ConsumerStatefulWidget {
  final int? patientId;
  const PatientFormScreen({super.key, this.patientId});

  @override
  ConsumerState<PatientFormScreen> createState() => _PatientFormScreenState();
}

class _PatientFormScreenState extends ConsumerState<PatientFormScreen> {
  final _nameCtrl = TextEditingController();
  final _cpfCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _birthCtrl = TextEditingController();
  final _addressCtrl = TextEditingController();
  final _notesCtrl = TextEditingController();
  bool _isLoading = false;
  Patient? _existing;

  @override
  void initState() {
    super.initState();
    if (widget.patientId != null) _loadPatient();
  }

  Future<void> _loadPatient() async {
    final patients = ref.read(patientProvider).patients;
    _existing = patients.where((p) => p.id == widget.patientId).firstOrNull;
    if (_existing != null) {
      _nameCtrl.text = _existing!.name;
      _cpfCtrl.text = _existing!.cpf;
      _phoneCtrl.text = _existing!.phone;
      _emailCtrl.text = _existing!.email;
      _birthCtrl.text = _existing!.birthDate;
      _addressCtrl.text = _existing!.address;
      _notesCtrl.text = _existing!.notes;
      setState(() {});
    }
  }

  @override
  void dispose() {
    for (final c in [_nameCtrl, _cpfCtrl, _phoneCtrl, _emailCtrl, _birthCtrl, _addressCtrl, _notesCtrl]) c.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_nameCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Informe o nome do paciente')));
      return;
    }
    setState(() => _isLoading = true);
    final patient = Patient(
      id: _existing?.id ?? 0,
      name: _nameCtrl.text.trim(),
      cpf: _cpfCtrl.text.trim(),
      phone: _phoneCtrl.text.trim(),
      email: _emailCtrl.text.trim(),
      birthDate: _birthCtrl.text.trim(),
      address: _addressCtrl.text.trim(),
      notes: _notesCtrl.text.trim(),
      createdAt: _existing?.createdAt ?? DateTime.now().millisecondsSinceEpoch,
    );
    if (_existing == null) {
      await ref.read(patientProvider.notifier).insert(patient);
    } else {
      await ref.read(patientProvider.notifier).update(patient);
    }
    if (mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.patientId == null ? 'Novo Paciente' : 'Editar Paciente'),
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Theme.of(context).colorScheme.onPrimary,
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isLoading ? null : _save,
        icon: _isLoading
            ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.save),
        label: const Text('Salvar'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _field(_nameCtrl, 'Nome *', icon: Icons.person_outline),
            _field(_phoneCtrl, 'Telefone', icon: Icons.phone_outlined, type: TextInputType.phone),
            _field(_cpfCtrl, 'CPF', icon: Icons.badge_outlined),
            _field(_emailCtrl, 'Email', icon: Icons.email_outlined, type: TextInputType.emailAddress),
            _field(_birthCtrl, 'Data de nascimento', icon: Icons.cake_outlined, hint: 'dd/mm/aaaa'),
            _field(_addressCtrl, 'Endereço', icon: Icons.location_on_outlined),
            _field(_notesCtrl, 'Observações', icon: Icons.notes_outlined, maxLines: 4),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }

  Widget _field(TextEditingController ctrl, String label, {
    IconData? icon, TextInputType? type, String? hint, int maxLines = 1,
  }) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: TextField(
        controller: ctrl,
        keyboardType: type,
        maxLines: maxLines,
        decoration: InputDecoration(
          labelText: label,
          hintText: hint,
          prefixIcon: icon != null ? Icon(icon) : null,
        ),
      ),
    );
  }
}
