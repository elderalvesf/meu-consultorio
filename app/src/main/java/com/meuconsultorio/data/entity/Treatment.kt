package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "treatments",
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
data class Treatment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val procedure: String,
    val tooth: String = "",
    val description: String = "",
    val cost: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val status: TreatmentStatus = TreatmentStatus.EM_ANDAMENTO
)

enum class TreatmentStatus(val label: String) {
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado")
}
