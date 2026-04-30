package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val repository: AppointmentRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    val allAppointments: StateFlow<List<Appointment>> = repository.getAllAppointments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAppointments: StateFlow<List<Appointment>> = _selectedDate.flatMapLatest { date ->
        val (start, end) = getDayRange(date)
        repository.getAppointmentsByDay(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayCount: StateFlow<Int> = run {
        val (start, end) = getDayRange(System.currentTimeMillis())
        repository.countAppointmentsToday(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    private val _selectedAppointment = MutableStateFlow<Appointment?>(null)
    val selectedAppointment: StateFlow<Appointment?> = _selectedAppointment.asStateFlow()

    private val _patientAppointments = MutableStateFlow<List<Appointment>>(emptyList())
    val patientAppointments: StateFlow<List<Appointment>> = _patientAppointments.asStateFlow()

    fun selectDate(dateMillis: Long) { _selectedDate.value = dateMillis }

    fun loadAppointment(id: Long) {
        viewModelScope.launch {
            repository.getAppointmentById(id).collect { _selectedAppointment.value = it }
        }
    }

    fun loadPatientAppointments(patientId: Long) {
        viewModelScope.launch {
            repository.getAppointmentsByPatient(patientId).collect {
                _patientAppointments.value = it
            }
        }
    }

    fun saveAppointment(appointment: Appointment, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (appointment.id == 0L) repository.insertAppointment(appointment)
            else repository.updateAppointment(appointment)
            onComplete()
        }
    }

    fun updateStatus(appointment: Appointment, status: AppointmentStatus) {
        viewModelScope.launch {
            repository.updateAppointment(appointment.copy(status = status))
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch { repository.deleteAppointment(appointment) }
    }

    private fun getDayRange(dateMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return Pair(start, cal.timeInMillis)
    }
}
