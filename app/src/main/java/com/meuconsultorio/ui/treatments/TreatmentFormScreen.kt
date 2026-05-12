package com.meuconsultorio.ui.treatments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.entity.TreatmentStatus
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel
import java.util.*

val dentalTreatments = listOf(
    "Restauração resina composta",
    "Restauração amálgama",
    "Tratamento de canal",
    "Extração dentária",
    "Extração de siso",
    "Instalação de implantes dentários",
    "Prótese total",
    "Prótese parcial removível",
    "Coroa dentária",
    "Faceta de porcelana",
    "Clareamento dental",
    "Instalação aparelho ortodôntico",
    "Manutenção aparelho ortodôntico",
    "Remoção de sutura",
    "Acompanhamento pós-operatório",
    "Manutenção de PSI",
    "Moldagem para restauração semidireta",
    "Cimentação de restauração semidireta",
    "Botóx",
    "Retorno 15 dias Botóx",
    "Preenchimento Labial",
    "Retorno 15 dias preenchimento Labial",
    "Sessão Laserterapia",
    "Frenectomia",
    "Extração de supranumerário",
    "Extração de siso incluso/impactado",
    "Tracionamento de dente incluso",
    "Remoção de mucocele",
    "Biópsia de tecidos moles",
    "Biópsia de lesão óssea",
    "Cirurgia parendodôntica",
    "Levantamento de seio maxilar",
    "Urgência",
    "Contenção ortodôntica",
    "Cirurgia periodontal",
    "Raspagem e alisamento radicular",
    "Aplicação de flúor",
    "Selante de fóssulas",
    "Limpeza e profilaxia",
    "Placa Miorrelaxante",
    "Protocolo DTM - 3 sessões laser + placa",
    "Retorno 10 dias DTM",
    "Retorno 15 dias DTM",
    "Outro"
)

