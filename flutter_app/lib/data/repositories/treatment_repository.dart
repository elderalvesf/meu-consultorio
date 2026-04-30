import '../database/app_database.dart';
import '../models/treatment.dart';
import '../services/firestore_sync.dart';

class TreatmentRepository {
  final AppDatabase _db;
  final FirestoreSync _sync;

  // In-memory cache
  List<Treatment> _cache = [];

  TreatmentRepository({AppDatabase? db, FirestoreSync? sync})
      : _db = db ?? AppDatabase(),
        _sync = sync ?? FirestoreSync();

  // ─── Cache / Refresh ──────────────────────────────────────────────────────────

  List<Treatment> get cached => List.unmodifiable(_cache);

  Future<void> refresh() async {
    _cache = await _db.getAllTreatments();
  }

  // ─── Read ─────────────────────────────────────────────────────────────────────

  Future<List<Treatment>> getAll() async {
    final result = await _db.getAllTreatments();
    _cache = result;
    return result;
  }

  Future<Treatment?> getById(int id) async {
    return _db.getTreatmentById(id);
  }

  Future<List<Treatment>> getByPatient(int patientId) async {
    return _db.getTreatmentsByPatientId(patientId);
  }

  // ─── Write ────────────────────────────────────────────────────────────────────

  Future<int> insert(Treatment treatment) async {
    final id = await _db.insertTreatment(treatment);
    final saved = treatment.copyWith(id: id);
    _sync.pushTreatment(saved); // fire-and-forget
    return id;
  }

  Future<void> update(Treatment treatment) async {
    await _db.updateTreatment(treatment);
    _sync.pushTreatment(treatment); // fire-and-forget
  }

  Future<void> delete(int id) async {
    await _db.deleteTreatment(id);
    _sync.deleteTreatment(id); // fire-and-forget
  }
}
