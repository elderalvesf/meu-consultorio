import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/payment.dart';
import '../../providers/payment_provider.dart';
import '../../providers/patient_provider.dart';

class PaymentFormScreen extends ConsumerStatefulWidget {
  final int? paymentId;
  final int? preselectedPatientId;
  const PaymentFormScreen({super.key, this.paymentId, this.preselectedPatientId});

  @override
  ConsumerState<PaymentFormScreen> createState() => _PaymentFormScreenState();
}

class _PaymentFormScreenState extends ConsumerState<PaymentFormScreen> {
  int? _patientId;
  final _descCtrl = TextEditingController();
  final _amountCtrl = TextEditingController();
  final _notesCtrl = TextEditingController();
  PaymentMethod _method = PaymentMethod.PIX;
  bool _isPaid = true;
  DateTime _date = DateTime.now();
  bool _isSaving = false;
  Payment? _existing;

  @override
  void initState() {
    super.initState();
    _patientId = widget.preselectedPatientId;
    Future.microtask(() {
      ref.read(patientProvider.notifier).loadAll();
      if (widget.paymentId != null) _load();
    });
  }

  void _load() {
    final all = ref.read(paymentProvider).payments;
    _existing = all.where((p) => p.id == widget.paymentId).firstOrNull;
    if (_existing != null) {
      setState(() {
        _patientId = _existing!.patientId;
        _descCtrl.text = _existing!.description;
        _amountCtrl.text = _existing!.amount.toStringAsFixed(2);
        _notesCtrl.text = _existing!.notes;
        _method = _existing!.method;
        _isPaid = _existing!.isPaid;
        _date = DateTime.fromMillisecondsSinceEpoch(_existing!.date);
      });
    }
  }

  @override
  void dispose() {
    _descCtrl.dispose(); _amountCtrl.dispose(); _notesCtrl.dispose();
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
    if (_patientId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Selecione um paciente')));
      return;
    }
    if (_descCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Informe a descrição')));
      return;
    }
    final amount = double.tryParse(_amountCtrl.text.replaceAll(',', '.'));
    if (amount == null || amount <= 0) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Informe um valor válido')));
      return;
    }
    setState(() => _isSaving = true);
    final payment = Payment(
      id: _existing?.id ?? 0,
      patientId: _patientId!,
      description: _descCtrl.text.trim(),
      amount: amount,
      method: _method,
      date: _date.millisecondsSinceEpoch,
      notes: _notesCtrl.text.trim(),
      isPaid: _isPaid,
    );
    if (_existing == null) await ref.read(paymentProvider.notifier).insert(payment);
    else await ref.read(paymentProvider.notifier).update(payment);
    if (mounted) { setState(() => _isSaving = false); Navigator.pop(context); }
  }

  @override
  Widget build(BuildContext context) {
    final patients = ref.watch(patientProvider).patients..sort((a, b) => a.name.compareTo(b.name));
    final cs = Theme.of(context).colorScheme;
    final dateStr = '${_date.day.toString().padLeft(2,'0')}/${_date.month.toString().padLeft(2,'0')}/${_date.year}';

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.paymentId == null ? 'Novo Pagamento' : 'Editar Pagamento'),
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
            TextField(controller: _descCtrl, decoration: const InputDecoration(labelText: 'Descrição *', prefixIcon: Icon(Icons.description_outlined))),
            const SizedBox(height: 12),
            TextField(
              controller: _amountCtrl,
              decoration: const InputDecoration(labelText: 'Valor (R\$) *', prefixIcon: Icon(Icons.attach_money)),
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<PaymentMethod>(
              value: _method,
              decoration: const InputDecoration(labelText: 'Forma de pagamento'),
              items: PaymentMethod.values.map((m) => DropdownMenuItem(value: m, child: Text(m.label))).toList(),
              onChanged: (v) => setState(() => _method = v!),
            ),
            const SizedBox(height: 12),
            InkWell(
              onTap: _pickDate,
              child: InputDecorator(
                decoration: const InputDecoration(labelText: 'Data', suffixIcon: Icon(Icons.calendar_month)),
                child: Text(dateStr),
              ),
            ),
            const SizedBox(height: 12),
            SwitchListTile(
              title: const Text('Pago'),
              subtitle: Text(_isPaid ? 'Pagamento confirmado' : 'Pendente'),
              value: _isPaid,
              onChanged: (v) => setState(() => _isPaid = v),
              contentPadding: EdgeInsets.zero,
            ),
            TextField(
              controller: _notesCtrl,
              decoration: const InputDecoration(labelText: 'Observações'),
              maxLines: 3,
            ),
            const SizedBox(height: 80),
          ],
        ),
      ),
    );
  }
}
