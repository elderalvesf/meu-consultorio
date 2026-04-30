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
import Patient
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class ViewMode { DIA, SEMANA }

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
    val weekStart by viewModel.weekStart.collectAsState()
    val weekAppointments by viewModel.weekAppointments.collectAsState()

    var viewMode by remember { mutableStateOf(ViewMode.DIA) }
    var filterStatus by remember { mutableStateOf<AppointmentStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        val localCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.selectDate(localCal.timeInMillis)
                    }
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

    val patientMap = patients.associateBy { it.id }

    Scaffold(
        topBar = { AppTopBar(title = "Agenda") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAppointment) {
                Icon(Icons.Filled.Add, contentDescription = "Nova consulta")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // View mode toggle
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    onClick = { viewMode = ViewMode.DIA },
                    selected = viewMode == ViewMode.DIA,
                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                ) { Text("Dia") }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    onClick = { viewMode = ViewMode.SEMANA },
                    selected = viewMode == ViewMode.SEMANA,
                    icon = { SegmentedButtonDefaults.ActiveIcon() }
                ) { Text("Semana") }
            }

            if (viewMode == ViewMode.DIA) {
                DayView(
                    selectedDate = selectedDate,
                    appointments = todayAppointments,
                    patientMap = patientMap,
                    filterStatus = filterStatus,
                    onFilterStatus = { filterStatus = it },
                    onShowDatePicker = { showDatePicker = true },
                    onEditAppointment = onEditAppointment,
                    onPatientClick = onPatientClick,
                    onStatusChange = { appt, status -> viewModel.updateStatus(appt, status) }
                )
            } else {
                WeekView(
                    weekStart = weekStart,
                    weekAppointments = weekAppointments,
                    patientMap = patientMap,
                    onPreviousWeek = { viewModel.previousWeek() },
                    onNextWeek = { viewModel.nextWeek() },
                    onEditAppointment = onEditAppointment,
                    onDayClick = { dayMillis ->
                        viewModel.selectDate(dayMillis)
                        viewMode = ViewMode.DIA
                    }
                )
            }
        }
    }
}

@Composable
private fun DayView(
    selectedDate: Long,
    appointments: List<Appointment>,
    patientMap: Map<Long, Patient>,
    filterStatus: AppointmentStatus?,
    onFilterStatus: (AppointmentStatus?) -> Unit,
    onShowDatePicker: () -> Unit,
    onEditAppointment: (Long) -> Unit,
    onPatientClick: (Long) -> Unit,
    onStatusChange: (Appointment, AppointmentStatus) -> Unit
) {
    val displayedAppointments = if (filterStatus != null)
        appointments.filter { it.status == filterStatus } else appointments
    val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(selectedDate))

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                Text(formattedDate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${displayedAppointments.size} consulta(s)", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onShowDatePicker) {
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
        Tab(selected = filterStatus == null, onClick = { onFilterStatus(null) },
            text = { Text("Todas") })
        AppointmentStatus.entries.forEach { status ->
            Tab(
                selected = filterStatus == status,
                onClick = { onFilterStatus(if (filterStatus == status) null else status) },
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
                    onStatusChange = { status -> onStatusChange(appointment, status) }
                )
            }
        }
    }
}

@Composable
private fun WeekView(
    weekStart: Long,
    weekAppointments: List<Appointment>,
    patientMap: Map<Long, Patient>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEditAppointment: (Long) -> Unit,
    onDayClick: (Long) -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
    val weekEnd = weekStart + 6L * 24 * 60 * 60 * 1000
    val weekLabel = "${fmt.format(Date(weekStart))} – ${fmt.format(Date(weekEnd))}"

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Semana anterior")
        }
        Text(weekLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNextWeek) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Próxima semana")
        }
    }

    val days = (0..6).map { weekStart + it * 24L * 60 * 60 * 1000 }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { dayMillis ->
            val dayAppointments = weekAppointments.filter { isSameDay(it.dateTime, dayMillis) }
                .sortedBy { it.dateTime }
            DayCard(
                dayMillis = dayMillis,
                appointments = dayAppointments,
                patientMap = patientMap,
                onEditAppointment = onEditAppointment,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
private fun DayCard(
    dayMillis: Long,
    appointments: List<Appointment>,
    patientMap: Map<Long, Patient>,
    onEditAppointment: (Long) -> Unit,
    onDayClick: (Long) -> Unit
) {
    val isToday = isSameDay(dayMillis, System.currentTimeMillis())
    val dayName = SimpleDateFormat("EEE", Locale("pt", "BR")).format(Date(dayMillis))
        .replaceFirstChar { it.uppercase() }
    val dayDate = SimpleDateFormat("dd/MM", Locale("pt", "BR")).format(Date(dayMillis))

    val headerColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        appointments.isNotEmpty() -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onDayClick(dayMillis) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Surface(color = headerColor, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$dayName, $dayDate",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.weight(1f))
                    if (appointments.isNotEmpty()) {
                        Text("${appointments.size} consulta(s)", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (appointments.isEmpty()) {
                Text(
                    "Sem consultas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            } else {
                appointments.forEach { appointment ->
                    val patient = patientMap[appointment.patientId]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onEditAppointment(appointment.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            appointment.dateTime.toFormattedTime(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(48.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(patient?.name ?: "Desconhecido", style = MaterialTheme.typography.bodySmall)
                            Text(appointment.procedureType, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor(appointment.status)
                        ) {
                            Text(
                                appointment.status.label,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
}

private fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
