import '../database/app_database.dart';
import '../models/patient.dart';
import '../services/firestore_sync.dart';

class PatientRepository {
  final AppDatabase _db;
  final FirestoreSync _sync;

  // In-memory cache
  List<Patient> _cache = [];

  PatientRepository({AppDatabase? db, FirestoreSync? sync})
      : _db = db ?? AppDatabase(),
        _sync = sync ?? FirestoreSync();

  // ─── Cache / Refresh ──────────────────────────────────────────────────────────

  List<Patient> get cached => List.unmodifiable(_cache);

  Future<void> refresh() async {
    _cache = await _db.getAllPatients();
  }

  // ─── Read ─────────────────────────────────────────────────────────────────────

  Future<List<Patient>> getAll() async {
    final result = await _db.getAllPatients();
    _cache = result;
    return result;
  }

  Future<Patient?> getById(int id) async {
    return _db.getPatientById(id);
  }

  Future<int> count() async {
    return _db.countPatients();
  }

  // ─── Write ────────────────────────────────────────────────────────────────────

  Future<int> insert(Patient patient) async {
    final id = await _db.insertPatient(patient);
    final saved = patient.copyWith(id: id);
    _sync.pushPatient(saved); // fire-and-forget
    return id;
  }

  Future<void> update(Patient patient) async {
    await _db.updatePatient(patient);
    _sync.pushPatient(patient); // fire-and-forget
  }

  Future<void> delete(int id) async {
    await _db.deletePatient(id);
    _sync.deletePatient(id); // fire-and-forget
  }
}
