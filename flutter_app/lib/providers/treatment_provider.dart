import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/treatment.dart';
import '../data/repositories/treatment_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class TreatmentState {
  final List<Treatment> treatments;
  final List<Treatment> patientTreatments;
  final bool isLoading;
  final String? errorMessage;

  const TreatmentState({
    this.treatments = const [],
    this.patientTreatments = const [],
    this.isLoading = false,
    this.errorMessage,
  });

  TreatmentState copyWith({
    List<Treatment>? treatments,
    List<Treatment>? patientTreatments,
    bool? isLoading,
    String? errorMessage,
  }) {
    return TreatmentState(
      treatments: treatments ?? this.treatments,
      patientTreatments: patientTreatments ?? this.patientTreatments,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class TreatmentNotifier extends StateNotifier<TreatmentState> {
  final TreatmentRepository _repository;

  TreatmentNotifier(this._repository) : super(const TreatmentState()) {
    loadAll();
  }

  Future<void> loadAll() async {
    state = state.copyWith(isLoading: true);
    try {
      final treatments = await _repository.getAll();
      state = TreatmentState(treatments: treatments, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> loadByPatient(int patientId) async {
    try {
      final list = await _repository.getByPatient(patientId);
      state = state.copyWith(patientTreatments: list);
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> insert(Treatment treatment) async {
    try {
      await _repository.insert(treatment);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> update(Treatment treatment) async {
    try {
      await _repository.update(treatment);
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
    state = state.copyWith(
      treatments: _repository.cached,
      isLoading: false,
    );
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final treatmentRepositoryProvider = Provider<TreatmentRepository>(
  (ref) => TreatmentRepository(),
);

final treatmentProvider =
    StateNotifierProvider<TreatmentNotifier, TreatmentState>((ref) {
  final repository = ref.watch(treatmentRepositoryProvider);
  return TreatmentNotifier(repository);
});
