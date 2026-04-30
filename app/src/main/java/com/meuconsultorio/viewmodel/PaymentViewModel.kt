package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Payment
import com.meuconsultorio.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: PaymentRepository
) : ViewModel() {

    val allPayments: StateFlow<List<Payment>> = repository.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalReceived: StateFlow<Double> = repository.getTotalReceived()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPending: StateFlow<Double> = repository.getTotalPending()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthReceived: StateFlow<Double> = run {
        val (start, end) = getCurrentMonthRange()
        repository.getTotalReceivedInRange(start, end)
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }

    private val _patientPayments = MutableStateFlow<List<Payment>>(emptyList())
    val patientPayments: StateFlow<List<Payment>> = _patientPayments.asStateFlow()

    private val _selectedPayment = MutableStateFlow<Payment?>(null)
    val selectedPayment: StateFlow<Payment?> = _selectedPayment.asStateFlow()

    fun loadPatientPayments(patientId: Long) {
        viewModelScope.launch {
            repository.getPaymentsByPatient(patientId).collect { _patientPayments.value = it }
        }
    }

    fun loadPayment(id: Long) {
        viewModelScope.launch {
            repository.getPaymentById(id).collect { _selectedPayment.value = it }
        }
    }

    fun savePayment(payment: Payment, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (payment.id == 0L) repository.insertPayment(payment)
            else repository.updatePayment(payment)
            onComplete()
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch { repository.deletePayment(payment) }
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return Pair(start, cal.timeInMillis)
    }
}
