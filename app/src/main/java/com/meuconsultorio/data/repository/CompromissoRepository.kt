package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.CompromissoDao
import com.meuconsultorio.data.entity.Compromisso
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompromissoRepository @Inject constructor(
    private val dao: CompromissoDao
) {
    fun getAllCompromissos(): Flow<List<Compromisso>> = dao.getAllCompromissos()
    fun getCompromissosByDay(start: Long, end: Long): Flow<List<Compromisso>> = dao.getCompromissosByDay(start, end)
    fun getCompromissoById(id: Long): Flow<Compromisso?> = dao.getCompromissoById(id)
    suspend fun insertCompromisso(c: Compromisso): Long = dao.insertCompromisso(c)
    suspend fun updateCompromisso(c: Compromisso) = dao.updateCompromisso(c)
    suspend fun deleteCompromisso(c: Compromisso) = dao.deleteCompromisso(c)
}
