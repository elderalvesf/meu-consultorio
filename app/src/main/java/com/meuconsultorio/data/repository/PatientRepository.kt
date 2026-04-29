package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.PatientDao
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.firebase.FirestoreSync
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val dao: PatientDao,
    private val sync: FirestoreSync
) {
    fun getAllPatients(): Flow<List<Patient>> = dao.getAllPatients()
    fun searchPatients(query: String): Flow<List<Patient>> = dao.searchPatients(query)
    fun getPatientById(id: Long): Flow<Patient?> = dao.getPatientById(id)
    fun getTotalPatients(): Flow<Int> = dao.getTotalPatients()

    suspend fun insertPatient(patient: Patient): Long {
        val id = dao.insertPatient(patient)
        sync.pushPatient(patient.copy(id = id))
        return id
    }

    suspend fun updatePatient(patient: Patient) {
        dao.updatePatient(patient)
        sync.pushPatient(patient)
    }

    suspend fun deletePatient(patient: Patient) {
        dao.deletePatient(patient)
        sync.deletePatient(patient.id)
    }
}
