package com.meuconsultorio.ui.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Turno
import com.meuconsultorio.data.entity.TurnoStatus
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.TurnoViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurnoFormScreen(
    turnoId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: TurnoViewModel = hiltViewModel()
) {
    val selectedTurno by viewModel.selectedTurno.collectAsState()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var valorText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TurnoStatus.PENDENTE) }
    var nameError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.MINUTE)
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().apply { timeInMillis = endDate ?: (date + 60 * 60_000L) }.get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().apply { timeInMillis = endDate ?: (date + 60 * 60_000L) }.get(Calendar.MINUTE)
    )

    LaunchedEffect(turnoId) {
        if (turnoId != null) viewModel.loadTurno(turnoId)
    }

    LaunchedEffect(selectedTurno) {
        selectedTurno?.let { t ->
            if (turnoId != null) {
                name = t.name
                description = t.description
                date = t.date
                endDate = t.endDate
                valorText = if (t.valor > 0) t.valor.toString() else ""
                status = t.status
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

    if (showEndTimePicker) {
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Horário de término") },
            text = { TimePicker(state = endTimePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, endTimePickerState.hour)
                        set(Calendar.MINUTE, endTimePickerState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    endDate = cal.timeInMillis
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    endDate = null
                    showEndTimePicker = false
                }) { Text("Remover") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir turno") },
            text = { Text("Deseja excluir este turno? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    selectedTurno?.let { viewModel.deleteTurno(it) }
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
    val formattedEndTime = endDate?.let {
        val c = Calendar.getInstance().apply { timeInMillis = it }
        "${c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${c.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    } ?: ""

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (turnoId != null) "Editar Turno" else "Novo Turno",
                onBack = onBack,
                actions = {
                    if (turnoId != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir turno",
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
                        val valor = valorText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val t = Turno(
                            id = turnoId ?: 0L,
                            name = name.trim(),
                            description = description.trim(),
                            date = date,
                            endDate = endDate,
                            valor = valor,
                            status = status
                        )
                        viewModel.saveTurno(t) { onSave() }
                    }
                },
                modifier = Modifier.semantics { contentDescription = "btn_salvar_turno" },
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
                .semantics { contentDescription = "turno_form_screen" },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Nome *") },
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "campo_nome_turno" },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Informe o nome do turno") }) else null,
                placeholder = { Text("ex: Plantão noturno") },
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
                        label = { Text("Início") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showTimePicker = true })
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f))
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formattedEndTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Término (opcional)") },
                        trailingIcon = { Icon(Icons.Filled.Schedule, null) },
                        placeholder = { Text("--:--") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { showEndTimePicker = true })
                }
            }

            OutlinedTextField(
                value = valorText,
                onValueChange = { valorText = it },
                label = { Text("Valor (R$)") },
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "campo_valor_turno" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("0,00") },
                leadingIcon = { Text("R$", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true
            )

            Text("Status do pagamento", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = status == TurnoStatus.PENDENTE,
                    onClick = { status = TurnoStatus.PENDENTE },
                    label = { Text(TurnoStatus.PENDENTE.label) },
                    leadingIcon = if (status == TurnoStatus.PENDENTE) {
                        { Icon(Icons.Filled.HourglassEmpty, null, Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFC107).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFF57C00)
                    )
                )
                FilterChip(
                    selected = status == TurnoStatus.CONFIRMADO,
                    onClick = { status = TurnoStatus.CONFIRMADO },
                    label = { Text(TurnoStatus.CONFIRMADO.label) },
                    leadingIcon = if (status == TurnoStatus.CONFIRMADO) {
                        { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF2E7D32)
                    )
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth().height(120.dp).semantics(mergeDescendants = true) { contentDescription = "campo_descricao_turno" },
                placeholder = { Text("Detalhes do turno (opcional)") },
                maxLines = 5
            )

            Spacer(Modifier.height(72.dp))
        }
    }
}
