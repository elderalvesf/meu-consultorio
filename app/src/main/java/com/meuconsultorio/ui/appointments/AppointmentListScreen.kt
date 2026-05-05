package com.meuconsultorio.ui.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.entity.Patient
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

    LaunchedEffect(Unit) { viewModel.pullFromCalendar() }

    var weeklyView by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf<AppointmentStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val appointmentsByDay = remember(allAppointments) {
        allAppointments.groupBy { dayKeyFmt.format(Date(it.dateTime)) }
            .mapValues { it.value.size }
    }

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
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
    }

    val displayedAppointments = todayAppointments.let { list ->
        if (filterStatus != null) list.filter { it.status == filterStatus } else list
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

            // Toggle Dia / Semana
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = !weeklyView,
                        onClick = { weeklyView = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Dia") }
                    )
                    SegmentedButton(
                        selected = weeklyView,
                        onClick = { weeklyView = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Semana") }
                    )
                }
            }

            if (weeklyView) {
                WeekStrip(
                    selectedDate = selectedDate,
                    appointmentsByDay = appointmentsByDay,
                    onDaySelected = { viewModel.selectDate(it) },
                    onPrevWeek = {
                        viewModel.selectDate(Calendar.getInstance().apply {
                            timeInMillis = selectedDate; add(Calendar.DAY_OF_MONTH, -7)
                        }.timeInMillis)
                    },
                    onNextWeek = {
                        viewModel.selectDate(Calendar.getInstance().apply {
                            timeInMillis = selectedDate; add(Calendar.DAY_OF_MONTH, 7)
                        }.timeInMillis)
                    }
                )
                val dayHeaderFmt = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
                Text(
                    dayHeaderFmt.format(Date(selectedDate)).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            } else {
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
                            Text("Data selecionada",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(selectedDate.toFormattedDate(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Text("${displayedAppointments.size} consulta(s)",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Selecionar data",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Status filter — only for weekly list view
            if (weeklyView) {
                ScrollableTabRow(
                    selectedTabIndex = AppointmentStatus.entries.indexOfFirst { it == filterStatus }
                        .let { if (it == -1) 0 else it + 1 },
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
            }

            DayTimelineView(
                appointments = displayedAppointments,
                patientMap = patientMap,
                selectedDate = selectedDate,
                onEdit = onEditAppointment,
                onDelete = { viewModel.deleteAppointment(it) },
                onPatientClick = onPatientClick,
                onStatusChange = { appt, status -> viewModel.updateStatus(appt, status) }
            )
        }
    }
}

// ─── Timeline (Day View) ───────────────────────────────────────────────────────

private const val START_HOUR = 6
private const val END_HOUR = 22

@Composable
fun DayTimelineView(
    appointments: List<Appointment>,
    patientMap: Map<Long, Patient>,
    selectedDate: Long,
    onEdit: (Long) -> Unit,
    onDelete: (Appointment) -> Unit,
    onPatientClick: (Long) -> Unit,
    onStatusChange: (Appointment, AppointmentStatus) -> Unit
) {
    val hourHeight = 64.dp
    val totalHours = END_HOUR - START_HOUR + 1
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val dayFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val isToday = dayFmt.format(Date(selectedDate)) == dayFmt.format(Date())
    val now = remember { Calendar.getInstance() }
    val currentMinutesFromStart = (now.get(Calendar.HOUR_OF_DAY) - START_HOUR) * 60 + now.get(Calendar.MINUTE)

    LaunchedEffect(appointments, selectedDate) {
        val targetHour = if (appointments.isNotEmpty()) {
            Calendar.getInstance().apply { timeInMillis = appointments.minBy { it.dateTime }.dateTime }
                .get(Calendar.HOUR_OF_DAY) - 1
        } else if (isToday) {
            now.get(Calendar.HOUR_OF_DAY) - 1
        } else {
            8
        }
        val scrollHour = (targetHour - START_HOUR).coerceIn(0, totalHours - 1)
        val px = with(density) { (hourHeight * scrollHour).toPx().toInt() }
        scrollState.animateScrollTo(px)
    }

    Row(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Hour labels
        Column(Modifier.width(52.dp)) {
            repeat(totalHours) { i ->
                Box(Modifier.height(hourHeight)) {
                    Text(
                        "${(START_HOUR + i).toString().padStart(2, '0')}:00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 8.dp, top = 2.dp)
                    )
                }
            }
        }

        // Timeline area
        Box(
            Modifier
                .weight(1f)
                .height(hourHeight * totalHours)
        ) {
            // Hour dividers
            repeat(totalHours) { i ->
                HorizontalDivider(
                    Modifier.offset(y = hourHeight * i),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Half-hour dividers (lighter)
            repeat(totalHours) { i ->
                HorizontalDivider(
                    Modifier.offset(y = hourHeight * i + hourHeight / 2),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
            }

            // Current time indicator
            if (isToday && currentMinutesFromStart >= 0) {
                val indicatorOffset = hourHeight * (currentMinutesFromStart / 60f)
                Row(
                    Modifier
                        .offset(y = indicatorOffset)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    HorizontalDivider(color = Color.Red, thickness = 1.5.dp)
                }
            }

            // Appointment blocks
            appointments.forEach { appointment ->
                val cal = Calendar.getInstance().apply { timeInMillis = appointment.dateTime }
                val minutesFromStart = (cal.get(Calendar.HOUR_OF_DAY) - START_HOUR) * 60 + cal.get(Calendar.MINUTE)
                val topOffset = hourHeight * (minutesFromStart.toFloat() / 60f)
                val blockHeight = (hourHeight * (appointment.durationMinutes.toFloat() / 60f)).coerceAtLeast(40.dp)

                AppointmentTimelineItem(
                    appointment = appointment,
                    patientName = patientMap[appointment.patientId]?.name ?: "Paciente",
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = topOffset)
                        .height(blockHeight)
                        .padding(start = 4.dp, end = 8.dp, bottom = 2.dp),
                    onEdit = { onEdit(appointment.id) },
                    onDelete = { onDelete(appointment) },
                    onPatientClick = { patientMap[appointment.patientId]?.let { onPatientClick(it.id) } },
                    onStatusChange = { onStatusChange(appointment, it) }
                )
            }
        }
    }
}

@Composable
fun AppointmentTimelineItem(
    appointment: Appointment,
    patientName: String,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPatientClick: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val color = statusColor(appointment.status)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir consulta") },
            text = { Text("Deseja excluir esta consulta? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Box(modifier) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { showMenu = true }
        ) {
            // Colored left border
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            // Content
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "${appointment.dateTime.toFormattedTime()} · $patientName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    appointment.procedureType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(patientName) },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                onClick = { showMenu = false; onPatientClick() }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Editar") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { showMenu = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { showMenu = false; showDeleteDialog = true }
            )
            HorizontalDivider()
            AppointmentStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = { onStatusChange(status); showMenu = false }
                )
            }
        }
    }
}

// ─── Week Strip ────────────────────────────────────────────────────────────────

@Composable
fun WeekStrip(
    selectedDate: Long,
    appointmentsByDay: Map<String, Int>,
    onDaySelected: (Long) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayNameFmt = remember { SimpleDateFormat("EEE", Locale("pt", "BR")) }
    val dayNumFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val monthYearFmt = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }

    val weekStart = remember(selectedDate) { weekStart(selectedDate) }
    val days = remember(weekStart) {
        (0..6).map { i ->
            Calendar.getInstance().apply {
                timeInMillis = weekStart; add(Calendar.DAY_OF_MONTH, i)
            }.timeInMillis
        }
    }

    val selectedKey = dayKeyFmt.format(Date(selectedDate))
    val todayKey = dayKeyFmt.format(Date())

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPrevWeek) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Semana anterior")
            }
            Text(monthYearFmt.format(Date(days[3])).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Próxima semana")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { dayMillis ->
                val key = dayKeyFmt.format(Date(dayMillis))
                val isSelected = key == selectedKey
                val isToday = key == todayKey
                val count = appointmentsByDay[key] ?: 0

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onDaySelected(dayMillis) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(dayNameFmt.format(Date(dayMillis)).uppercase().take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dayNumFmt.format(Date(dayMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        })
                    Box(
                        Modifier.size(6.dp).clip(CircleShape).background(
                            when {
                                count == 0 -> Color.Transparent
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

private fun weekStart(dateMillis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
    cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ─── List Card (Week View) ─────────────────────────────────────────────────────

@Composable
fun AppointmentCard(
    appointment: Appointment,
    patientName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPatientClick: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir consulta") },
            text = { Text("Deseja excluir esta consulta? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

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
                    Surface(shape = RoundedCornerShape(50), color = statusColor(appointment.status),
                        modifier = Modifier.clickable { showStatusMenu = true }) {
                        Text(appointment.status.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface)
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        AppointmentStatus.entries.forEach { status ->
                            DropdownMenuItem(text = { Text(status.label) },
                                onClick = { onStatusChange(status); showStatusMenu = false })
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(appointment.dateTime.toFormattedTime(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("${appointment.durationMinutes} min", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, "Editar", Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, "Excluir", Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            if (appointment.notes.isNotBlank()) {
                Text(appointment.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }

            if (appointment.calendarEventId > 0L) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, null, Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Google Calendar", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
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
