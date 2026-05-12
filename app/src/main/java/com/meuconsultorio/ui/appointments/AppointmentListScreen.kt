package com.meuconsultorio.ui.appointments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.entity.Compromisso
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.entity.TreatmentStatus
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.CompromissoViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    onAddAppointment: () -> Unit,
    onAddTreatment: () -> Unit,
    onAddCompromisso: () -> Unit,
    onEditAppointment: (Long) -> Unit,
    onEditTreatment: (Long) -> Unit,
    onEditCompromisso: (Long) -> Unit,
    onPatientClick: (Long) -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    compromissoViewModel: CompromissoViewModel = hiltViewModel()
) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val allTreatments by treatmentViewModel.allTreatments.collectAsState()
    val allCompromissos by compromissoViewModel.allCompromissos.collectAsState()
    val patients by patientViewModel.patients.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayAppointments by viewModel.todayAppointments.collectAsState()
    val treatmentsForDay by viewModel.treatmentsForSelectedDay.collectAsState()

    LaunchedEffect(Unit) { viewModel.pullFromCalendar() }

    var weeklyView by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf<AppointmentStatus?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    val dayKeyFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Combined events-per-day for dot indicators (appointments + treatments + compromissos)
    val eventsByDay = remember(allAppointments, allTreatments, allCompromissos) {
        val map = mutableMapOf<String, Int>()
        allAppointments.forEach { map[dayKeyFmt.format(Date(it.dateTime))] = (map[dayKeyFmt.format(Date(it.dateTime))] ?: 0) + 1 }
        allTreatments.forEach { map[dayKeyFmt.format(Date(it.date))] = (map[dayKeyFmt.format(Date(it.date))] ?: 0) + 1 }
        allCompromissos.forEach { map[dayKeyFmt.format(Date(it.date))] = (map[dayKeyFmt.format(Date(it.date))] ?: 0) + 1 }
        map as Map<String, Int>
    }

    // Filter compromissos for selected day
    val compromissosForDay = remember(allCompromissos, selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        allCompromissos.filter { it.date in start..end }
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
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    "Compromisso",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; onAddCompromisso() },
                                containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                                modifier = Modifier.semantics { contentDescription = "fab_novo_compromisso" }
                            ) {
                                Icon(Icons.Filled.Event, contentDescription = null, tint = Color(0xFF9C27B0))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    "Tratamento",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; onAddTreatment() },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.semantics { contentDescription = "fab_novo_tratamento" }
                            ) {
                                Icon(Icons.Filled.Healing, contentDescription = null)
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    "Consulta",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            SmallFloatingActionButton(
                                onClick = { fabExpanded = false; onAddAppointment() },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.semantics { contentDescription = "fab_novo_agendamento" }
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    modifier = Modifier.semantics { contentDescription = "fab_agenda" }
                ) {
                    Icon(
                        if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                        contentDescription = if (fabExpanded) "Fechar" else "Adicionar"
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "agenda_screen" }) {

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
                    eventsByDay = eventsByDay,
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
                            val treatLabel = if (treatmentsForDay.isNotEmpty()) " · ${treatmentsForDay.size} tratamento(s)" else ""
                            val compLabel = if (compromissosForDay.isNotEmpty()) " · ${compromissosForDay.size} compromisso(s)" else ""
                            Text("${displayedAppointments.size} consulta(s)$treatLabel$compLabel",
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

            Box(Modifier.weight(1f)) {
                DayTimelineView(
                    appointments = displayedAppointments,
                    treatments = treatmentsForDay,
                    compromissos = compromissosForDay,
                    patientMap = patientMap,
                    selectedDate = selectedDate,
                    onEdit = onEditAppointment,
                    onDelete = { viewModel.deleteAppointment(it) },
                    onEditTreatment = onEditTreatment,
                    onDeleteTreatment = { treatmentViewModel.deleteTreatment(it) },
                    onEditCompromisso = onEditCompromisso,
                    onDeleteCompromisso = { compromissoViewModel.deleteCompromisso(it) },
                    onPatientClick = onPatientClick,
                    onStatusChange = { appt, status -> viewModel.updateStatus(appt, status) }
                )
            }
        }
    }
}

