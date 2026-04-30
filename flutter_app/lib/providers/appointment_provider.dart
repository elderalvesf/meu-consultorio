import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/models/appointment.dart';
import '../data/repositories/appointment_repository.dart';
import '../data/services/calendar_service.dart';
import '../data/repositories/patient_repository.dart';

// ─── State ────────────────────────────────────────────────────────────────────

class AppointmentState {
  final DateTime selectedDate;
  final List<Appointment> appointments;       // filtered by selectedDate
  final List<Appointment> allAppointments;
  final List<Appointment> patientAppointments;
  final Appointment? selectedAppointment;
  final int todayCount;
  final bool isLoading;
  final String? errorMessage;

  const AppointmentState({
    DateTime? selectedDate,
    this.appointments = const [],
    this.allAppointments = const [],
    this.patientAppointments = const [],
    this.selectedAppointment,
    this.todayCount = 0,
    this.isLoading = false,
    this.errorMessage,
  }) : selectedDate = selectedDate ?? _kToday;

  static final DateTime _kToday = DateTime.now();

  AppointmentState copyWith({
    DateTime? selectedDate,
    List<Appointment>? appointments,
    List<Appointment>? allAppointments,
    List<Appointment>? patientAppointments,
    Appointment? selectedAppointment,
    bool clearSelectedAppointment = false,
    int? todayCount,
    bool? isLoading,
    String? errorMessage,
  }) {
    return AppointmentState(
      selectedDate: selectedDate ?? this.selectedDate,
      appointments: appointments ?? this.appointments,
      allAppointments: allAppointments ?? this.allAppointments,
      patientAppointments: patientAppointments ?? this.patientAppointments,
      selectedAppointment: clearSelectedAppointment
          ? null
          : (selectedAppointment ?? this.selectedAppointment),
      todayCount: todayCount ?? this.todayCount,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class AppointmentNotifier extends StateNotifier<AppointmentState> {
  final AppointmentRepository _repository;
  final PatientRepository _patientRepository;
  final CalendarService _calendarService;

  AppointmentNotifier(
    this._repository,
    this._patientRepository,
    this._calendarService,
  ) : super(const AppointmentState()) {
    loadAll();
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  static int _dayStart(DateTime date) =>
      DateTime(date.year, date.month, date.day)
          .millisecondsSinceEpoch;

  static int _dayEnd(DateTime date) =>
      DateTime(date.year, date.month, date.day, 23, 59, 59, 999)
          .millisecondsSinceEpoch;

  // ─── Load ───────────────────────────────────────────────────────────────────

  Future<void> loadAll() async {
    state = state.copyWith(isLoading: true);
    try {
      final all = await _repository.getAll();
      final today = DateTime.now();
      final todayCount = await _repository.countToday(
        _dayStart(today),
        _dayEnd(today),
      );
      final dayList = await _repository.getByDay(
        _dayStart(state.selectedDate),
        _dayEnd(state.selectedDate),
      );
      state = state.copyWith(
        allAppointments: all,
        appointments: dayList,
        todayCount: todayCount,
        isLoading: false,
      );
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> loadByDate(DateTime date) async {
    state = state.copyWith(selectedDate: date, isLoading: true);
    try {
      final dayList = await _repository.getByDay(
        _dayStart(date),
        _dayEnd(date),
      );
      state = state.copyWith(appointments: dayList, isLoading: false);
    } catch (e) {
      state = state.copyWith(isLoading: false, errorMessage: e.toString());
    }
  }

  Future<void> loadById(int id) async {
    try {
      final appointment = await _repository.getById(id);
      state = state.copyWith(selectedAppointment: appointment);
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> loadByPatient(int patientId) async {
    try {
      final list = await _repository.getByPatient(patientId);
      state = state.copyWith(patientAppointments: list);
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  // ─── Write ──────────────────────────────────────────────────────────────────

  Future<void> save(Appointment appointment) async {
    try {
      if (appointment.id == 0) {
        await _repository.insert(appointment);
      } else {
        await _repository.update(appointment);
      }
      await loadAll();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> delete(int id) async {
    try {
      await _repository.delete(id);
      await loadAll();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  Future<void> updateStatus(int id, AppointmentStatus status) async {
    try {
      final appointment = await _repository.getById(id);
      if (appointment == null) return;
      await _repository.update(appointment.copyWith(status: status));
      await loadAll();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  // ─── Calendar Sync ──────────────────────────────────────────────────────────

  /// Opens the device calendar so the user can add the appointment event.
  /// Marks [calendarEventId] as 1 (synced) on success.
  Future<void> syncCalendar(Appointment appointment) async {
    try {
      final patient =
          await _patientRepository.getById(appointment.patientId);
      final patientName = patient?.name ?? 'Paciente';
      final success =
          await _calendarService.addEvent(appointment, patientName);
      if (success) {
        // We use 1 to indicate synced (no native event ID returned by add_2_calendar)
        final updated = appointment.copyWith(calendarEventId: 1);
        await _repository.update(updated);
        await loadAll();
      }
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }

  /// Clears the calendar sync flag for the given appointment.
  Future<void> unsyncCalendar(Appointment appointment) async {
    try {
      final updated = appointment.copyWith(calendarEventId: -1);
      await _repository.update(updated);
      await loadAll();
    } catch (e) {
      state = state.copyWith(errorMessage: e.toString());
    }
  }
}

// ─── Providers ────────────────────────────────────────────────────────────────

final appointmentRepositoryProvider = Provider<AppointmentRepository>(
  (ref) => AppointmentRepository(),
);

final calendarServiceProvider = Provider<CalendarService>(
  (ref) => CalendarService(),
);

final _patientRepositoryForApptProvider = Provider<PatientRepository>(
  (ref) => PatientRepository(),
);

final appointmentProvider =
    StateNotifierProvider<AppointmentNotifier, AppointmentState>((ref) {
  return AppointmentNotifier(
    ref.watch(appointmentRepositoryProvider),
    ref.watch(_patientRepositoryForApptProvider),
    ref.watch(calendarServiceProvider),
  );
});
