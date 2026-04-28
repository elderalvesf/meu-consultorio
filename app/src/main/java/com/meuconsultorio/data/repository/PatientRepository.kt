package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.PatientDao
import com.meuconsultorio.data.entity.Patient
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(private val dao: PatientDao) {

    fun getAllPatients(): Flow<List<Patient>> = dao.getAllPatients()

    fun searchPatients(query: String): Flow<List<Patient>> = dao.searchPatients(query)

    fun getPatientById(id: Long): Flow<Patient?> = dao.getPatientById(id)

    fun getTotalPatients(): Flow<Int> = dao.getTotalPatients()

    suspend fun insertPatient(patient: Patient): Long = dao.insertPatient(patient)

    suspend fun updatePatient(patient: Patient) = dao.updatePatient(patient)

    suspend fun deletePatient(patient: Patient) = dao.deletePatient(patient)
}
