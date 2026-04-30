package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId")]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val description: String,
    val amount: Double,
    val method: PaymentMethod = PaymentMethod.PIX,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isPaid: Boolean = true
)

enum class PaymentMethod(val label: String) {
    DINHEIRO("Dinheiro"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    PIX("PIX"),
    CONVENIO("Convênio"),
    OUTRO("Outro")
}
