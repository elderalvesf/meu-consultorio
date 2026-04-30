import '../database/app_database.dart';
import '../models/payment.dart';
import '../services/firestore_sync.dart';

class PaymentRepository {
  final AppDatabase _db;
  final FirestoreSync _sync;

  // In-memory cache
  List<Payment> _cache = [];

  PaymentRepository({AppDatabase? db, FirestoreSync? sync})
      : _db = db ?? AppDatabase(),
        _sync = sync ?? FirestoreSync();

  // ─── Cache / Refresh ──────────────────────────────────────────────────────────

  List<Payment> get cached => List.unmodifiable(_cache);

  Future<void> refresh() async {
    _cache = await _db.getAllPayments();
  }

  // ─── Read ─────────────────────────────────────────────────────────────────────

  Future<List<Payment>> getAll() async {
    final result = await _db.getAllPayments();
    _cache = result;
    return result;
  }

  Future<Payment?> getById(int id) async {
    return _db.getPaymentById(id);
  }

  Future<List<Payment>> getByPatient(int patientId) async {
    return _db.getPaymentsByPatientId(patientId);
  }

  Future<double> getTotalReceived() async {
    return _db.getTotalReceived();
  }

  Future<double> getMonthReceived(int start, int end) async {
    return _db.getMonthReceived(start, end);
  }

  // ─── Write ────────────────────────────────────────────────────────────────────

  Future<int> insert(Payment payment) async {
    final id = await _db.insertPayment(payment);
    final saved = payment.copyWith(id: id);
    _sync.pushPayment(saved); // fire-and-forget
    return id;
  }

  Future<void> update(Payment payment) async {
    await _db.updatePayment(payment);
    _sync.pushPayment(payment); // fire-and-forget
  }

  Future<void> delete(int id) async {
    await _db.deletePayment(id);
    _sync.deletePayment(id); // fire-and-forget
  }
}