// ─── Timeline (Day View) ───────────────────────────────────────────────────────

private const val START_HOUR = 6
private const val END_HOUR = 22
private const val TREAT_DURATION_MS = 60 * 60_000L

private fun truncateToMin(ms: Long) = (ms / 60_000L) * 60_000L

private data class ApptBlock(
    val appointment: Appointment,
    val mergedTreatments: List<Treatment>,
    val column: Int,
    val totalColumns: Int
)

private data class TreatBlock(
    val treatment: Treatment,
    val column: Int,
    val totalColumns: Int
)

private data class CompromissoBlock(
    val compromisso: Compromisso,
    val column: Int = 0,
    val totalColumns: Int = 1
)

private fun computeTimelineLayout(
    appointments: List<Appointment>,
    treatments: List<Treatment>,
    compromissos: List<Compromisso>
): Triple<List<ApptBlock>, List<TreatBlock>, List<CompromissoBlock>> {
    val mergedIds = mutableSetOf<Long>()

    val apptBlocks = appointments.map { appt ->
        val apptEndMs = appt.dateTime + appt.durationMinutes * 60_000L
        val merged = treatments.filter { t ->
            t.patientId == appt.patientId &&
            appt.dateTime < t.date + TREAT_DURATION_MS &&
            t.date < apptEndMs
        }
        mergedIds += merged.map { it.id }
        ApptBlock(appt, merged, 0, 1)
    }

    val treatBlocks = treatments
        .filter { it.id !in mergedIds }
        .map { TreatBlock(it, 0, 1) }

    val compromissoBlocks = compromissos.map { CompromissoBlock(it) }

    data class Interval(val start: Long, val end: Long, val apptIdx: Int?, val treatIdx: Int?, val compIdx: Int?)

    val intervals = mutableListOf<Interval>()
    apptBlocks.forEachIndexed { i, b ->
        val start = truncateToMin(b.appointment.dateTime)
        intervals += Interval(start, start + b.appointment.durationMinutes * 60_000L, i, null, null)
    }
    treatBlocks.forEachIndexed { i, b ->
        val start = truncateToMin(b.treatment.date)
        intervals += Interval(start, start + TREAT_DURATION_MS, null, i, null)
    }
    compromissoBlocks.forEachIndexed { i, b ->
        val start = truncateToMin(b.compromisso.date)
        intervals += Interval(start, start + TREAT_DURATION_MS, null, null, i)
    }
    intervals.sortBy { it.start }

    val colEnds = mutableListOf<Long>()
    val cols = IntArray(intervals.size)

    intervals.forEachIndexed { i, iv ->
        val col = colEnds.indexOfFirst { it <= iv.start }.takeIf { it >= 0 }
            ?: colEnds.size.also { colEnds.add(0L) }
        if (col < colEnds.size) colEnds[col] = maxOf(colEnds[col], iv.end)
        else colEnds.add(iv.end)
        cols[i] = col
    }

    val totalCols = IntArray(intervals.size) { 1 }
    for (i in intervals.indices) {
        val maxOverlapCol = intervals.indices
            .filter { j -> j != i && intervals[j].start < intervals[i].end && intervals[i].start < intervals[j].end }
            .maxOfOrNull { cols[it] } ?: -1
        totalCols[i] = maxOf(cols[i], maxOverlapCol) + 1
    }

    val finalAppts = apptBlocks.toMutableList()
    val finalTreats = treatBlocks.toMutableList()
    val finalComps = compromissoBlocks.toMutableList()
    intervals.forEachIndexed { i, iv ->
        when {
            iv.apptIdx != null -> finalAppts[iv.apptIdx] = finalAppts[iv.apptIdx].copy(column = cols[i], totalColumns = totalCols[i])
            iv.treatIdx != null -> finalTreats[iv.treatIdx] = finalTreats[iv.treatIdx].copy(column = cols[i], totalColumns = totalCols[i])
            iv.compIdx != null -> finalComps[iv.compIdx] = finalComps[iv.compIdx].copy(column = cols[i], totalColumns = totalCols[i])
        }
    }

    return Triple(finalAppts, finalTreats, finalComps)
}