val proceduresWithSessions = setOf("Sessão Laserterapia")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentFormScreen(
    treatmentId: Long?,
    preselectedPatientId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: TreatmentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val selectedTreatment by viewModel.selectedTreatment.collectAsState()
    val patients by patientViewModel.patients.collectAsState()

    var selectedPatientId by remember { mutableStateOf(preselectedPatientId) }
    var patientNameText by remember { mutableStateOf("") }
    var procedure by remember { mutableStateOf("") }
    var tooth by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var sessionsText by remember { mutableStateOf("") }
    var isLaserterapia by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(TreatmentStatus.EM_ANDAMENTO) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showPatientDropdown by remember { mutableStateOf(false) }
    var showProcedureDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showNewProcedureDialog by remember { mutableStateOf(false) }
    var newProcedureName by remember { mutableStateOf("") }
    var newProcedureNameError by remember { mutableStateOf(false) }

    var patientError by remember { mutableStateOf(false) }
    var procedureError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MINUTE)
    )

    LaunchedEffect(treatmentId) {
        if (treatmentId != null) viewModel.loadTreatment(treatmentId)
    }

    LaunchedEffect(selectedTreatment, patients) {
        selectedTreatment?.let { t ->
            if (treatmentId != null) {
                selectedPatientId = t.patientId
                patientNameText = patients.find { it.id == t.patientId }?.name ?: ""
                procedure = t.procedure
                tooth = t.tooth
                description = t.description
                costText = if (t.cost > 0) t.cost.toString() else ""
                priceText = if (t.price > 0) t.price.toString() else ""
                sessionsText = if (t.sessions > 0) t.sessions.toString() else ""
                isLaserterapia = t.procedure in proceduresWithSessions
                status = t.status
                date = t.date
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        val localCal = Calendar.getInstance().apply {
                            timeInMillis = date
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        }
                        date = localCal.timeInMillis
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
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    date = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") } }
        )
    }

    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val formattedDate = "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}/" +
            "${(cal.get(Calendar.MONTH)+1).toString().padStart(2,'0')}/${cal.get(Calendar.YEAR)}"
    val formattedTime = "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')}:" +
            "${cal.get(Calendar.MINUTE).toString().padStart(2,'0')}"

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
                        procedure = newProcedureName.trim()
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

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (treatmentId != null) "Editar Tratamento" else "Novo Tratamento",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.semantics { contentDescription = "btn_salvar_tratamento" },
                onClick = {
                    patientError = selectedPatientId == null
                    procedureError = procedure.isBlank()
                    if (!patientError && !procedureError) {
                        val treatment = Treatment(
                            id = treatmentId ?: 0L,
                            patientId = selectedPatientId!!,
                            procedure = procedure,
                            tooth = tooth.trim(),
                            description = description.trim(),
                            cost = costText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            price = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            sessions = if (isLaserterapia) sessionsText.toIntOrNull() ?: 0 else 0,
                            date = date,
                            status = status
                        )
                        viewModel.saveTreatment(treatment) { onSave() }
                    }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = null) },
                text = { Text("Salvar") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
                .semantics { contentDescription = "tratamento_form_screen" },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = showPatientDropdown, onExpandedChange = { showPatientDropdown = it }) {
                OutlinedTextField(
                    value = patientNameText,
                    onValueChange = { patientNameText = it; selectedPatientId = null },
                    label = { Text("Paciente *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPatientDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().semantics(mergeDescendants = true) { contentDescription = "campo_paciente_tratamento" },
                    isError = patientError,
                    supportingText = if (patientError) ({ Text("Selecione um paciente") }) else null,
                    placeholder = { Text("Buscar paciente...") }
                )
                ExposedDropdownMenu(expanded = showPatientDropdown, onDismissRequest = { showPatientDropdown = false }) {
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

            ExposedDropdownMenuBox(expanded = showProcedureDropdown, onExpandedChange = { showProcedureDropdown = it }) {
                OutlinedTextField(
                    value = procedure,
                    onValueChange = { procedure = it; procedureError = false },
                    label = { Text("Procedimento *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProcedureDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().semantics(mergeDescendants = true) { contentDescription = "campo_procedimento_tratamento" },
                    isError = procedureError,
                    supportingText = if (procedureError) ({ Text("Informe o procedimento") }) else null,
                    placeholder = { Text("Buscar procedimento...") }
                )
                ExposedDropdownMenu(expanded = showProcedureDropdown, onDismissRequest = { showProcedureDropdown = false }) {
                    DropdownMenuItem(
                        text = { Text("+ Novo procedimento", color = MaterialTheme.colorScheme.primary) },
                        onClick = { showProcedureDropdown = false; showNewProcedureDialog = true }
                    )
                    HorizontalDivider()
                    val filteredProcedures = dentalTreatments.sorted().filter {
                        procedure.isBlank() || it.contains(procedure, ignoreCase = true)
                    }
                    filteredProcedures.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc) },
                            onClick = {
                                procedure = proc
                                isLaserterapia = proc in proceduresWithSessions
                                if (!isLaserterapia) sessionsText = ""
                                procedureError = false
                                showProcedureDropdown = false
                            }
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

            OutlinedTextField(
                value = tooth,
                onValueChange = { tooth = it },
                label = { Text("Dente(s)") },
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "campo_dente" },
                placeholder = { Text("ex: 14, 15") },
                singleLine = true
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "campo_valor_tratamento" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Custo (R$)") },
                    modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "campo_custo_tratamento" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            if (isLaserterapia) {
                OutlinedTextField(
                    value = sessionsText,
                    onValueChange = { sessionsText = it },
                    label = { Text("Quantidade de sessões") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
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
                    Box(Modifier.matchParentSize().clickable { showDatePicker = true }.semantics(mergeDescendants = true) { contentDescription = "campo_data_tratamento" })
                }
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formattedTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horário") },
                        trailingIcon = { Icon(Icons.Filled.AccessTime, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showTimePicker = true }.semantics(mergeDescendants = true) { contentDescription = "campo_horario_tratamento" })
                }
            }

            ExposedDropdownMenuBox(expanded = showStatusDropdown, onExpandedChange = { showStatusDropdown = it }) {
                OutlinedTextField(
                    value = status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor().semantics(mergeDescendants = true) { contentDescription = "campo_status_tratamento" }
                )
                ExposedDropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                    TreatmentStatus.entries.forEach { s ->
                        DropdownMenuItem(text = { Text(s.label) }, onClick = { status = s; showStatusDropdown = false })
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição / Observações") },
                modifier = Modifier.fillMaxWidth().height(100.dp).semantics(mergeDescendants = true) { contentDescription = "campo_descricao_tratamento" },
                maxLines = 4
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}
