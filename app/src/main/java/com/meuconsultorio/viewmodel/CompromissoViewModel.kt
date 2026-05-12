package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.Compromisso
import com.meuconsultorio.data.repository.CompromissoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompromissoViewModel @Inject constructor(
    private val repository: CompromissoRepository
) : ViewModel() {

    val allCompromissos: StateFlow<List<Compromisso>> = repository.getAllCompromissos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCompromisso = MutableStateFlow<Compromisso?>(null)
    val selectedCompromisso: StateFlow<Compromisso?> = _selectedCompromisso.asStateFlow()

    fun loadCompromisso(id: Long) {
        viewModelScope.launch {
            repository.getCompromissoById(id).collect { _selectedCompromisso.value = it }
        }
    }

    fun saveCompromisso(compromisso: Compromisso, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (compromisso.id == 0L) repository.insertCompromisso(compromisso)
            else repository.updateCompromisso(compromisso)
            onComplete()
        }
    }

    fun deleteCompromisso(compromisso: Compromisso) {
        viewModelScope.launch { repository.deleteCompromisso(compromisso) }
    }
}