@Composable
fun DayTimelineView(
    appointments: List<Appointment>,
    treatments: List<Treatment>,
    compromissos: List<Compromisso>,
    patientMap: Map<Long, Patient>,
    selectedDate: Long,
    onEdit: (Long) -> Unit,
    onDelete: (Appointment) -> Unit,
    onEditTreatment: (Long) -> Unit,
    onDeleteTreatment: (Treatment) -> Unit,
    onEditCompromisso: (Long) -> Unit,
    onDeleteCompromisso: (Compromisso) -> Unit,
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

    LaunchedEffect(appointments, treatments, compromissos, selectedDate) {
        val allTimes = appointments.map { it.dateTime } + treatments.map { it.date } + compromissos.map { it.date }
        val targetHour = if (allTimes.isNotEmpty()) {
            Calendar.getInstance().apply { timeInMillis = allTimes.min() }
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

    val (apptBlocks, treatBlocks, compBlocks) = remember(appointments, treatments, compromissos) {
        computeTimelineLayout(appointments, treatments, compromissos)
    }

    Row(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .semantics { contentDescription = "timeline_agenda" }
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
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(hourHeight * totalHours)
        ) {
            val availableWidth = maxWidth

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
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                    HorizontalDivider(color = Color.Red, thickness = 1.5.dp)
                }
            }

            // Appointment blocks (may include merged treatments)
            apptBlocks.forEach { block ->
                val appt = block.appointment
                val cal = Calendar.getInstance().apply { timeInMillis = appt.dateTime }
                val minutesFromStart = (cal.get(Calendar.HOUR_OF_DAY) - START_HOUR) * 60 + cal.get(Calendar.MINUTE)
                val topOffset = hourHeight * (minutesFromStart.toFloat() / 60f)
                val blockHeight = (hourHeight * (appt.durationMinutes.toFloat() / 60f)).coerceAtLeast(40.dp)
                val colWidth = availableWidth / block.totalColumns
                val xOffset = colWidth * block.column

                AppointmentTimelineItem(
                    appointment = appt,
                    mergedTreatments = block.mergedTreatments,
                    patientName = patientMap[appt.patientId]?.name ?: "Paciente",
                    modifier = Modifier
                        .width(colWidth)
                        .offset(x = xOffset, y = topOffset)
                        .height(blockHeight)
                        .padding(start = 4.dp, end = 8.dp, bottom = 2.dp),
                    onEdit = { onEdit(appt.id) },
                    onDelete = { onDelete(appt) },
                    onPatientClick = { patientMap[appt.patientId]?.let { onPatientClick(it.id) } },
                    onTreatmentClick = onEditTreatment,
                    onStatusChange = { onStatusChange(appt, it) }
                )
            }

            // Standalone treatment blocks
            treatBlocks.forEach { block ->
                val t = block.treatment
                val cal = Calendar.getInstance().apply { timeInMillis = t.date }
                val minutesFromStart = (cal.get(Calendar.HOUR_OF_DAY) - START_HOUR) * 60 + cal.get(Calendar.MINUTE)
                val topOffset = (hourHeight * (minutesFromStart.toFloat() / 60f)).coerceAtLeast(0.dp)
                val colWidth = availableWidth / block.totalColumns
                val xOffset = colWidth * block.column

                TreatmentTimelineItem(
                    treatment = t,
                    patientName = patientMap[t.patientId]?.name ?: "Paciente",
                    modifier = Modifier
                        .width(colWidth)
                        .offset(x = xOffset, y = topOffset)
                        .height(48.dp)
                        .padding(start = 4.dp, end = 8.dp, bottom = 2.dp),
                    onEdit = { onEditTreatment(t.id) },
                    onDelete = { onDeleteTreatment(t) },
                    onPatientClick = { patientMap[t.patientId]?.let { onPatientClick(it.id) } }
                )
            }

            // Compromisso blocks
            compBlocks.forEach { block ->
                val c = block.compromisso
                val cal = Calendar.getInstance().apply { timeInMillis = c.date }
                val minutesFromStart = (cal.get(Calendar.HOUR_OF_DAY) - START_HOUR) * 60 + cal.get(Calendar.MINUTE)
                val topOffset = (hourHeight * (minutesFromStart.toFloat() / 60f)).coerceAtLeast(0.dp)
                val colWidth = availableWidth / block.totalColumns
                val xOffset = colWidth * block.column

                CompromissoTimelineItem(
                    compromisso = c,
                    modifier = Modifier
                        .width(colWidth)
                        .offset(x = xOffset, y = topOffset)
                        .height(48.dp)
                        .padding(start = 4.dp, end = 8.dp, bottom = 2.dp),
                    onEdit = { onEditCompromisso(c.id) },
                    onDelete = { onDeleteCompromisso(c) }
                )
            }
        }
    }
}

