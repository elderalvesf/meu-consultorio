package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.repository.TreatmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TreatmentViewModel @Inject constructor(
    private val repository: TreatmentRepository
) : ViewModel() {

    private val _patientTreatments = MutableStateFlow<List<Treatment>>(emptyList())
    val patientTreatments: StateFlow<List<Treatment>> = _patientTreatments.asStateFlow()

    private val _patientTotalCost = MutableStateFlow(0.0)
    val patientTotalCost: StateFlow<Double> = _patientTotalCost.asStateFlow()

    private val _selectedTreatment = MutableStateFlow<Treatment?>(null)
    val selectedTreatment: StateFlow<Treatment?> = _selectedTreatment.asStateFlow()

    fun loadPatientTreatments(patientId: Long) {
        viewModelScope.launch {
            repository.getTreatmentsByPatient(patientId).collect {
                _patientTreatments.value = it
            }
        }
        viewModelScope.launch {
            repository.getTotalCostByPatient(patientId).collect {
                _patientTotalCost.value = it ?: 0.0
            }
        }
    }

    fun loadTreatment(id: Long) {
        viewModelScope.launch {
            repository.getTreatmentById(id).collect { _selectedTreatment.value = it }
        }
    }

    fun saveTreatment(treatment: Treatment, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (treatment.id == 0L) repository.insertTreatment(treatment)
            else repository.updateTreatment(treatment)
            onComplete()
        }
    }

    fun deleteTreatment(treatment: Treatment) {
        viewModelScope.launch { repository.deleteTreatment(treatment) }
    }
}
