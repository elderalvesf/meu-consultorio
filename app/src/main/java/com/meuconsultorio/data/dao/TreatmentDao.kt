package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.Treatment
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {

    @Query("SELECT * FROM treatments ORDER BY date DESC")
    fun getAllTreatments(): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE patientId = :patientId ORDER BY date DESC")
    fun getTreatmentsByPatient(patientId: Long): Flow<List<Treatment>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    fun getTreatmentById(id: Long): Flow<Treatment?>

    @Query("SELECT SUM(cost) FROM treatments WHERE patientId = :patientId AND status = 'CONCLUIDO'")
    fun getTotalCostByPatient(patientId: Long): Flow<Double?>

    @Query("SELECT SUM(cost) FROM treatments WHERE status = 'CONCLUIDO'")
    fun getTotalCost(): Flow<Double?>

    @Query("SELECT SUM(price) FROM treatments WHERE patientId = :patientId AND status = 'CONCLUIDO'")
    fun getTotalPriceByPatient(patientId: Long): Flow<Double?>

    @Query("SELECT SUM(price) FROM treatments WHERE status = 'CONCLUIDO'")
    fun getTotalPrice(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatment(treatment: Treatment): Long

    @Update
    suspend fun updateTreatment(treatment: Treatment)

    @Delete
    suspend fun deleteTreatment(treatment: Treatment)
}
