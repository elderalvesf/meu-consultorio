class Patient {
  final int id;
  final String name;
  final String cpf;
  final String phone;
  final String email;
  final String birthDate;
  final String address;
  final String notes;
  final int createdAt;

  const Patient({
    required this.id,
    required this.name,
    required this.cpf,
    required this.phone,
    required this.email,
    required this.birthDate,
    required this.address,
    required this.notes,
    required this.createdAt,
  });

  Patient copyWith({
    int? id,
    String? name,
    String? cpf,
    String? phone,
    String? email,
    String? birthDate,
    String? address,
    String? notes,
    int? createdAt,
  }) {
    return Patient(
      id: id ?? this.id,
      name: name ?? this.name,
      cpf: cpf ?? this.cpf,
      phone: phone ?? this.phone,
      email: email ?? this.email,
      birthDate: birthDate ?? this.birthDate,
      address: address ?? this.address,
      notes: notes ?? this.notes,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  factory Patient.fromMap(Map<String, dynamic> map) {
    return Patient(
      id: (map['id'] as num?)?.toInt() ?? 0,
      name: map['name'] as String? ?? '',
      cpf: map['cpf'] as String? ?? '',
      phone: map['phone'] as String? ?? '',
      email: map['email'] as String? ?? '',
      birthDate: map['birthDate'] as String? ?? '',
      address: map['address'] as String? ?? '',
      notes: map['notes'] as String? ?? '',
      createdAt: (map['createdAt'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'cpf': cpf,
      'phone': phone,
      'email': email,
      'birthDate': birthDate,
      'address': address,
      'notes': notes,
      'createdAt': createdAt,
    };
  }

  @override
  String toString() =>
      'Patient(id: $id, name: $name, cpf: $cpf, phone: $phone, email: $email)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Patient && runtimeType == other.runtimeType && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
