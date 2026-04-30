import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/patient.dart';
import '../data/repositories/patient_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class PatientState {
  final List<Patient> patients;
  final bool isLoading;
  final String? errorMessage;

  const PatientState({
    this.patients = const [],
    this.isLoading = false,
    this.errorMessage,
  });

  int get total => patients.length;

  PatientState copyWith({
    List<Patient>? patients,
    bool? isLoading,
    String? errorMessage,
  }) {
    return PatientState(
      patients: patients ?? this.patients,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class PatientNotifier extends StateNotifier<PatientState> {
  final PatientRepository _repository;

  PatientNotifier(this._repository) : super(const PatientState()) {
    loadAll();
  }

  Future<void> loadAll() async {
    state = state.copyWith(isLoading: true);
    try {
      final patients = await _repository.getAll();
      state = PatientState(patients: patients, isLoading: false);
    } catch (e) {
      state = PatientState(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  Future<void> insert(Patient patient) async {
    try {
      await _repository.insert(patient);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> update(Patient patient) async {
    try {
      await _repository.update(patient);
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

  Future<int> count() async {
    return _repository.count();
  }

  Future<void> _refresh() async {
    await _repository.refresh();
    state = state.copyWith(patients: _repository.cached, isLoading: false);
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final patientRepositoryProvider = Provider<PatientRepository>(
  (ref) => PatientRepository(),
);

final patientProvider =
    StateNotifierProvider<PatientNotifier, PatientState>((ref) {
  final repository = ref.watch(patientRepositoryProvider);
  return PatientNotifier(repository);
});
