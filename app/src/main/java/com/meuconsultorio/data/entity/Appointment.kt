package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
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
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val dateTime: Long,
    val durationMinutes: Int = 60,
    val procedureType: String,
    val status: AppointmentStatus = AppointmentStatus.AGENDADA,
    val notes: String = "",
    val price: Double = 0.0,
    val isPaid: Boolean = false,
    val attachments: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val calendarEventId: Long = -1L
)

enum class AppointmentStatus(val label: String) {
    AGENDADA("Agendada"),
    CONFIRMADA("Confirmada"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada"),
    NAO_COMPARECEU("Não compareceu")
}
