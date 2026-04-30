enum TreatmentStatus {
  EM_ANDAMENTO,
  CONCLUIDO,
  CANCELADO,
}

extension TreatmentStatusLabel on TreatmentStatus {
  String get label {
    switch (this) {
      case TreatmentStatus.EM_ANDAMENTO:
        return 'Em Andamento';
      case TreatmentStatus.CONCLUIDO:
        return 'Concluído';
      case TreatmentStatus.CANCELADO:
        return 'Cancelado';
    }
  }
}

class Treatment {
  final int id;
  final int patientId;
  final String procedure;
  final String tooth;
  final String description;
  final double cost;
  final int date;
  final TreatmentStatus status;

  const Treatment({
    required this.id,
    required this.patientId,
    required this.procedure,
    required this.tooth,
    required this.description,
    required this.cost,
    required this.date,
    required this.status,
  });

  Treatment copyWith({
    int? id,
    int? patientId,
    String? procedure,
    String? tooth,
    String? description,
    double? cost,
    int? date,
    TreatmentStatus? status,
  }) {
    return Treatment(
      id: id ?? this.id,
      patientId: patientId ?? this.patientId,
      procedure: procedure ?? this.procedure,
      tooth: tooth ?? this.tooth,
      description: description ?? this.description,
      cost: cost ?? this.cost,
      date: date ?? this.date,
      status: status ?? this.status,
    );
  }

  factory Treatment.fromMap(Map<String, dynamic> map) {
    return Treatment(
      id: (map['id'] as num?)?.toInt() ?? 0,
      patientId: (map['patientId'] as num?)?.toInt() ?? 0,
      procedure: map['procedure'] as String? ?? '',
      tooth: map['tooth'] as String? ?? '',
      description: map['description'] as String? ?? '',
      cost: (map['cost'] as num?)?.toDouble() ?? 0.0,
      date: (map['date'] as num?)?.toInt() ?? 0,
      status: TreatmentStatus.values.firstWhere(
        (e) => e.name == (map['status'] as String? ?? 'EM_ANDAMENTO'),
        orElse: () => TreatmentStatus.EM_ANDAMENTO,
      ),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'patientId': patientId,
      'procedure': procedure,
      'tooth': tooth,
      'description': description,
      'cost': cost,
      'date': date,
      'status': status.name,
    };
  }

  @override
  String toString() =>
      'Treatment(id: $id, patientId: $patientId, procedure: $procedure, status: ${status.name})';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Treatment &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
