package com.meuconsultorio.ui.home

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.Compromisso
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.entity.Turno
import com.meuconsultorio.data.entity.TurnoStatus
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.ui.util.isTablet
import androidx.compose.ui.graphics.Color
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.AuthViewModel
import com.meuconsultorio.viewmodel.CompromissoViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel
import com.meuconsultorio.viewmodel.TurnoViewModel
import java.text.SimpleDateFormat
import java.util.*

private sealed class TodayEvent(val time: Long) {
    class Appt(val appointment: Appointment, val patientName: String?) : TodayEvent(appointment.dateTime)
    class Treat(val treatment: Treatment, val patientName: String?) : TodayEvent(treatment.date)
    class Comp(val compromisso: Compromisso) : TodayEvent(compromisso.date)
    class Turn(val turno: Turno) : TodayEvent(turno.date)
}

private fun todayRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
    return Pair(start, cal.timeInMillis)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPatients: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToAppointmentForm: () -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    compromissoViewModel: CompromissoViewModel = hiltViewModel(),
    turnoViewModel: TurnoViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val totalPatients by patientViewModel.totalPatients.collectAsState()
    val patients by patientViewModel.patients.collectAsState()
    val patientMap = patients.associateBy { it.id }
    val todayAppointments by appointmentViewModel.todayAppointments.collectAsState()
    val allTreatments by treatmentViewModel.allTreatments.collectAsState()
    val allCompromissos by compromissoViewModel.allCompromissos.collectAsState()
    val allTurnos by turnoViewModel.allTurnos.collectAsState()
    val tablet = isTablet()
    var showMenu by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val todayEvents = remember(todayAppointments, allTreatments, allCompromissos, allTurnos, patientMap) {
        val (start, end) = todayRange()
        val events = mutableListOf<TodayEvent>()
        todayAppointments.forEach { events += TodayEvent.Appt(it, patientMap[it.patientId]?.name) }
        allTreatments.filter { it.date in start..end }.forEach { events += TodayEvent.Treat(it, patientMap[it.patientId]?.name) }
        allCompromissos.filter { it.date in start..end }.forEach { events += TodayEvent.Comp(it) }
        allTurnos.filter { it.date in start..end }.forEach { events += TodayEvent.Turn(it) }
        events.sortedBy { it.time }
    }

    val today = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("pt", "BR"))
        .format(Date())
        .replaceFirstChar { it.uppercase() }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sair") },
            text = { Text("Deseja sair da sua conta?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    authViewModel.signOut()
                }) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Meu Consultório", style = MaterialTheme.typography.titleLarge)
                        Text(today, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAppointmentForm) {
                        Icon(Icons.Filled.Add, contentDescription = "Nova consulta")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sair") },
                                leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showSignOutDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (tablet) {
            // Tablet: two-column layout — stats on left, appointments on right
            Row(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.width(340.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            Modifier.weight(1f).semantics { contentDescription = "card_stat_pacientes" }, Icons.Filled.People, "Pacientes",
                            totalPatients.toString(), MaterialTheme.colorScheme.primary, onNavigateToPatients
                        )
                        StatCard(
                            Modifier.weight(1f).semantics { contentDescription = "card_stat_consultas_hoje" }, Icons.Filled.CalendarMonth, "Hoje",
                            todayEvents.size.toString(), MaterialTheme.colorScheme.secondary, onNavigateToAppointments
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToAppointmentForm,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nova consulta")
                    }
                }

                // Right pane: today's events list
                Column(Modifier.weight(1f)) {
                    Text("Agenda de hoje", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    TodayEventsPanel(todayEvents)
                }
            }
        } else {
            // Phone: single column
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).semantics { contentDescription = "home_screen" },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f).semantics { contentDescription = "card_stat_pacientes" }, Icons.Filled.People, "Pacientes",
                            totalPatients.toString(), MaterialTheme.colorScheme.primary, onNavigateToPatients)
                        StatCard(Modifier.weight(1f).semantics { contentDescription = "card_stat_consultas_hoje" }, Icons.Filled.CalendarMonth, "Hoje",
                            todayEvents.size.toString(), MaterialTheme.colorScheme.secondary, onNavigateToAppointments)
                    }
                }

                item {
                    Text("Agenda de hoje", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }

                if (todayEvents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum evento para hoje",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(todayEvents) { event ->
                        TodayEventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayEventsPanel(events: List<TodayEvent>) {
    if (events.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Nenhum evento para hoje",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(events) { TodayEventCard(it) }
        }
    }
}

@Composable
fun TodayEventCard(event: TodayEvent) {
    when (event) {
        is TodayEvent.Appt -> TodayAppointmentCard(event.appointment, event.patientName)
        is TodayEvent.Treat -> TodayTreatmentCard(event.treatment, event.patientName)
        is TodayEvent.Comp -> TodayCompromissoCard(event.compromisso)
        is TodayEvent.Turn -> TodayTurnoCard(event.turno)
    }
}

@Composable
fun TodayTreatmentCard(treatment: Treatment, patientName: String?) {
    val color = Color(0xFFFF9800)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(treatment.date.toFormattedTime(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (!patientName.isNullOrBlank()) {
                    Text(patientName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text(treatment.procedure, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${treatment.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Healing, null, Modifier.size(12.dp), tint = color)
                    Spacer(Modifier.width(4.dp))
                    Text("Tratamento", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}

@Composable
fun TodayCompromissoCard(compromisso: Compromisso) {
    val color = Color(0xFF9C27B0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(compromisso.date.toFormattedTime(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(compromisso.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                if (compromisso.description.isNotBlank()) {
                    Text(compromisso.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (compromisso.endDate != null) {
                    Text("Até ${compromisso.endDate.toFormattedTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Event, null, Modifier.size(12.dp), tint = color)
                    Spacer(Modifier.width(4.dp))
                    Text("Compromisso", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}

@Composable
fun TodayTurnoCard(turno: Turno) {
    val isPendente = turno.status == TurnoStatus.PENDENTE
    val color = if (isPendente) Color(0xFF00BCD4) else Color(0xFF00897B)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(turno.date.toFormattedTime(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(turno.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                if (turno.description.isNotBlank()) {
                    Text(turno.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (turno.valor > 0) {
                    Text("R$ ${"%.2f".format(turno.valor)} · ${turno.status.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = color)
                }
            }
            Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
                Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventNote, null, Modifier.size(12.dp), tint = color)
                    Spacer(Modifier.width(4.dp))
                    Text("Turno", style = MaterialTheme.typography.labelSmall, color = color)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TodayAppointmentCard(appointment: Appointment, patientName: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "card_consulta_hoje" },
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(appointment.dateTime.toFormattedTime(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (!patientName.isNullOrBlank()) {
                    Text(patientName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
                Text(appointment.procedureType, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${appointment.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (appointment.notes.isNotBlank()) {
                    Text(appointment.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AppointmentStatusChip(appointment.status.label)
        }
    }
}

@Composable
fun AppointmentStatusChip(label: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
