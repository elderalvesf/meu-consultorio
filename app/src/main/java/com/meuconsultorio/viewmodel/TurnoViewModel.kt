package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Turno
import com.meuconsultorio.data.entity.TurnoStatus
import com.meuconsultorio.data.repository.TurnoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TurnoViewModel @Inject constructor(
    private val repository: TurnoRepository
) : ViewModel() {

    val allTurnos: StateFlow<List<Turno>> = repository.getAllTurnos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTurnoConfirmado: StateFlow<Double> = allTurnos
        .map { list -> list.filter { it.status == TurnoStatus.CONFIRMADO }.sumOf { it.valor } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedTurno = MutableStateFlow<Turno?>(null)
    val selectedTurno: StateFlow<Turno?> = _selectedTurno.asStateFlow()

    fun loadTurno(id: Long) {
        viewModelScope.launch {
            repository.getTurnoById(id).collect { _selectedTurno.value = it }
        }
    }

    fun saveTurno(turno: Turno, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (turno.id == 0L) repository.insertTurno(turno)
            else repository.updateTurno(turno)
            onComplete()
        }
    }

    fun deleteTurno(turno: Turno) {
        viewModelScope.launch { repository.deleteTurno(turno) }
    }
}
