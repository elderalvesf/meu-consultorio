package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prontuario_entries",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Appointment::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("patientId"), Index("appointmentId")]
)
data class ProntuarioEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: Long,
    val appointmentId: Long? = null,
    val text: String = "",
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
