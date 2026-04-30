package com.meuconsultorio.ui.appointments

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current

    var selectedPatientId by remember { mutableStateOf(preselectedPatientId) }
    var procedureType by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AppointmentStatus.AGENDADA) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var notes by remember { mutableStateOf("") }
    var dateTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var existingCalendarEventId by remember { mutableLongStateOf(-1L) }

    var syncWithCalendar by remember { mutableStateOf(false) }
    var syncResultMessage by remember { mutableStateOf<String?>(null) }
    var syncResultSuccess by remember { mutableStateOf(false) }
    var showSyncResultDialog by remember { mutableStateOf(false) }
    var pendingNavigateOnDismiss by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPatientDropdown by remember { mutableStateOf(false) }
    var showProcedureDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    var patientError by remember { mutableStateOf(false) }
    var procedureError by remember { mutableStateOf(false) }

    // Guarda o appointment montado para usar após pedido de permissão
    var pendingAppointment by remember { mutableStateOf<Appointment?>(null) }
    var pendingSavedId by remember { mutableLongStateOf(-1L) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                      permissions[Manifest.permission.WRITE_CALENDAR] == true
        val appt = pendingAppointment
        if (granted && appt != null && pendingSavedId > 0L) {
            val patientName = patients.find { it.id == appt.patientId }?.name ?: ""
            isSyncing = true
            viewModel.syncWithCalendar(appt.copy(id = pendingSavedId), patientName) { success, msg ->
                isSyncing = false
                syncResultSuccess = success
                syncResultMessage = msg
                pendingNavigateOnDismiss = success
                showSyncResultDialog = true
            }
        } else {
            syncResultSuccess = false
            syncResultMessage = "Permissão de calendário negada. A consulta foi salva, mas não foi adicionada ao Google Calendar."
            pendingNavigateOnDismiss = true
            showSyncResultDialog = true
        }
    }

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
                existingCalendarEventId = appt.calendarEventId
                syncWithCalendar = appt.calendarEventId > 0L
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

    fun handleSave() {
        patientError = selectedPatientId == null
        procedureError = procedureType.isBlank()
        if (patientError || procedureError) return

        val appointment = Appointment(
            id = appointmentId ?: 0L,
            patientId = selectedPatientId!!,
            dateTime = dateTime,
            durationMinutes = durationMinutes,
            procedureType = procedureType,
            status = status,
            notes = notes,
            calendarEventId = existingCalendarEventId
        )
        val patientName = patients.find { it.id == selectedPatientId }?.name ?: ""

        viewModel.saveAppointment(appointment) { savedId ->
            val savedAppointment = appointment.copy(id = savedId)

            when {
                syncWithCalendar -> {
                    val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    if (hasRead && hasWrite) {
                        isSyncing = true
                        viewModel.syncWithCalendar(savedAppointment, patientName) { success, msg ->
                            isSyncing = false
                            syncResultSuccess = success
                            syncResultMessage = msg
                            pendingNavigateOnDismiss = success
                            showSyncResultDialog = true
                        }
                    } else {
                        pendingAppointment = savedAppointment
                        pendingSavedId = savedId
                        calendarPermissionLauncher.launch(
                            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                        )
                    }
                }
                !syncWithCalendar && existingCalendarEventId > 0L -> {
                    viewModel.unsyncFromCalendar(savedAppointment) { onSave() }
                }
                else -> onSave()
            }
        }
    }

    if (showSyncResultDialog) {
        AlertDialog(
            onDismissRequest = {
                showSyncResultDialog = false
                if (pendingNavigateOnDismiss) onSave()
            },
            icon = {
                Icon(
                    if (syncResultSuccess) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (syncResultSuccess) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
            },
            title = { Text(if (syncResultSuccess) "Google Calendar" else "Erro na sincronização") },
            text = { Text(syncResultMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    showSyncResultDialog = false
                    if (pendingNavigateOnDismiss) onSave()
                }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (appointmentId != null) "Editar Consulta" else "Nova Consulta",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!isSyncing) handleSave() },
                icon = {
                    if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Save, contentDescription = null)
                },
                text = { Text(if (isSyncing) "Sincronizando..." else "Salvar") }
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

            // Card de sincronização com Google Calendar
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (syncWithCalendar)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = if (syncWithCalendar)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Google Calendar",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (syncWithCalendar && existingCalendarEventId > 0L)
                                "Sincronizado — será atualizado ao salvar"
                            else if (syncWithCalendar)
                                "Será adicionado ao salvar"
                            else
                                "Não sincronizado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = syncWithCalendar,
                        onCheckedChange = { syncWithCalendar = it }
                    )
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}
