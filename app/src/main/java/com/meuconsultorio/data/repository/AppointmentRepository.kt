package com.meuconsultorio.data.repository

import com.meuconsultorio.data.dao.AppointmentDao
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.firebase.FirestoreSync
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(
    private val dao: AppointmentDao,
    private val sync: FirestoreSync
) {
    fun getAllAppointments(): Flow<List<Appointment>> = dao.getAllAppointments()
    fun getAppointmentsByPatient(patientId: Long): Flow<List<Appointment>> = dao.getAppointmentsByPatient(patientId)
    fun getAppointmentsByDay(startOfDay: Long, endOfDay: Long): Flow<List<Appointment>> = dao.getAppointmentsByDay(startOfDay, endOfDay)
    fun getAppointmentsByRange(start: Long, end: Long): Flow<List<Appointment>> = dao.getAppointmentsByRange(start, end)
    fun getAppointmentById(id: Long): Flow<Appointment?> = dao.getAppointmentById(id)
    fun countAppointmentsToday(startOfDay: Long, endOfDay: Long): Flow<Int> =
        dao.countAppointmentsToday(startOfDay, endOfDay, listOf(AppointmentStatus.CANCELADA))
    fun getTotalPrice(): Flow<Double?> = dao.getTotalPrice()

    suspend fun insertAppointment(appointment: Appointment): Long {
        val id = dao.insertAppointment(appointment)
        sync.pushAppointment(appointment.copy(id = id))
        return id
    }

    suspend fun updateAppointment(appointment: Appointment) {
        dao.updateAppointment(appointment)
        sync.pushAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: Appointment) {
        dao.deleteAppointment(appointment)
        sync.deleteAppointment(appointment.id)
    }
}
