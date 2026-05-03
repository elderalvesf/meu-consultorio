package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.TreatmentDao
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.firebase.FirestoreSync
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TreatmentRepository @Inject constructor(
    private val dao: TreatmentDao,
    private val sync: FirestoreSync
) {
    fun getAllTreatments(): Flow<List<Treatment>> = dao.getAllTreatments()
    fun getTreatmentsByPatient(patientId: Long): Flow<List<Treatment>> = dao.getTreatmentsByPatient(patientId)
    fun getTreatmentById(id: Long): Flow<Treatment?> = dao.getTreatmentById(id)
    fun getTotalCostByPatient(patientId: Long): Flow<Double?> = dao.getTotalCostByPatient(patientId)
    fun getTotalCost(): Flow<Double?> = dao.getTotalCost()
    fun getTotalPriceByPatient(patientId: Long): Flow<Double?> = dao.getTotalPriceByPatient(patientId)
    fun getTotalPrice(): Flow<Double?> = dao.getTotalPrice()

    suspend fun insertTreatment(treatment: Treatment): Long {
        val id = dao.insertTreatment(treatment)
        sync.pushTreatment(treatment.copy(id = id))
        return id
    }

    suspend fun updateTreatment(treatment: Treatment) {
        dao.updateTreatment(treatment)
        sync.pushTreatment(treatment)
    }

    suspend fun deleteTreatment(treatment: Treatment) {
        dao.deleteTreatment(treatment)
        sync.deleteTreatment(treatment.id)
    }
}
