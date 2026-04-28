package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.PaymentDao
import com.meuconsultorio.data.entity.Payment
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(private val dao: PaymentDao) {

    fun getAllPayments(): Flow<List<Payment>> = dao.getAllPayments()

    fun getPaymentsByPatient(patientId: Long): Flow<List<Payment>> =
        dao.getPaymentsByPatient(patientId)

    fun getPaymentById(id: Long): Flow<Payment?> = dao.getPaymentById(id)

    fun getTotalReceived(): Flow<Double?> = dao.getTotalReceived()

    fun getTotalPending(): Flow<Double?> = dao.getTotalPending()

    fun getTotalReceivedInRange(start: Long, end: Long): Flow<Double?> =
        dao.getTotalReceivedInRange(start, end)

    fun getPaymentsByRange(start: Long, end: Long): Flow<List<Payment>> =
        dao.getPaymentsByRange(start, end)

    suspend fun insertPayment(payment: Payment): Long = dao.insertPayment(payment)

    suspend fun updatePayment(payment: Payment) = dao.updatePayment(payment)

    suspend fun deletePayment(payment: Payment) = dao.deletePayment(payment)
}
