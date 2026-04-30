import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../providers/payment_provider.dart';
import '../../providers/patient_provider.dart';
import '../../data/models/payment.dart';
import '../../widgets/common_widgets.dart';

class FinancialScreen extends ConsumerStatefulWidget {
  const FinancialScreen({super.key});

  @override
  ConsumerState<FinancialScreen> createState() => _FinancialScreenState();
}

class _FinancialScreenState extends ConsumerState<FinancialScreen> {
  PaymentMethod? _filterMethod;

  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(paymentProvider.notifier).loadAll();
      ref.read(patientProvider.notifier).loadAll();
    });
  }

  @override
  Widget build(BuildContext context) {
    final payState = ref.watch(paymentProvider);
    final patientState = ref.watch(patientProvider);
    final patientMap = {for (final p in patientState.patients) p.id: p};
    final cs = Theme.of(context).colorScheme;

    var payments = payState.payments;
    payments.sort((a, b) => b.date.compareTo(a.date));
    if (_filterMethod != null) payments = payments.where((p) => p.method == _filterMethod).toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Financeiro'),
        backgroundColor: cs.primary,
        foregroundColor: cs.onPrimary,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => context.push('/payment-form'),
        child: const Icon(Icons.add),
      ),
      body: Column(
        children: [
          // Cards de resumo
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(child: _SummaryCard(
                  label: 'Mês atual', value: formatCurrency(payState.monthReceived), color: cs.secondary)),
                const SizedBox(width: 12),
                Expanded(child: _SummaryCard(
                  label: 'Total recebido', value: formatCurrency(payState.totalReceived), color: cs.primary)),
              ],
            ),
          ),
          // Filtro de método
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                FilterChip(
                  label: const Text('Todos'),
                  selected: _filterMethod == null,
                  onSelected: (_) => setState(() => _filterMethod = null),
                ),
                const SizedBox(width: 8),
                ...PaymentMethod.values.map((m) => Padding(
                  padding: const EdgeInsets.only(left: 8),
                  child: FilterChip(
                    label: Text(m.label),
                    selected: _filterMethod == m,
                    onSelected: (_) => setState(() => _filterMethod = _filterMethod == m ? null : m),
                  ),
                )),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: payments.isEmpty
                ? EmptyState(message: 'Nenhum pagamento registrado', icon: Icons.payments_outlined)
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 0, 16, 88),
                    itemCount: payments.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, i) {
                      final p = payments[i];
                      final patientName = patientMap[p.patientId]?.name ?? 'Desconhecido';
                      return Card(
                        child: ListTile(
                          leading: CircleAvatar(
                            backgroundColor: p.isPaid ? cs.tertiaryContainer : cs.errorContainer,
                            child: Icon(
                              p.isPaid ? Icons.check : Icons.pending,
                              color: p.isPaid ? cs.onTertiaryContainer : cs.onErrorContainer,
                              size: 20,
                            ),
                          ),
                          title: Text(p.description, style: const TextStyle(fontWeight: FontWeight.w600)),
                          subtitle: Text('$patientName • ${p.method.label} • ${formatDate(p.date)}'),
                          trailing: Text(
                            formatCurrency(p.amount),
                            style: TextStyle(
                              color: p.isPaid ? cs.primary : cs.error,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          onTap: () => context.push('/payment-form?paymentId=${p.id}&patientId=${p.patientId}'),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _SummaryCard({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Card(
      color: color.withOpacity(0.12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: TextStyle(color: color.withOpacity(0.8), fontSize: 12)),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 18)),
          ],
        ),
      ),
    );
  }
}
