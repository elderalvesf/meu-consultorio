package com.meuconsultorio.ui.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.PatientViewModel

@Composable
fun PatientFormScreen(
    patientId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val selectedPatient by viewModel.selectedPatient.collectAsState()

    var name by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        if (patientId != null) viewModel.loadPatient(patientId)
    }

    LaunchedEffect(selectedPatient) {
        selectedPatient?.let { p ->
            if (patientId != null) {
                name = p.name
                cpf = p.cpf
                phone = p.phone
                email = p.email
                birthDate = p.birthDate
                address = p.address
                notes = p.notes
            }
        }
    }

    val isEditing = patientId != null
    val title = if (isEditing) "Editar Paciente" else "Novo Paciente"

    Scaffold(
        topBar = {
            AppTopBar(title = title, onBack = onBack)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    nameError = name.isBlank()
                    phoneError = phone.isBlank()
                    if (!nameError && !phoneError) {
                        val patient = Patient(
                            id = if (isEditing) patientId!! else 0L,
                            name = name.trim(),
                            cpf = cpf.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            birthDate = birthDate.trim(),
                            address = address.trim(),
                            notes = notes.trim()
                        )
                        viewModel.savePatient(patient) { onSave() }
                    }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = null) },
                text = { Text("Salvar") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nome completo *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) ({ Text("Nome é obrigatório") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            OutlinedTextField(
                value = cpf,
                onValueChange = { if (it.length <= 14) cpf = it },
                label = { Text("CPF") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("000.000.000-00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 15) phone = it; phoneError = false },
                label = { Text("Telefone / WhatsApp *") },
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError,
                supportingText = if (phoneError) ({ Text("Telefone é obrigatório") }) else null,
                placeholder = { Text("(00) 00000-0000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            OutlinedTextField(
                value = birthDate,
                onValueChange = { if (it.length <= 10) birthDate = it },
                label = { Text("Data de nascimento") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("dd/mm/aaaa") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Endereço") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 2
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 5
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}
