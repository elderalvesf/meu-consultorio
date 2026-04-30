import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

import '../models/patient.dart';
import '../models/appointment.dart';
import '../models/treatment.dart';
import '../models/payment.dart';
import '../models/prontuario_entry.dart';

class AppDatabase {
  static const _databaseName = 'meu_consultorio.db';
  static const _databaseVersion = 1;

  static AppDatabase? _instance;
  static Database? _database;

  AppDatabase._internal();

  factory AppDatabase() {
    _instance ??= AppDatabase._internal();
    return _instance!;
  }

  Future<Database> get database async {
    _database ??= await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, _databaseName);

    return openDatabase(
      path,
      version: _databaseVersion,
      onCreate: _onCreate,
    );
  }

  Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE patients (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        name TEXT NOT NULL,
        cpf TEXT NOT NULL DEFAULT '',
        phone TEXT NOT NULL DEFAULT '',
        email TEXT NOT NULL DEFAULT '',
        birthDate TEXT NOT NULL DEFAULT '',
        address TEXT NOT NULL DEFAULT '',
        notes TEXT NOT NULL DEFAULT '',
        createdAt INTEGER NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE appointments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        patientId INTEGER NOT NULL,
        dateTime INTEGER NOT NULL,
        durationMinutes INTEGER NOT NULL DEFAULT 30,
        procedureType TEXT NOT NULL DEFAULT '',
        status TEXT NOT NULL DEFAULT 'AGENDADA',
        notes TEXT NOT NULL DEFAULT '',
        createdAt INTEGER NOT NULL,
        calendarEventId INTEGER NOT NULL DEFAULT -1,
        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE treatments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        patientId INTEGER NOT NULL,
        procedure TEXT NOT NULL DEFAULT '',
        tooth TEXT NOT NULL DEFAULT '',
        description TEXT NOT NULL DEFAULT '',
        cost REAL NOT NULL DEFAULT 0.0,
        date INTEGER NOT NULL,
        status TEXT NOT NULL DEFAULT 'EM_ANDAMENTO',
        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE payments (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        patientId INTEGER NOT NULL,
        description TEXT NOT NULL DEFAULT '',
        amount REAL NOT NULL DEFAULT 0.0,
        method TEXT NOT NULL DEFAULT 'DINHEIRO',
        date INTEGER NOT NULL,
        notes TEXT NOT NULL DEFAULT '',
        isPaid INTEGER NOT NULL DEFAULT 0,
        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
      )
    ''');

    await db.execute('''
      CREATE TABLE prontuario_entries (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        patientId INTEGER NOT NULL,
        appointmentId INTEGER,
        text TEXT NOT NULL DEFAULT '',
        imagePath TEXT,
        imageUrl TEXT,
        createdAt INTEGER NOT NULL,
        FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
      )
    ''');
  }

  // ─── PATIENTS ────────────────────────────────────────────────────────────────

  Future<int> insertPatient(Patient patient) async {
    final db = await database;
    final map = patient.toMap()..remove('id');
    return db.insert('patients', map,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updatePatient(Patient patient) async {
    final db = await database;
    return db.update(
      'patients',
      patient.toMap(),
      where: 'id = ?',
      whereArgs: [patient.id],
    );
  }

  Future<int> deletePatient(int id) async {
    final db = await database;
    return db.delete('patients', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Patient>> getAllPatients() async {
    final db = await database;
    final maps = await db.query('patients', orderBy: 'name ASC');
    return maps.map((m) => Patient.fromMap(m)).toList();
  }

  Future<Patient?> getPatientById(int id) async {
    final db = await database;
    final maps =
        await db.query('patients', where: 'id = ?', whereArgs: [id], limit: 1);
    if (maps.isEmpty) return null;
    return Patient.fromMap(maps.first);
  }

  Future<int> countPatients() async {
    final db = await database;
    final result =
        await db.rawQuery('SELECT COUNT(*) as count FROM patients');
    return (result.first['count'] as num).toInt();
  }

  // ─── APPOINTMENTS ─────────────────────────────────────────────────────────────

  Future<int> insertAppointment(Appointment appointment) async {
    final db = await database;
    final map = appointment.toMap()..remove('id');
    return db.insert('appointments', map,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updateAppointment(Appointment appointment) async {
    final db = await database;
    return db.update(
      'appointments',
      appointment.toMap(),
      where: 'id = ?',
      whereArgs: [appointment.id],
    );
  }

  Future<int> deleteAppointment(int id) async {
    final db = await database;
    return db.delete('appointments', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Appointment>> getAllAppointments() async {
    final db = await database;
    final maps = await db.query('appointments', orderBy: 'dateTime DESC');
    return maps.map((m) => Appointment.fromMap(m)).toList();
  }

  Future<Appointment?> getAppointmentById(int id) async {
    final db = await database;
    final maps = await db.query('appointments',
        where: 'id = ?', whereArgs: [id], limit: 1);
    if (maps.isEmpty) return null;
    return Appointment.fromMap(maps.first);
  }

  Future<List<Appointment>> getAppointmentsByPatientId(int patientId) async {
    final db = await database;
    final maps = await db.query(
      'appointments',
      where: 'patientId = ?',
      whereArgs: [patientId],
      orderBy: 'dateTime DESC',
    );
    return maps.map((m) => Appointment.fromMap(m)).toList();
  }

  Future<List<Appointment>> getAppointmentsByDay(int start, int end) async {
    final db = await database;
    final maps = await db.query(
      'appointments',
      where: 'dateTime >= ? AND dateTime < ?',
      whereArgs: [start, end],
      orderBy: 'dateTime ASC',
    );
    return maps.map((m) => Appointment.fromMap(m)).toList();
  }

  Future<List<Appointment>> getAppointmentsByRange(int start, int end) async {
    final db = await database;
    final maps = await db.query(
      'appointments',
      where: 'dateTime >= ? AND dateTime < ?',
      whereArgs: [start, end],
      orderBy: 'dateTime ASC',
    );
    return maps.map((m) => Appointment.fromMap(m)).toList();
  }

  Future<int> countTodayAppointments(int start, int end) async {
    final db = await database;
    final result = await db.rawQuery(
      'SELECT COUNT(*) as count FROM appointments WHERE dateTime >= ? AND dateTime < ?',
      [start, end],
    );
    return (result.first['count'] as num).toInt();
  }

  // ─── TREATMENTS ───────────────────────────────────────────────────────────────

  Future<int> insertTreatment(Treatment treatment) async {
    final db = await database;
    final map = treatment.toMap()..remove('id');
    return db.insert('treatments', map,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updateTreatment(Treatment treatment) async {
    final db = await database;
    return db.update(
      'treatments',
      treatment.toMap(),
      where: 'id = ?',
      whereArgs: [treatment.id],
    );
  }

  Future<int> deleteTreatment(int id) async {
    final db = await database;
    return db.delete('treatments', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Treatment>> getAllTreatments() async {
    final db = await database;
    final maps = await db.query('treatments', orderBy: 'date DESC');
    return maps.map((m) => Treatment.fromMap(m)).toList();
  }

  Future<Treatment?> getTreatmentById(int id) async {
    final db = await database;
    final maps = await db.query('treatments',
        where: 'id = ?', whereArgs: [id], limit: 1);
    if (maps.isEmpty) return null;
    return Treatment.fromMap(maps.first);
  }

  Future<List<Treatment>> getTreatmentsByPatientId(int patientId) async {
    final db = await database;
    final maps = await db.query(
      'treatments',
      where: 'patientId = ?',
      whereArgs: [patientId],
      orderBy: 'date DESC',
    );
    return maps.map((m) => Treatment.fromMap(m)).toList();
  }

  // ─── PAYMENTS ─────────────────────────────────────────────────────────────────

  Future<int> insertPayment(Payment payment) async {
    final db = await database;
    final map = payment.toMap()..remove('id');
    return db.insert('payments', map,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updatePayment(Payment payment) async {
    final db = await database;
    return db.update(
      'payments',
      payment.toMap(),
      where: 'id = ?',
      whereArgs: [payment.id],
    );
  }

  Future<int> deletePayment(int id) async {
    final db = await database;
    return db.delete('payments', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<Payment>> getAllPayments() async {
    final db = await database;
    final maps = await db.query('payments', orderBy: 'date DESC');
    return maps.map((m) => Payment.fromMap(m)).toList();
  }

  Future<Payment?> getPaymentById(int id) async {
    final db = await database;
    final maps = await db.query('payments',
        where: 'id = ?', whereArgs: [id], limit: 1);
    if (maps.isEmpty) return null;
    return Payment.fromMap(maps.first);
  }

  Future<List<Payment>> getPaymentsByPatientId(int patientId) async {
    final db = await database;
    final maps = await db.query(
      'payments',
      where: 'patientId = ?',
      whereArgs: [patientId],
      orderBy: 'date DESC',
    );
    return maps.map((m) => Payment.fromMap(m)).toList();
  }

  Future<double> getTotalReceived() async {
    final db = await database;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(amount), 0.0) as total FROM payments WHERE isPaid = 1',
    );
    return (result.first['total'] as num?)?.toDouble() ?? 0.0;
  }

  Future<double> getMonthReceived(int start, int end) async {
    final db = await database;
    final result = await db.rawQuery(
      'SELECT COALESCE(SUM(amount), 0.0) as total FROM payments WHERE isPaid = 1 AND date >= ? AND date < ?',
      [start, end],
    );
    return (result.first['total'] as num?)?.toDouble() ?? 0.0;
  }

  // ─── PRONTUARIO ENTRIES ───────────────────────────────────────────────────────

  Future<int> insertProntuarioEntry(ProntuarioEntry entry) async {
    final db = await database;
    final map = entry.toMap()..remove('id');
    return db.insert('prontuario_entries', map,
        conflictAlgorithm: ConflictAlgorithm.replace);
  }

  Future<int> updateProntuarioEntry(ProntuarioEntry entry) async {
    final db = await database;
    return db.update(
      'prontuario_entries',
      entry.toMap(),
      where: 'id = ?',
      whereArgs: [entry.id],
    );
  }

  Future<int> deleteProntuarioEntry(int id) async {
    final db = await database;
    return db.delete('prontuario_entries', where: 'id = ?', whereArgs: [id]);
  }

  Future<List<ProntuarioEntry>> getAllProntuarioEntries() async {
    final db = await database;
    final maps =
        await db.query('prontuario_entries', orderBy: 'createdAt DESC');
    return maps.map((m) => ProntuarioEntry.fromMap(m)).toList();
  }

  Future<ProntuarioEntry?> getProntuarioEntryById(int id) async {
    final db = await database;
    final maps = await db.query('prontuario_entries',
        where: 'id = ?', whereArgs: [id], limit: 1);
    if (maps.isEmpty) return null;
    return ProntuarioEntry.fromMap(maps.first);
  }

  Future<List<ProntuarioEntry>> getProntuarioEntriesByPatientId(
      int patientId) async {
    final db = await database;
    final maps = await db.query(
      'prontuario_entries',
      where: 'patientId = ?',
      whereArgs: [patientId],
      orderBy: 'createdAt DESC',
    );
    return maps.map((m) => ProntuarioEntry.fromMap(m)).toList();
  }
}
