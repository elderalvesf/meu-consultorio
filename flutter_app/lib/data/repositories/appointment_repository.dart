import '../database/app_database.dart';
import '../models/appointment.dart';
import '../services/firestore_sync.dart';

class AppointmentRepository {
  final AppDatabase _db;
  final FirestoreSync _sync;

  // In-memory cache
  List<Appointment> _cache = [];

  AppointmentRepository({AppDatabase? db, FirestoreSync? sync})
      : _db = db ?? AppDatabase(),
        _sync = sync ?? FirestoreSync();

  // ─── Cache / Refresh ──────────────────────────────────────────────────────────

  List<Appointment> get cached => List.unmodifiable(_cache);

  Future<void> refresh() async {
    _cache = await _db.getAllAppointments();
  }

  // ─── Read ─────────────────────────────────────────────────────────────────────

  Future<List<Appointment>> getAll() async {
    final result = await _db.getAllAppointments();
    _cache = result;
    return result;
  }

  Future<Appointment?> getById(int id) async {
    return _db.getAppointmentById(id);
  }

  Future<List<Appointment>> getByPatient(int patientId) async {
    return _db.getAppointmentsByPatientId(patientId);
  }

  Future<List<Appointment>> getByDay(int start, int end) async {
    return _db.getAppointmentsByDay(start, end);
  }

  Future<List<Appointment>> getByRange(int start, int end) async {
    return _db.getAppointmentsByRange(start, end);
  }

  Future<int> countToday(int start, int end) async {
    return _db.countTodayAppointments(start, end);
  }

  // ─── Write ────────────────────────────────────────────────────────────────────

  Future<int> insert(Appointment appointment) async {
    final id = await _db.insertAppointment(appointment);
    final saved = appointment.copyWith(id: id);
    _sync.pushAppointment(saved); // fire-and-forget
    return id;
  }

  Future<void> update(Appointment appointment) async {
    await _db.updateAppointment(appointment);
    _sync.pushAppointment(appointment); // fire-and-forget
  }

  Future<void> delete(int id) async {
    await _db.deleteAppointment(id);
    _sync.deleteAppointment(id); // fire-and-forget
  }
}
