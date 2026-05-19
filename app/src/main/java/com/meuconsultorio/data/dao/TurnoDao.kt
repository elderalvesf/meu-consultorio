package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.Turno
import kotlinx.coroutines.flow.Flow

@Dao
interface TurnoDao {

    @Query("SELECT * FROM turnos ORDER BY date ASC")
    fun getAllTurnos(): Flow<List<Turno>>

    @Query("SELECT * FROM turnos WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date ASC")
    fun getTurnosByDay(startOfDay: Long, endOfDay: Long): Flow<List<Turno>>

    @Query("SELECT * FROM turnos WHERE id = :id")
    fun getTurnoById(id: Long): Flow<Turno?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurno(turno: Turno): Long

    @Update
    suspend fun updateTurno(turno: Turno)

    @Delete
    suspend fun deleteTurno(turno: Turno)
}
