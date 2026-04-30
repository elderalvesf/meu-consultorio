import '../database/app_database.dart';
import '../models/prontuario_entry.dart';
import '../services/firestore_sync.dart';

class ProntuarioRepository {
  final AppDatabase _db;
  final FirestoreSync _sync;

  // In-memory cache
  List<ProntuarioEntry> _cache = [];

  ProntuarioRepository({AppDatabase? db, FirestoreSync? sync})
      : _db = db ?? AppDatabase(),
        _sync = sync ?? FirestoreSync();

  // ─── Cache / Refresh ──────────────────────────────────────────────────────────

  List<ProntuarioEntry> get cached => List.unmodifiable(_cache);

  Future<void> refresh() async {
    _cache = await _db.getAllProntuarioEntries();
  }

  // ─── Read ─────────────────────────────────────────────────────────────────────

  Future<List<ProntuarioEntry>> getAll() async {
    final result = await _db.getAllProntuarioEntries();
    _cache = result;
    return result;
  }

  Future<ProntuarioEntry?> getById(int id) async {
    return _db.getProntuarioEntryById(id);
  }

  Future<List<ProntuarioEntry>> getByPatient(int patientId) async {
    return _db.getProntuarioEntriesByPatientId(patientId);
  }

  // ─── Write ────────────────────────────────────────────────────────────────────

  Future<int> insert(ProntuarioEntry entry) async {
    final id = await _db.insertProntuarioEntry(entry);
    final saved = entry.copyWith(id: id);
    _sync.pushProntuarioEntry(saved); // fire-and-forget
    return id;
  }

  Future<void> update(ProntuarioEntry entry) async {
    await _db.updateProntuarioEntry(entry);
    _sync.pushProntuarioEntry(entry); // fire-and-forget
  }

  Future<void> delete(int id) async {
    await _db.deleteProntuarioEntry(id);
    _sync.deleteProntuarioEntry(id); // fire-and-forget
  }

  /// Uploads the image at [localPath] to Firebase Storage and updates the
  /// entry's [imageUrl] both in the local DB and Firestore.
  Future<String?> uploadImage(int entryId, String localPath) async {
    final url = await _sync.uploadProntuarioImage(entryId, localPath);
    if (url != null) {
      final entry = await _db.getProntuarioEntryById(entryId);
      if (entry != null) {
        final updated = entry.copyWith(imagePath: localPath, imageUrl: url);
        await _db.updateProntuarioEntry(updated);
        _sync.pushProntuarioEntry(updated); // fire-and-forget
      }
    }
    return url;
  }
}
