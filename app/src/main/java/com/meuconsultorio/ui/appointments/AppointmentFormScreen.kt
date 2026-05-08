package com.meuconsultorio.ui.appointments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import kotlinx.coroutines.launch
import java.io.File
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
    "Retorno 10 dias DTM",
    "Retorno 15 dias DTM",
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
    val coroutineScope = rememberCoroutineScope()

    var selectedPatientId by remember { mutableStateOf(preselectedPatientId) }
    var procedureType by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(AppointmentStatus.AGENDADA) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var notes by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var attachmentPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAddingAttachment by remember { mutableStateOf(false) }
    var isPaid by remember { mutableStateOf(false) }
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
    var patientNameText by remember { mutableStateOf("") }
    var showProcedureDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNewPatientDialog by remember { mutableStateOf(false) }
    var newPatientName by remember { mutableStateOf("") }
    var newPatientPhone by remember { mutableStateOf("") }
    var newPatientNameError by remember { mutableStateOf(false) }
    var showNewProcedureDialog by remember { mutableStateOf(false) }
    var newProcedureName by remember { mutableStateOf("") }
    var newProcedureNameError by remember { mutableStateOf(false) }

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isAddingAttachment = true
                val path = viewModel.saveFileToInternalStorage(context, it, "image/*")
                if (path != null) attachmentPaths = attachmentPaths + path
                isAddingAttachment = false
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isAddingAttachment = true
                val path = viewModel.saveFileToInternalStorage(context, it, "application/pdf")
                if (path != null) attachmentPaths = attachmentPaths + path
                isAddingAttachment = false
            }
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

    LaunchedEffect(selectedAppointment, patients) {
        selectedAppointment?.let { appt ->
            if (appointmentId != null) {
                selectedPatientId = appt.patientId
                patientNameText = patients.find { it.id == appt.patientId }?.name ?: ""
                procedureType = appt.procedureType
                status = appt.status
                durationMinutes = appt.durationMinutes
                notes = appt.notes
                priceText = if (appt.price > 0) appt.price.toString() else ""
                isPaid = appt.isPaid
                attachmentPaths = appt.attachments.split(";").filter { it.isNotBlank() }
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
            price = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0,
            isPaid = isPaid,
            attachments = attachmentPaths.joinToString(";"),
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

    if (showNewPatientDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewPatientDialog = false
                newPatientName = ""; newPatientPhone = ""; newPatientNameError = false
            },
            title = { Text("Novo paciente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPatientName,
                        onValueChange = { newPatientName = it; newPatientNameError = false },
                        label = { Text("Nome *") },
                        isError = newPatientNameError,
                        supportingText = if (newPatientNameError) ({ Text("Nome obrigatório") }) else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPatientPhone,
                        onValueChange = { newPatientPhone = it },
                        label = { Text("Telefone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPatientName.isBlank()) {
                        newPatientNameError = true
                    } else {
                        val patient = Patient(name = newPatientName.trim(), phone = newPatientPhone.trim())
                        patientViewModel.savePatient(patient) { newId ->
                            selectedPatientId = newId
                            patientNameText = newPatientName.trim()
                            patientError = false
                        }
                        showNewPatientDialog = false
                        newPatientName = ""; newPatientPhone = ""; newPatientNameError = false
                    }
                }) { Text("Cadastrar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewPatientDialog = false
                    newPatientName = ""; newPatientPhone = ""; newPatientNameError = false
                }) { Text("Cancelar") }
            }
        )
    }

    if (showNewProcedureDialog) {
        AlertDialog(
            onDismissRequest = { showNewProcedureDialog = false; newProcedureName = ""; newProcedureNameError = false },
            title = { Text("Novo procedimento") },
            text = {
                OutlinedTextField(
                    value = newProcedureName,
                    onValueChange = { newProcedureName = it; newProcedureNameError = false },
                    label = { Text("Nome do procedimento *") },
                    isError = newProcedureNameError,
                    supportingText = if (newProcedureNameError) ({ Text("Nome obrigatório") }) else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newProcedureName.isBlank()) {
                        newProcedureNameError = true
                    } else {
                        procedureType = newProcedureName.trim()
                        procedureError = false
                        showNewProcedureDialog = false
                        newProcedureName = ""
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewProcedureDialog = false; newProcedureName = ""; newProcedureNameError = false
                }) { Text("Cancelar") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir consulta") },
            text = { Text("Deseja excluir esta consulta? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedAppointment?.let { viewModel.deleteAppointment(it) }
                    onBack()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (appointmentId != null) "Editar Consulta" else "Nova Consulta",
                onBack = onBack,
                actions = {
                    if (appointmentId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir consulta",
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
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
                    value = patientNameText,
                    onValueChange = {
                        patientNameText = it
                        selectedPatientId = null
                        showPatientDropdown = true
                    },
                    label = { Text("Paciente *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPatientDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = patientError,
                    supportingText = if (patientError) ({ Text("Selecione um paciente") }) else null,
                    placeholder = { Text("Buscar paciente...") }
                )
                ExposedDropdownMenu(
                    expanded = showPatientDropdown,
                    onDismissRequest = { showPatientDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("+ Novo paciente", color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showPatientDropdown = false
                            showNewPatientDialog = true
                        }
                    )
                    HorizontalDivider()
                    val filteredPatients = patients.filter {
                        patientNameText.isBlank() || it.name.contains(patientNameText, ignoreCase = true)
                    }
                    filteredPatients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name) },
                            onClick = {
                                selectedPatientId = patient.id
                                patientNameText = patient.name
                                patientError = false
                                showPatientDropdown = false
                            }
                        )
                    }
                    if (filteredPatients.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Nenhum paciente encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {}
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
                    onValueChange = {
                        procedureType = it
                        showProcedureDropdown = true
                    },
                    label = { Text("Procedimento *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProcedureDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = procedureError,
                    supportingText = if (procedureError) ({ Text("Informe o procedimento") }) else null,
                    placeholder = { Text("Buscar procedimento...") }
                )
                ExposedDropdownMenu(
                    expanded = showProcedureDropdown,
                    onDismissRequest = { showProcedureDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("+ Novo procedimento", color = MaterialTheme.colorScheme.primary) },
                        onClick = { showProcedureDropdown = false; showNewProcedureDialog = true }
                    )
                    HorizontalDivider()
                    val filteredProcedures = dentalProcedures.sorted().filter {
                        procedureType.isBlank() || it.contains(procedureType, ignoreCase = true)
                    }
                    filteredProcedures.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc) },
                            onClick = { procedureType = proc; procedureError = false; showProcedureDropdown = false }
                        )
                    }
                    if (filteredProcedures.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Nenhum procedimento encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {}
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationMinutes.toString(),
                    onValueChange = { it.toIntOrNull()?.let { v -> if (v in 10..480) durationMinutes = v } },
                    label = { Text("Duração (min)") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = { Text("min", style = MaterialTheme.typography.labelMedium) }
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Consulta paga", style = MaterialTheme.typography.bodyMedium)
                    Text("Valor refletido no financeiro ao marcar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isPaid, onCheckedChange = { isPaid = it })
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // Anexos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Anexos", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                attachmentPaths.forEach { path ->
                    val file = File(path)
                    val isPdf = path.endsWith(".pdf")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPdf) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1)
                            } else {
                                AsyncImage(
                                    model = Uri.fromFile(file),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1)
                            }
                            IconButton(onClick = {
                                file.takeIf { it.exists() }?.delete()
                                attachmentPaths = attachmentPaths - path
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remover",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (isAddingAttachment) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Imagem")
                    }
                    OutlinedButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF")
                    }
                }
            }

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
