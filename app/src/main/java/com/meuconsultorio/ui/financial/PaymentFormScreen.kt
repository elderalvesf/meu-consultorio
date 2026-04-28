package com.meuconsultorio.ui.financial

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
import com.meuconsultorio.data.entity.Payment
import com.meuconsultorio.data.entity.PaymentMethod
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.PaymentViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentFormScreen(
    paymentId: Long?,
    preselectedPatientId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val selectedPayment by viewModel.selectedPayment.collectAsState()
    val patients by patientViewModel.patients.collectAsState()

    var selectedPatientId by remember { mutableStateOf(preselectedPatientId) }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.PIX) }
    var isPaid by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var showPatientDropdown by remember { mutableStateOf(false) }
    var showMethodDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var patientError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    LaunchedEffect(paymentId) {
        if (paymentId != null) viewModel.loadPayment(paymentId)
    }

    LaunchedEffect(selectedPayment) {
        selectedPayment?.let { p ->
            if (paymentId != null) {
                selectedPatientId = p.patientId
                description = p.description
                amountText = p.amount.toString()
                method = p.method
                isPaid = p.isPaid
                notes = p.notes
                date = p.date
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
                title = if (paymentId != null) "Editar Pagamento" else "Novo Pagamento",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    patientError = selectedPatientId == null
                    descriptionError = description.isBlank()
                    val amount = amountText.replace(",", ".").toDoubleOrNull()
                    amountError = amount == null || amount <= 0
                    if (!patientError && !descriptionError && !amountError) {
                        val payment = Payment(
                            id = paymentId ?: 0L,
                            patientId = selectedPatientId!!,
                            description = description.trim(),
                            amount = amount!!,
                            method = method,
                            isPaid = isPaid,
                            notes = notes.trim(),
                            date = date
                        )
                        viewModel.savePayment(payment) { onSave() }
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; descriptionError = false },
                label = { Text("Descrição *") },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError,
                supportingText = if (descriptionError) ({ Text("Informe uma descrição") }) else null,
                placeholder = { Text("ex: Restauração dente 14") },
                singleLine = true
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; amountError = false },
                label = { Text("Valor (R$) *") },
                modifier = Modifier.fillMaxWidth(),
                isError = amountError,
                supportingText = if (amountError) ({ Text("Informe um valor válido") }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("R$") },
                singleLine = true
            )

            ExposedDropdownMenuBox(expanded = showMethodDropdown, onExpandedChange = { showMethodDropdown = it }) {
                OutlinedTextField(
                    value = method.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Forma de pagamento") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMethodDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = showMethodDropdown, onDismissRequest = { showMethodDropdown = false }) {
                    PaymentMethod.entries.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m.label) },
                            onClick = { method = m; showMethodDropdown = false }
                        )
                    }
                }
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

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(checked = isPaid, onCheckedChange = { isPaid = it })
                Spacer(Modifier.width(8.dp))
                Text(if (isPaid) "Pago" else "Pendente", style = MaterialTheme.typography.bodyLarge)
            }

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
