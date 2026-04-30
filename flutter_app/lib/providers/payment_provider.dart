import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/payment.dart';
import '../data/repositories/payment_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class PaymentState {
  final List<Payment> payments;
  final double totalReceived;
  final double monthReceived;
  final bool isLoading;
  final String? errorMessage;

  const PaymentState({
    this.payments = const [],
    this.totalReceived = 0.0,
    this.monthReceived = 0.0,
    this.isLoading = false,
    this.errorMessage,
  });

  PaymentState copyWith({
    List<Payment>? payments,
    double? totalReceived,
    double? monthReceived,
    bool? isLoading,
    String? errorMessage,
  }) {
    return PaymentState(
      payments: payments ?? this.payments,
      totalReceived: totalReceived ?? this.totalReceived,
      monthReceived: monthReceived ?? this.monthReceived,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class PaymentNotifier extends StateNotifier<PaymentState> {
  final PaymentRepository _repository;

  PaymentNotifier(this._repository) : super(const PaymentState()) {
    loadAll();
  }

  static int _monthStart() {
    final now = DateTime.now();
    return DateTime(now.year, now.month, 1).millisecondsSinceEpoch;
  }

  static int _monthEnd() {
    final now = DateTime.now();
    return DateTime(now.year, now.month + 1, 1).millisecondsSinceEpoch;
  }

  Future<void> loadAll() async {
    state = state.copyWith(isLoading: true);
    try {
      final payments = await _repository.getAll();
      final total = await _repository.getTotalReceived();
      final month =
          await _repository.getMonthReceived(_monthStart(), _monthEnd());
      state = PaymentState(
        payments: payments,
        totalReceived: total,
        monthReceived: month,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> insert(Payment payment) async {
    try {
      await _repository.insert(payment);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> update(Payment payment) async {
    try {
      await _repository.update(payment);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> delete(int id) async {
    try {
      await _repository.delete(id);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> _refresh() async {
    await _repository.refresh();
    final total = await _repository.getTotalReceived();
    final month =
        await _repository.getMonthReceived(_monthStart(), _monthEnd());
    state = state.copyWith(
      payments: _repository.cached,
      totalReceived: total,
      monthReceived: month,
      isLoading: false,
    );
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final paymentRepositoryProvider = Provider<PaymentRepository>(
  (ref) => PaymentRepository(),
);

final paymentProvider =
    StateNotifierProvider<PaymentNotifier, PaymentState>((ref) {
  final repository = ref.watch(paymentRepositoryProvider);
  return PaymentNotifier(repository);
});
