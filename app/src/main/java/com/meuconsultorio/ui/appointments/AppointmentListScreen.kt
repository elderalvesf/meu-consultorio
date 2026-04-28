package com.meuconsultorio.ui.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    onAddAppointment: () -> Unit,
    onEditAppointment: (Long) -> Unit,
    onPatientClick: (Long) -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val patients by patientViewModel.patients.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayAppointments by viewModel.todayAppointments.collectAsState()

    var filterStatus by remember { mutableStateOf<AppointmentStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.selectDate(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val displayedAppointments = todayAppointments.let { list ->
        if (filterStatus != null) list.filter { it.status == filterStatus } else list
    }

    val patientMap = patients.associateBy { it.id }
    val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(selectedDate))

    Scaffold(
        topBar = { AppTopBar(title = "Agenda") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAppointment) {
                Icon(Icons.Filled.Add, contentDescription = "Nova consulta")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Data selecionada", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Text(formattedDate, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("${displayedAppointments.size} consulta(s)", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Selecionar data",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = AppointmentStatus.entries.indexOfFirst { it == filterStatus }.let {
                    if (it == -1) 0 else it + 1
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = filterStatus == null, onClick = { filterStatus = null },
                    text = { Text("Todas") })
                AppointmentStatus.entries.forEach { status ->
                    Tab(
                        selected = filterStatus == status,
                        onClick = { filterStatus = if (filterStatus == status) null else status },
                        text = { Text(status.label) }
                    )
                }
            }

            if (displayedAppointments.isEmpty()) {
                EmptyState("Nenhuma consulta para esta data")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedAppointments, key = { it.id }) { appointment ->
                        val patient = patientMap[appointment.patientId]
                        AppointmentCard(
                            appointment = appointment,
                            patientName = patient?.name ?: "Paciente desconhecido",
                            onEdit = { onEditAppointment(appointment.id) },
                            onPatientClick = { patient?.let { onPatientClick(it.id) } },
                            onStatusChange = { status -> viewModel.updateStatus(appointment, status) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    patientName: String,
    onEdit: () -> Unit,
    onPatientClick: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(patientName, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable(onClick = onPatientClick))
                    Text(appointment.procedureType, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor(appointment.status),
                        modifier = Modifier.clickable { showStatusMenu = true }
                    ) {
                        Text(appointment.status.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface)
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        AppointmentStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = { onStatusChange(status); showStatusMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(appointment.dateTime.toFormattedTime(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${appointment.durationMinutes} min", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                }
            }

            if (appointment.notes.isNotBlank()) {
                Text(appointment.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun statusColor(status: AppointmentStatus) = when (status) {
    AppointmentStatus.AGENDADA -> MaterialTheme.colorScheme.primary
    AppointmentStatus.CONFIRMADA -> MaterialTheme.colorScheme.secondary
    AppointmentStatus.CONCLUIDA -> MaterialTheme.colorScheme.tertiary
    AppointmentStatus.CANCELADA -> MaterialTheme.colorScheme.error
    AppointmentStatus.NAO_COMPARECEU -> MaterialTheme.colorScheme.errorContainer
}
