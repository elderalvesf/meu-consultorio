package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE patientId = :patientId ORDER BY date DESC")
    fun getPaymentsByPatient(patientId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    fun getPaymentById(id: Long): Flow<Payment?>

    @Query("SELECT SUM(amount) FROM payments WHERE isPaid = 1")
    fun getTotalReceived(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE isPaid = 0")
    fun getTotalPending(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE isPaid = 1 AND date BETWEEN :start AND :end")
    fun getTotalReceivedInRange(start: Long, end: Long): Flow<Double?>

    @Query("SELECT * FROM payments WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getPaymentsByRange(start: Long, end: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)
}
