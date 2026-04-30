import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/prontuario_entry.dart';
import '../data/repositories/prontuario_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class ProntuarioState {
  final List<ProntuarioEntry> entries;
  final List<ProntuarioEntry> patientEntries;
  final bool isLoading;
  final bool isUploading;
  final String? errorMessage;

  const ProntuarioState({
    this.entries = const [],
    this.patientEntries = const [],
    this.isLoading = false,
    this.isUploading = false,
    this.errorMessage,
  });

  ProntuarioState copyWith({
    List<ProntuarioEntry>? entries,
    List<ProntuarioEntry>? patientEntries,
    bool? isLoading,
    bool? isUploading,
    String? errorMessage,
  }) {
    return ProntuarioState(
      entries: entries ?? this.entries,
      patientEntries: patientEntries ?? this.patientEntries,
      isLoading: isLoading ?? this.isLoading,
      isUploading: isUploading ?? this.isUploading,
      errorMessage: errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class ProntuarioNotifier extends StateNotifier<ProntuarioState> {
  final ProntuarioRepository _repository;

  ProntuarioNotifier(this._repository) : super(const ProntuarioState()) {
    loadAll();
  }

  Future<void> loadAll() async {
    state = state.copyWith(isLoading: true);
    try {
      final entries = await _repository.getAll();
      state = ProntuarioState(entries: entries, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> loadByPatient(int patientId) async {
    try {
      final list = await _repository.getByPatient(patientId);
      state = state.copyWith(patientEntries: list);
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> insert(ProntuarioEntry entry) async {
    try {
      await _repository.insert(entry);
      await _refresh();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> update(ProntuarioEntry entry) async {
    try {
      await _repository.update(entry);
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

  /// Uploads a local image for the given [entryId] to Firebase Storage and
  /// updates the entry record with the resulting download URL.
  Future<String?> uploadImage(int entryId, String localPath) async {
    state = state.copyWith(isUploading: true);
    try {
      final url = await _repository.uploadImage(entryId, localPath);
      await _refresh();
      state = state.copyWith(isUploading: false);
      return url;
    } catch (e) {
      state = state.copyWith(isUploading: false, errorMessage: e.toString());
      return null;
    }
  }

  Future<void> _refresh() async {
    await _repository.refresh();
    state = state.copyWith(
      entries: _repository.cached,
      isLoading: false,
    );
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final prontuarioRepositoryProvider = Provider<ProntuarioRepository>(
  (ref) => ProntuarioRepository(),
);

final prontuarioProvider =
    StateNotifierProvider<ProntuarioNotifier, ProntuarioState>((ref) {
  final repository = ref.watch(prontuarioRepositoryProvider);
  return ProntuarioNotifier(repository);
});
