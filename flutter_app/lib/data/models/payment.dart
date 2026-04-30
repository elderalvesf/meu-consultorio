enum PaymentMethod {
  PIX,
  CARTAO_CREDITO,
  CARTAO_DEBITO,
  DINHEIRO,
  TRANSFERENCIA,
  CONVENIO,
}

extension PaymentMethodLabel on PaymentMethod {
  String get label {
    switch (this) {
      case PaymentMethod.PIX:
        return 'Pix';
      case PaymentMethod.CARTAO_CREDITO:
        return 'Cartão de Crédito';
      case PaymentMethod.CARTAO_DEBITO:
        return 'Cartão de Débito';
      case PaymentMethod.DINHEIRO:
        return 'Dinheiro';
      case PaymentMethod.TRANSFERENCIA:
        return 'Transferência';
      case PaymentMethod.CONVENIO:
        return 'Convênio';
    }
  }
}

class Payment {
  final int id;
  final int patientId;
  final String description;
  final double amount;
  final PaymentMethod method;
  final int date;
  final String notes;
  final bool isPaid;

  const Payment({
    required this.id,
    required this.patientId,
    required this.description,
    required this.amount,
    required this.method,
    required this.date,
    required this.notes,
    required this.isPaid,
  });

  Payment copyWith({
    int? id,
    int? patientId,
    String? description,
    double? amount,
    PaymentMethod? method,
    int? date,
    String? notes,
    bool? isPaid,
  }) {
    return Payment(
      id: id ?? this.id,
      patientId: patientId ?? this.patientId,
      description: description ?? this.description,
      amount: amount ?? this.amount,
      method: method ?? this.method,
      date: date ?? this.date,
      notes: notes ?? this.notes,
      isPaid: isPaid ?? this.isPaid,
    );
  }

  factory Payment.fromMap(Map<String, dynamic> map) {
    return Payment(
      id: (map['id'] as num?)?.toInt() ?? 0,
      patientId: (map['patientId'] as num?)?.toInt() ?? 0,
      description: map['description'] as String? ?? '',
      amount: (map['amount'] as num?)?.toDouble() ?? 0.0,
      method: PaymentMethod.values.firstWhere(
        (e) => e.name == (map['method'] as String? ?? 'DINHEIRO'),
        orElse: () => PaymentMethod.DINHEIRO,
      ),
      date: (map['date'] as num?)?.toInt() ?? 0,
      notes: map['notes'] as String? ?? '',
      isPaid: (map['isPaid'] == 1 || map['isPaid'] == true),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'patientId': patientId,
      'description': description,
      'amount': amount,
      'method': method.name,
      'date': date,
      'notes': notes,
      'isPaid': isPaid ? 1 : 0,
    };
  }

  @override
  String toString() =>
      'Payment(id: $id, patientId: $patientId, amount: $amount, method: ${method.name}, isPaid: $isPaid)';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Payment && runtimeType == other.runtimeType && id == other.id;

  @override
  int get hashCode => id.hashCode;
}
