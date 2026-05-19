package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TurnoStatus(val label: String) {
    PENDENTE("Pendente"),
    CONFIRMADO("Confirmado")
}

@Entity(tableName = "turnos")
data class Turno(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val valor: Double = 0.0,
    val status: TurnoStatus = TurnoStatus.PENDENTE
)
