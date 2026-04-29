package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.ProntuarioDao
import com.meuconsultorio.data.entity.ProntuarioEntry
import com.meuconsultorio.data.firebase.FirestoreSync
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProntuarioRepository @Inject constructor(
    private val dao: ProntuarioDao,
    private val sync: FirestoreSync
) {
    fun getEntriesByPatient(patientId: Long): Flow<List<ProntuarioEntry>> = dao.getEntriesByPatient(patientId)
    fun getEntriesByAppointment(appointmentId: Long): Flow<List<ProntuarioEntry>> = dao.getEntriesByAppointment(appointmentId)
    fun getEntryById(id: Long): Flow<ProntuarioEntry?> = dao.getEntryById(id)

    suspend fun insertEntry(entry: ProntuarioEntry): Long {
        val id = dao.insert(entry)
        sync.pushProntuarioEntry(entry.copy(id = id))
        return id
    }

    suspend fun updateEntry(entry: ProntuarioEntry) {
        dao.update(entry)
        sync.pushProntuarioEntry(entry)
    }

    suspend fun deleteEntry(entry: ProntuarioEntry) {
        dao.delete(entry)
        sync.deleteProntuarioEntry(entry.id)
    }
}
