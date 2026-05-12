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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Compromisso
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.CompromissoViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompromissoFormScreen(
    compromissoId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: CompromissoViewModel = hiltViewModel()
) {
    val selectedCompromisso by viewModel.selectedCompromisso.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MINUTE)
    )

    LaunchedEffect(compromissoId) {
        if (compromissoId != null) viewModel.loadCompromisso(compromissoId)
    }

    LaunchedEffect(selectedCompromisso) {
        selectedCompromisso?.let { c ->
            if (compromissoId != null) {
                name = c.name
                description = c.description
                date = c.date
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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir compromisso") },
            text = { Text("Deseja excluir este compromisso? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedCompromisso?.let { viewModel.deleteCompromisso(it) }
                    onBack()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    val cal = Calendar.getInstance().apply { timeInMillis = date }
    val formattedDate = "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}/" +
            "${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}/${cal.get(Calendar.YEAR)}"
    val formattedTime = "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:" +
            "${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (compromissoId != null) "Editar Compromisso" else "Novo Compromisso",
                onBack = onBack,
                actions = {
                    if (compromissoId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir compromisso",
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    nameError = name.isBlank()
                    if (!nameError) {
                        val c = Compromisso(
                            id = compromissoId ?: 0L,
                            name = name.trim(),
                            description = description.trim(),
                            date = date
                        )
                        viewModel.saveCompromisso(c) { onSave() }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "btn_salvar_compromisso" },
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
                .padding(16.dp)
                .semantics { contentDescription = "compromisso_form_screen" },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nome *") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "campo_nome_compromisso" },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Informe o nome do compromisso") }) else null,
                placeholder = { Text("ex: Reunião com fornecedor") },
                singleLine = true
            )

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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = "campo_descricao_compromisso" },
                placeholder = { Text("Detalhes do compromisso (opcional)") },
                maxLines = 5
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}
