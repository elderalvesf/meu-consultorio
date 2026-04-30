import 'dart:io';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';

import '../database/app_database.dart';
import '../models/patient.dart';
import '../models/appointment.dart';
import '../models/treatment.dart';
import '../models/payment.dart';
import '../models/prontuario_entry.dart';

class FirestoreSync {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final FirebaseStorage _storage = FirebaseStorage.instance;
  final AppDatabase _db = AppDatabase();

  // ─── Helpers ──────────────────────────────────────────────────────────────────

  String? get _uid => _auth.currentUser?.uid;

  CollectionReference<Map<String, dynamic>> _col(String name) {
    final uid = _uid;
    if (uid == null) throw Exception('FirestoreSync: user not authenticated');
    return _firestore.collection('users').doc(uid).collection(name);
  }

  // ─── PATIENTS ────────────────────────────────────────────────────────────────

  Future<void> pushPatient(Patient patient) async {
    try {
      await _col('patients').doc(patient.id.toString()).set(patient.toMap());
    } catch (e) {
      // ignore sync errors – local DB is source of truth
    }
  }

  Future<void> deletePatient(int id) async {
    try {
      await _col('patients').doc(id.toString()).delete();
    } catch (e) {
      // ignore
    }
  }

  // ─── APPOINTMENTS ─────────────────────────────────────────────────────────────

  Future<void> pushAppointment(Appointment appointment) async {
    try {
      final map = appointment.toMap();
      // calendarEventId is local-only – do not push to Firestore
      map.remove('calendarEventId');
      await _col('appointments').doc(appointment.id.toString()).set(map);
    } catch (e) {
      // ignore
    }
  }

  Future<void> deleteAppointment(int id) async {
    try {
      await _col('appointments').doc(id.toString()).delete();
    } catch (e) {
      // ignore
    }
  }

  // ─── TREATMENTS ───────────────────────────────────────────────────────────────

  Future<void> pushTreatment(Treatment treatment) async {
    try {
      await _col('treatments').doc(treatment.id.toString()).set(treatment.toMap());
    } catch (e) {
      // ignore
    }
  }

  Future<void> deleteTreatment(int id) async {
    try {
      await _col('treatments').doc(id.toString()).delete();
    } catch (e) {
      // ignore
    }
  }

  // ─── PAYMENTS ─────────────────────────────────────────────────────────────────

  Future<void> pushPayment(Payment payment) async {
    try {
      await _col('payments').doc(payment.id.toString()).set(payment.toMap());
    } catch (e) {
      // ignore
    }
  }

  Future<void> deletePayment(int id) async {
    try {
      await _col('payments').doc(id.toString()).delete();
    } catch (e) {
      // ignore
    }
  }

  // ─── PRONTUARIO ENTRIES ───────────────────────────────────────────────────────

  Future<void> pushProntuarioEntry(ProntuarioEntry entry) async {
    try {
      await _col('prontuario_entries')
          .doc(entry.id.toString())
          .set(entry.toMap());
    } catch (e) {
      // ignore
    }
  }

  Future<void> deleteProntuarioEntry(int id) async {
    try {
      await _col('prontuario_entries').doc(id.toString()).delete();
    } catch (e) {
      // ignore
    }
  }

  // ─── PULL ALL ─────────────────────────────────────────────────────────────────

  /// Downloads all data from Firestore and upserts into the local SQLite database.
  Future<void> pullAll() async {
    if (_uid == null) return;

    try {
      await _pullPatients();
      await _pullAppointments();
      await _pullTreatments();
      await _pullPayments();
      await _pullProntuarioEntries();
    } catch (e) {
      // Sync errors should not crash the app
    }
  }

  Future<void> _pullPatients() async {
    final snapshot = await _col('patients').get();
    for (final doc in snapshot.docs) {
      try {
        final patient = Patient.fromMap(doc.data());
        await _db.insertPatient(patient);
      } catch (_) {}
    }
  }

  Future<void> _pullAppointments() async {
    final snapshot = await _col('appointments').get();
    for (final doc in snapshot.docs) {
      try {
        final data = doc.data();
        // calendarEventId not stored in Firestore – default to -1 on pull
        data.putIfAbsent('calendarEventId', () => -1);
        final appointment = Appointment.fromMap(data);
        await _db.insertAppointment(appointment);
      } catch (_) {}
    }
  }

  Future<void> _pullTreatments() async {
    final snapshot = await _col('treatments').get();
    for (final doc in snapshot.docs) {
      try {
        final treatment = Treatment.fromMap(doc.data());
        await _db.insertTreatment(treatment);
      } catch (_) {}
    }
  }

  Future<void> _pullPayments() async {
    final snapshot = await _col('payments').get();
    for (final doc in snapshot.docs) {
      try {
        final payment = Payment.fromMap(doc.data());
        await _db.insertPayment(payment);
      } catch (_) {}
    }
  }

  Future<void> _pullProntuarioEntries() async {
    final snapshot = await _col('prontuario_entries').get();
    for (final doc in snapshot.docs) {
      try {
        final entry = ProntuarioEntry.fromMap(doc.data());
        await _db.insertProntuarioEntry(entry);
      } catch (_) {}
    }
  }

  // ─── IMAGE UPLOAD ─────────────────────────────────────────────────────────────

  /// Uploads a prontuario image to Firebase Storage and returns its download URL.
  /// Returns null if the upload fails or the user is not authenticated.
  Future<String?> uploadProntuarioImage(int entryId, String localPath) async {
    final uid = _uid;
    if (uid == null) return null;

    try {
      final file = File(localPath);
      if (!file.existsSync()) return null;

      final ref = _storage
          .ref()
          .child('users/$uid/prontuario/$entryId/${file.uri.pathSegments.last}');

      final uploadTask = await ref.putFile(file);
      final downloadUrl = await uploadTask.ref.getDownloadURL();
      return downloadUrl;
    } catch (e) {
      return null;
    }
  }
}
