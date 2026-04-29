package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.TreatmentDao
import com.meuconsultorio.data.entity.Treatment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TreatmentRepository @Inject constructor(private val dao: TreatmentDao) {

    fun getAllTreatments(): Flow<List<Treatment>> = dao.getAllTreatments()

    fun getTreatmentsByPatient(patientId: Long): Flow<List<Treatment>> =
        dao.getTreatmentsByPatient(patientId)

    fun getTreatmentById(id: Long): Flow<Treatment?> = dao.getTreatmentById(id)

    fun getTotalCostByPatient(patientId: Long): Flow<Double?> =
        dao.getTotalCostByPatient(patientId)

    fun getTotalCost(): Flow<Double?> = dao.getTotalCost()

    suspend fun insertTreatment(treatment: Treatment): Long = dao.insertTreatment(treatment)

    suspend fun updateTreatment(treatment: Treatment) = dao.updateTreatment(treatment)

    suspend fun deleteTreatment(treatment: Treatment) = dao.deleteTreatment(treatment)
}
