enum AppointmentStatus {
  AGENDADA,
  CONFIRMADA,
  CONCLUIDA,
  CANCELADA,
  NAO_COMPARECEU,
}

extension AppointmentStatusLabel on AppointmentStatus {
  String get label {
    switch (this) {
      case AppointmentStatus.AGENDADA:
        return 'Agendada';
      case AppointmentStatus.CONFIRMADA:
        return 'Confirmada';
      case AppointmentStatus.CONCLUIDA:
        return 'Concluída';
      case AppointmentStatus.CANCELADA:
        return 'Cancelada';
      case AppointmentStatus.NAO_COMPARECEU:
        return 'Não Compareceu';
    }
  }
}

class Appointment {
  final int id;
  final int patientId;
  final int dateTime; // milliseconds since epoch
  final int durationMinutes;
  final String procedureType;
  final AppointmentStatus status;
  final String notes;
  final int createdAt;
  final int calendarEventId; // -1 se não sincronizado

  const Appointment({
    required this.id,
    required this.patientId,
    required this.dateTime,
    required this.durationMinutes,
    required this.procedureType,
    required this.status,
    required this.notes,
    required this.createdAt,
    this.calendarEventId = -1,
  });

  Appointment copyWith({
    int? id,
    int? patientId,
    int? dateTime,
    int? durationMinutes,
    String? procedureType,
    AppointmentStatus? status,
    String? notes,
    int? createdAt,
    int? calendarEventId,
  }) {
    return Appointment(
      id: id ?? this.id,
      patientId: patientId ?? this.patientId,
      dateTime: dateTime ?? this.dateTime,
      durationMinutes: durationMinutes ?? this.durationMinutes,
      procedureType: procedureType ?? this.procedureType,
      status: status ?? this.status,
      notes: notes ?? this.notes,
      createdAt: createdAt ?? this.createdAt,
      calendarEventId: calendarEventId ?? this.calendarEventId,
    );
  }

  factory Appointment.fromMap(Map<String, dynamic> map) {
    return Appointment(
      id: (map['id'] as num?)?.toInt() ?? 0,
      patientId: (map['patientId'] as num?)?.toInt() ?? 0,
      dateTime: (map['dateTime'] as num?)?.toInt() ?? 0,
      durationMinutes: (map['durationMinutes'] as num?)?.toInt() ?? 30,
      procedureType: map['procedureType'] as String? ?? '',
      status: AppointmentStatus.values.firstWhere(
        (e) => e.name == (map['status'] as String? ?? 'AGENDADA'),
        orElse: () => AppointmentStatus.AGENDADA,
      ),
      notes: map['notes'] as String? ?? '',
      createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
      calendarEventId: (map['calendarEventId'] as num?)?.toInt() ?? -1,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'patientId': patientId,
      'dateTime': dateTime,
      'durationMinutes': durationMinutes,
      'procedureType': procedureType,
      'status': status.name,
      'notes': notes,
      'createdAt': createdAt,
      'calendarEventId': calendarEventId,
    };
  }

  @override
  String toString() =>
      'Appointment(id: $id, patientId: $patientId, dateTime: $dateTime, status: ${status.name})';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Appointment &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
