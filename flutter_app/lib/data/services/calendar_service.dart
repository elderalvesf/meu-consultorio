import 'package:add_2_calendar/add_2_calendar.dart';

import '../models/appointment.dart';

class CalendarService {
  /// Opens the device calendar app so the user can confirm adding the event.
  /// Returns true if the intent was dispatched without errors.
  /// Note: add_2_calendar does not return a native event ID – calendar sync
  /// status is tracked locally via [Appointment.calendarEventId].
  Future<bool> addEvent(Appointment appointment, String patientName) async {
    try {
      final start =
          DateTime.fromMillisecondsSinceEpoch(appointment.dateTime);
      final end = start.add(Duration(minutes: appointment.durationMinutes));

      final event = Event(
        title: '${appointment.procedureType} – $patientName',
        description: appointment.notes,
        startDate: start,
        endDate: end,
        iosParams: const IOSParams(reminder: Duration(minutes: 30)),
        androidParams: const AndroidParams(emailInvites: []),
      );

      await Add2Calendar.addEvent2Cal(event);
      return true;
    } catch (_) {
      return false;
    }
  }
}
