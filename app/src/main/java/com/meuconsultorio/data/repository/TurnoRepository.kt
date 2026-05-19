package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.TurnoDao
import com.meuconsultorio.data.entity.Turno
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TurnoRepository @Inject constructor(
    private val dao: TurnoDao
) {
    fun getAllTurnos(): Flow<List<Turno>> = dao.getAllTurnos()
    fun getTurnosByDay(start: Long, end: Long): Flow<List<Turno>> = dao.getTurnosByDay(start, end)
    fun getTurnoById(id: Long): Flow<Turno?> = dao.getTurnoById(id)
    suspend fun insertTurno(t: Turno): Long = dao.insertTurno(t)
    suspend fun updateTurno(t: Turno) = dao.updateTurno(t)
    suspend fun deleteTurno(t: Turno) = dao.deleteTurno(t)
}
