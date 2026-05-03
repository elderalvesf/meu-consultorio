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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    "Implante dentário",
    "Prótese total",
    "Prótese parcial removível",
    "Coroa dentária",
    "Faceta de porcelana",
    "Clareamento dental",
    "Aparelho ortodôntico",
    "Contenção ortodôntica",
    "Cirurgia periodontal",
    "Raspagem e alisamento radicular",
    "Aplicação de flúor",
    "Selante de fóssulas",
    "Limpeza e profilaxia",
    "Outro"
)

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
    var procedure by remember { mutableStateOf("") }
    var tooth by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TreatmentStatus.EM_ANDAMENTO) }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showPatientDropdown by remember { mutableStateOf(false) }
    var showProcedureDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var patientError by remember { mutableStateOf(false) }
    var procedureError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    LaunchedEffect(treatmentId) {
        if (treatmentId != null) viewModel.loadTreatment(treatmentId)
    }

    LaunchedEffect(selectedTreatment) {
        selectedTreatment?.let { t ->
            if (treatmentId != null) {
                selectedPatientId = t.patientId
                procedure = t.procedure
                tooth = t.tooth
                description = t.description
                costText = if (t.cost > 0) t.cost.toString() else ""
                priceText = if (t.price > 0) t.price.toString() else ""
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
                    datePickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val formattedDate = "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')}/" +
            "${(cal.get(Calendar.MONTH)+1).toString().padStart(2,'0')}/${cal.get(Calendar.YEAR)}"

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (treatmentId != null) "Editar Tratamento" else "Novo Tratamento",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
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
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = showPatientDropdown, onExpandedChange = { showPatientDropdown = it }) {
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
                ExposedDropdownMenu(expanded = showPatientDropdown, onDismissRequest = { showPatientDropdown = false }) {
                    patients.forEach { patient ->
                        DropdownMenuItem(
                            text = { Text(patient.name) },
                            onClick = { selectedPatientId = patient.id; patientError = false; showPatientDropdown = false }
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    isError = procedureError,
                    supportingText = if (procedureError) ({ Text("Informe o procedimento") }) else null
                )
                ExposedDropdownMenu(expanded = showProcedureDropdown, onDismissRequest = { showProcedureDropdown = false }) {
                    dentalTreatments.forEach { proc ->
                        DropdownMenuItem(
                            text = { Text(proc) },
                            onClick = { procedure = proc; procedureError = false; showProcedureDropdown = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = tooth,
                onValueChange = { tooth = it },
                label = { Text("Dente(s)") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ex: 14, 15") },
                singleLine = true
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Valor (R$)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Custo (R$)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            Box {
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

            ExposedDropdownMenuBox(expanded = showStatusDropdown, onExpandedChange = { showStatusDropdown = it }) {
                OutlinedTextField(
                    value = status.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
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
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}
