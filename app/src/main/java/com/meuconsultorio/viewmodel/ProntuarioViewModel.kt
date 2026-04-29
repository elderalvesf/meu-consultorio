package com.meuconsultorio.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meuconsultorio.data.entity.ProntuarioEntry
import com.meuconsultorio.data.repository.ProntuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ProntuarioViewModel @Inject constructor(
    private val repository: ProntuarioRepository
) : ViewModel() {

    private val _patientEntries = MutableStateFlow<List<ProntuarioEntry>>(emptyList())
    val patientEntries: StateFlow<List<ProntuarioEntry>> = _patientEntries.asStateFlow()

    private val _selectedEntry = MutableStateFlow<ProntuarioEntry?>(null)
    val selectedEntry: StateFlow<ProntuarioEntry?> = _selectedEntry.asStateFlow()

    fun loadPatientEntries(patientId: Long) {
        viewModelScope.launch {
            repository.getEntriesByPatient(patientId).collect { _patientEntries.value = it }
        }
    }

    fun loadEntry(id: Long) {
        viewModelScope.launch {
            repository.getEntryById(id).collect { _selectedEntry.value = it }
        }
    }

    fun saveEntry(entry: ProntuarioEntry, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            if (entry.id == 0L) repository.insertEntry(entry)
            else repository.updateEntry(entry)
            onComplete()
        }
    }

    fun deleteEntry(entry: ProntuarioEntry) {
        viewModelScope.launch {
            entry.imagePath?.let { path -> File(path).takeIf { it.exists() }?.delete() }
            repository.deleteEntry(entry)
        }
    }

    suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "prontuario").apply { mkdirs() }
                val destFile = File(dir, "prontuario_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                destFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
}
