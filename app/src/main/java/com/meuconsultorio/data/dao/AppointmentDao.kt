package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments ORDER BY dateTime ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY dateTime DESC")
    fun getAppointmentsByPatient(patientId: Long): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE dateTime BETWEEN :startOfDay AND :endOfDay ORDER BY dateTime ASC")
    fun getAppointmentsByDay(startOfDay: Long, endOfDay: Long): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE dateTime BETWEEN :start AND :end ORDER BY dateTime ASC")
    fun getAppointmentsByRange(start: Long, end: Long): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    fun getAppointmentById(id: Long): Flow<Appointment?>

    @Query("SELECT COUNT(*) FROM appointments WHERE dateTime BETWEEN :startOfDay AND :endOfDay AND status NOT IN (:excludedStatuses)")
    fun countAppointmentsToday(startOfDay: Long, endOfDay: Long, excludedStatuses: List<AppointmentStatus>): Flow<Int>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = :status")
    fun countAppointmentsByStatus(status: AppointmentStatus): Flow<Int>

    @Query("SELECT SUM(price) FROM appointments WHERE isPaid = 1")
    fun getTotalPrice(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)
}
