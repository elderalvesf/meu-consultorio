package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val repository: PatientRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<Patient>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllPatients()
            else repository.searchPatients(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPatients: StateFlow<Int> = repository.getTotalPatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedPatient = MutableStateFlow<Patient?>(null)
    val selectedPatient: StateFlow<Patient?> = _selectedPatient.asStateFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun loadPatient(id: Long) {
        viewModelScope.launch {
            repository.getPatientById(id).collect { _selectedPatient.value = it }
        }
    }

    fun savePatient(patient: Patient, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (patient.id == 0L) repository.insertPatient(patient)
            else { repository.updatePatient(patient); patient.id }
            onComplete(id)
        }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch { repository.deletePatient(patient) }
    }
}
