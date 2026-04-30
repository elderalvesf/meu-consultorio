package com.meuconsultorio.ui.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import java.util.*

val dentalProcedures = listOf(
    "Consulta de avaliação",
    "Limpeza e profilaxia",
    "Clareamento dental",
    "Restauração (resina)",
    "Restauração (amálgama)",
    "Extração simples",
    "Extração de siso",
    "Canal (endodontia)",
    "Prótese dentária",
    "Implante dentário",
    "Ortodontia (aparelho)",
    "Periodontia (gengiva)",
    "Radiografia",
    "Urgência / Dor",
    "Retorno",
    "Outro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentFormScreen(
    appointmentId: Long?,
    preselectedPatientId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val selectedAppointment by viewModel.selectedAppointment.collectAsState()
    val patients by patientViewModel.patients.collectAsState()

    var selectedPatientId by remember { mutableStateOf(preselectedPatientId) }
    var procedureType by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AppointmentStatus.AGENDADA) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var notes by remember { mutableStateOf("") }
    var dateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPatientDropdown by remember { mutableStateOf(false) }
    var showProcedureDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    var patientError by remember { mutableStateOf(false) }
    var procedureError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateTime)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = dateTime }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = dateTime }.get(Calendar.MINUTE)
    )

    LaunchedEffect(appointmentId) {
        if (appointmentId != null) viewModel.loadAppointment(appointmentId)
    }

    LaunchedEffect(selectedAppointment) {
        selectedAppointment?.let { appt ->
            if (appointmentId != null) {
                selectedPatientId = appt.patientId
                procedureType = appt.procedureType
                status = appt.status
                durationMinutes = appt.durationMinutes
                notes = appt.notes
                dateTime = appt.dateTime
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMs ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateTime
                            // selectedMs é UTC midnight — usar Calendar UTC para extrair dia correto
                            val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedMs }
                            set(Calendar.YEAR, selectedCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, selectedCal.get(Calendar.DAY_OF_MONTH))
                        }
                        dateTime = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Selecionar horário") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = dateTime
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    dateTime = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } }
        )
    }

    val cal = Calendar.getInstance().apply { timeInMillis = dateTime }
    val formattedDate = "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}/" +
            "${(cal.get(Calendar.MONTH)+1).toString().padStart(2,'0')}/${cal.get(Calendar.YEAR)}"
    val formattedTime = "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')}:" +
            "${cal.get(Calendar.MINUTE).toString().padStart(2,'0')}"

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (appointmentId != null) "Editar Consulta" else "Nova Consulta",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    patientError = selectedPatientId == null
                    procedureError = procedureType.isBlank()
                    if (!patientError && !procedureError) {
                        val appointment = Appointment(
                            id = appointmentId ?: 0L,
                            patientId = selectedPatientId!!,
                            dateTime = dateTime,
                            durationMinutes = durationMinutes,
                            procedureType = procedureType,
                            status = status,
                            notes = notes
                        )
                        viewModel.saveAppointment(appointment) { onSave() }
                    }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = null) },
                text = { Text("Salvar") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = showPatientDropdown,
                onExpandedChange = { showPatientDropdown = it }
            ) {
                OutlinedTextField(
                    value = patients.find { it.id == selectedPatientId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Paciente *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPatientDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = patientError,
                    supportingText = if (patientError) ({ Text("Selecione um paciente") }) else null
                )
                ExposedDropdownMenu(
                    expanded = showPatientDropdown,
                    onDismissRequest = { showPatientDropdown = false }
                ) {
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name) },
                            onClick = {
                                selectedPatientId = patient.id
                                patientError = false
                                showPatientDropdown = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = showProcedureDropdown,
                onExpandedChange = { showProcedureDropdown = it }
            ) {
                OutlinedTextField(
                    value = procedureType,
                    onValueChange = { procedureType = it; procedureError = false },
                    label = { Text("Procedimento *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProcedureDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = procedureError,
                    supportingText = if (procedureError) ({ Text("Informe o procedimento") }) else null
                )
                ExposedDropdownMenu(
                    expanded = showProcedureDropdown,
                    onDismissRequest = { showProcedureDropdown = false }
                ) {
                    dentalProcedures.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc) },
                            onClick = { procedureType = proc; procedureError = false; showProcedureDropdown = false }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formattedDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data") },
                        trailingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showDatePicker = true })
                }
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formattedTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horário") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showTimePicker = true })
                }
            }

            ExposedDropdownMenuBox(
                expanded = showStatusDropdown,
                onExpandedChange = { showStatusDropdown = it }
            ) {
                OutlinedTextField(
                    value = status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showStatusDropdown,
                    onDismissRequest = { showStatusDropdown = false }
                ) {
                    AppointmentStatus.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { status = s; showStatusDropdown = false })
                    }
                }
            }

            OutlinedTextField(
                value = durationMinutes.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> if (v in 10..480) durationMinutes = v } },
                label = { Text("Duração (minutos)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Text("min", style = MaterialTheme.typography.labelMedium) }
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}

