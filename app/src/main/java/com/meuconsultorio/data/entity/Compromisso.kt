package com.meuconsultorio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compromissos")
data class Compromisso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val date: Long = System.currentTimeMillis()
)