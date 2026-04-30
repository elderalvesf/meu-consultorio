class ProntuarioEntry {
  final int id;
  final int patientId;
  final int? appointmentId;
  final String text;
  final String? imagePath;
  final String? imageUrl;
  final int createdAt;

  const ProntuarioEntry({
    required this.id,
    required this.patientId,
    this.appointmentId,
    required this.text,
    this.imagePath,
    this.imageUrl,
    required this.createdAt,
  });

  ProntuarioEntry copyWith({
    int? id,
    int? patientId,
    int? appointmentId,
    String? text,
    String? imagePath,
    String? imageUrl,
    int? createdAt,
  }) {
    return ProntuarioEntry(
      id: id ?? this.id,
      patientId: patientId ?? this.patientId,
      appointmentId: appointmentId ?? this.appointmentId,
      text: text ?? this.text,
      imagePath: imagePath ?? this.imagePath,
      imageUrl: imageUrl ?? this.imageUrl,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  factory ProntuarioEntry.fromMap(Map<String, dynamic> map) {
    return ProntuarioEntry(
      id: (map['id'] as num?)?.toInt() ?? 0,
      patientId: (map['patientId'] as num?)?.toInt() ?? 0,
      appointmentId: (map['appointmentId'] as num?)?.toInt(),
      text: map['text'] as String? ?? '',
      imagePath: map['imagePath'] as String?,
      imageUrl: map['imageUrl'] as String?,
      createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'patientId': patientId,
      'appointmentId': appointmentId,
      'text': text,
      'imagePath': imagePath,
      'imageUrl': imageUrl,
      'createdAt': createdAt,
    };
  }

  @override
  String toString() =>
      'ProntuarioEntry(id: $id, patientId: $patientId, appointmentId: $appointmentId, createdAt: $createdAt)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is ProntuarioEntry &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}
