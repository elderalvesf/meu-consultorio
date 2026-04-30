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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.ui.util.isTablet
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.AuthViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.PaymentViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPatients: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToAppointmentForm: () -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val totalPatients by patientViewModel.totalPatients.collectAsState()
    val todayAppointments by appointmentViewModel.todayAppointments.collectAsState()
    val totalReceived by paymentViewModel.totalReceived.collectAsState()
    val monthReceived by paymentViewModel.monthReceived.collectAsState()
    val tablet = isTablet()
    var showMenu by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

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
                            Modifier.weight(1f), Icons.Filled.People, "Pacientes",
                            totalPatients.toString(), MaterialTheme.colorScheme.primary, onNavigateToPatients
                        )
                        StatCard(
                            Modifier.weight(1f), Icons.Filled.CalendarMonth, "Hoje",
                            todayAppointments.size.toString(), MaterialTheme.colorScheme.secondary, onNavigateToAppointments
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            Modifier.weight(1f), Icons.Filled.AttachMoney, "Mês atual",
                            monthReceived.toCurrency(), MaterialTheme.colorScheme.tertiary, {}
                        )
                        StatCard(
                            Modifier.weight(1f), Icons.Filled.Savings, "Total recebido",
                            totalReceived.toCurrency(), Color(0xFF7B1FA2), {}
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

                // Right pane: today's appointments list
                Column(Modifier.weight(1f)) {
                    Text("Consultas de hoje", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    TodayAppointmentsPanel(todayAppointments)
                }
            }
        } else {
            // Phone: single column
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), Icons.Filled.People, "Pacientes",
                            totalPatients.toString(), MaterialTheme.colorScheme.primary, onNavigateToPatients)
                        StatCard(Modifier.weight(1f), Icons.Filled.CalendarMonth, "Hoje",
                            todayAppointments.size.toString(), MaterialTheme.colorScheme.secondary, onNavigateToAppointments)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), Icons.Filled.AttachMoney, "Mês atual",
                            monthReceived.toCurrency(), MaterialTheme.colorScheme.tertiary, {})
                        StatCard(Modifier.weight(1f), Icons.Filled.Savings, "Total recebido",
                            totalReceived.toCurrency(), Color(0xFF7B1FA2), {})
                    }
                }

                item {
                    Text("Consultas de hoje", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }

                if (todayAppointments.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma consulta para hoje",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(todayAppointments) { appointment ->
                        TodayAppointmentCard(appointment = appointment)
                    }
                }
            }
        }
    }
}

@Composable
fun TodayAppointmentsPanel(appointments: List<Appointment>) {
    if (appointments.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Nenhuma consulta para hoje",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(appointments) { TodayAppointmentCard(it) }
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
fun TodayAppointmentCard(appointment: Appointment) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
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
                Text(appointment.procedureType, style = MaterialTheme.typography.titleMedium)
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