@Composable
fun AppointmentTimelineItem(
    appointment: Appointment,
    patientName: String,
    mergedTreatments: List<Treatment> = emptyList(),
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPatientClick: () -> Unit,
    onTreatmentClick: (Long) -> Unit = {},
    onStatusChange: (AppointmentStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTreatmentsDialog by remember { mutableStateOf(false) }
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

    if (showTreatmentsDialog && mergedTreatments.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showTreatmentsDialog = false },
            title = { Text("Tratamentos vinculados") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    mergedTreatments.forEachIndexed { idx, t ->
                        val tColor = treatmentStatusColor(t.status)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTreatmentsDialog = false; onTreatmentClick(t.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(tColor))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.procedure, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(t.status.label, style = MaterialTheme.typography.bodySmall, color = tColor)
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (idx < mergedTreatments.lastIndex) HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTreatmentsDialog = false }) { Text("Fechar") }
            }
        )
    }

    Box(modifier) {
        Row(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { showMenu = true }
                .semantics(mergeDescendants = true) { contentDescription = "card_agendamento" }
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(color))
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
                if (mergedTreatments.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Healing, null, Modifier.size(10.dp), tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "+${mergedTreatments.size} tratamento(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
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
            if (mergedTreatments.isNotEmpty()) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Tratamentos (${mergedTreatments.size})") },
                    leadingIcon = { Icon(Icons.Filled.Healing, null, tint = Color(0xFFFF9800)) },
                    onClick = { showMenu = false; showTreatmentsDialog = true }
                )
            }
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
    eventsByDay: Map<String, Int>,
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
                val count = eventsByDay[key] ?: 0

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
    AppointmentStatus.AGENDADA -> Color(0xFF9E9E9E)
    AppointmentStatus.CONFIRMADA -> Color(0xFF4CAF50)
    AppointmentStatus.CONCLUIDA -> MaterialTheme.colorScheme.tertiary
    AppointmentStatus.CANCELADA -> Color(0xFFF44336)
    AppointmentStatus.NAO_COMPARECEU -> MaterialTheme.colorScheme.errorContainer
}

fun treatmentStatusColor(status: TreatmentStatus) = when (status) {
    TreatmentStatus.EM_ANDAMENTO -> Color(0xFFFF9800)
    TreatmentStatus.CONCLUIDO -> Color(0xFF4CAF50)
    TreatmentStatus.CANCELADO -> Color(0xFFF44336)
}

@Composable
fun TreatmentTimelineItem(
    treatment: Treatment,
    patientName: String,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPatientClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val color = treatmentStatusColor(treatment.status)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir tratamento") },
            text = { Text("Deseja excluir este tratamento? Esta ação não pode ser desfeita.") },
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
                .semantics(mergeDescendants = true) { contentDescription = "card_tratamento" }
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "${treatment.date.toFormattedTime()} · $patientName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Tratamento · ${treatment.procedure}",
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
        }
    }
}

@Composable
fun CompromissoTimelineItem(
    compromisso: Compromisso,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val color = Color(0xFF9C27B0)
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir compromisso") },
            text = { Text("Deseja excluir este compromisso? Esta ação não pode ser desfeita.") },
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
                .semantics(mergeDescendants = true) { contentDescription = "card_compromisso" }
        ) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(color))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "${compromisso.date.toFormattedTime()} · ${compromisso.name}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (compromisso.description.isNotBlank()) {
                    Text(
                        compromisso.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
        }
    }
}
