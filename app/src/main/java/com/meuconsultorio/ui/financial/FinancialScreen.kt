package com.meuconsultorio.ui.financial

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.AppointmentStatus
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.data.entity.TreatmentStatus
import com.meuconsultorio.data.entity.Turno
import com.meuconsultorio.data.entity.TurnoStatus
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel
import com.meuconsultorio.viewmodel.TurnoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    onPatientClick: (Long) -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel(),
    turnoViewModel: TurnoViewModel = hiltViewModel()
) {
    val patients by patientViewModel.patients.collectAsState()
    val allTreatments by treatmentViewModel.allTreatments.collectAsState()
    val allAppointments by appointmentViewModel.allAppointments.collectAsState()
    val allTurnos by turnoViewModel.allTurnos.collectAsState()
    val totalTreatmentCost by treatmentViewModel.totalTreatmentCost.collectAsState()
    val totalTreatmentPrice by treatmentViewModel.totalTreatmentPrice.collectAsState()
    val totalAppointmentPrice by appointmentViewModel.totalAppointmentPrice.collectAsState()
    val totalTurnoConfirmado by turnoViewModel.totalTurnoConfirmado.collectAsState()

    val patientMap = patients.associateBy { it.id }
    val totalRevenue = totalTreatmentPrice + totalAppointmentPrice + totalTurnoConfirmado

    var showRevenueDetail by remember { mutableStateOf(false) }

    if (showRevenueDetail) {
        val concludedTreatments = allTreatments.filter { it.status == TreatmentStatus.CONCLUIDO && it.price > 0 }
        val paidAppointments = allAppointments.filter { it.isPaid && it.price > 0 }
        val confirmedTurnos = allTurnos.filter { it.status == TurnoStatus.CONFIRMADO && it.valor > 0 }

        ModalBottomSheet(onDismissRequest = { showRevenueDetail = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Origem do valor", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Total: ${totalRevenue.toCurrency()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(16.dp))

                if (concludedTreatments.isNotEmpty()) {
                    Text("Tratamentos concluídos",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    concludedTreatments.forEach { treatment ->
                        RevenueDetailRow(
                            title = treatment.procedure,
                            subtitle = patientMap[treatment.patientId]?.name ?: "Paciente",
                            date = treatment.date.toFormattedDate(),
                            amount = treatment.price.toCurrency()
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal tratamentos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(totalTreatmentPrice.toCurrency(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (paidAppointments.isNotEmpty()) {
                    Text("Consultas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    paidAppointments.forEach { appointment ->
                        RevenueDetailRow(
                            title = appointment.procedureType,
                            subtitle = patientMap[appointment.patientId]?.name ?: "Paciente",
                            date = appointment.dateTime.toFormattedDate(),
                            amount = appointment.price.toCurrency()
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal consultas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(totalAppointmentPrice.toCurrency(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }

                if (confirmedTurnos.isNotEmpty()) {
                    Text("Turnos confirmados",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                    confirmedTurnos.forEach { turno ->
                        RevenueDetailRow(
                            title = turno.name,
                            subtitle = if (turno.description.isNotBlank()) turno.description else "Turno",
                            date = turno.date.toFormattedDate(),
                            amount = turno.valor.toCurrency()
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal turnos",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(totalTurnoConfirmado.toCurrency(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (concludedTreatments.isEmpty() && paidAppointments.isEmpty() && confirmedTurnos.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Nenhum valor registrado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "Financeiro") }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Resumo financeiro", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.AttachMoney,
                        label = "Receita total",
                        value = totalRevenue.toCurrency(),
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { showRevenueDetail = true }
                    )
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.MoneyOff,
                        label = "Custo materiais",
                        value = totalTreatmentCost.toCurrency(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
                FinancialCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.TrendingUp,
                    label = "Lucro",
                    value = (totalRevenue - totalTreatmentCost).toCurrency(),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(8.dp))
                Text("Tratamentos", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (allTreatments.isEmpty()) {
                item { EmptyState("Nenhum tratamento registrado", Modifier.height(80.dp)) }
            } else {
                items(allTreatments, key = { "t_${it.id}" }) { treatment ->
                    TreatmentFinancialCard(
                        treatment = treatment,
                        patientName = patientMap[treatment.patientId]?.name ?: "Paciente",
                        onPatientClick = { onPatientClick(treatment.patientId) },
                        onStatusChange = { newStatus ->
                            treatmentViewModel.saveTreatment(treatment.copy(status = newStatus))
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Consultas", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (allAppointments.isEmpty()) {
                item { EmptyState("Nenhuma consulta registrada", Modifier.height(80.dp)) }
            } else {
                items(allAppointments, key = { "a_${it.id}" }) { appointment ->
                    AppointmentFinancialCard(
                        appointment = appointment,
                        patientName = patientMap[appointment.patientId]?.name ?: "Paciente",
                        onPatientClick = { onPatientClick(appointment.patientId) },
                        onTogglePaid = {
                            appointmentViewModel.saveAppointment(appointment.copy(isPaid = !appointment.isPaid))
                        },
                        onStatusChange = { newStatus ->
                            appointmentViewModel.updateStatus(appointment, newStatus)
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text("Turnos", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (allTurnos.isEmpty()) {
                item { EmptyState("Nenhum turno registrado", Modifier.height(80.dp)) }
            } else {
                items(allTurnos, key = { "tn_${it.id}" }) { turno ->
                    TurnoFinancialCard(
                        turno = turno,
                        onStatusChange = { newStatus ->
                            turnoViewModel.saveTurno(turno.copy(status = newStatus))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueDetailRow(title: String, subtitle: String, date: String, amount: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(date, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun FinancialCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                if (onClick != null) {
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Ver detalhes",
                        tint = color.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TreatmentFinancialCard(
    treatment: Treatment,
    patientName: String,
    onPatientClick: () -> Unit,
    onStatusChange: (TreatmentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var pendingStatus by remember { mutableStateOf<TreatmentStatus?>(null) }

    pendingStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { pendingStatus = null },
            title = { Text("Alterar status") },
            text = { Text("Alterar status de \"${treatment.status.label}\" para \"${status.label}\"?") },
            confirmButton = {
                TextButton(onClick = { onStatusChange(status); pendingStatus = null }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatus = null }) { Text("Cancelar") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(patientName, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onPatientClick))
                Text(treatment.procedure, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (treatment.tooth.isNotBlank()) {
                    Text("Dente: ${treatment.tooth}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(treatment.date.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (treatment.price > 0)
                    Text(treatment.price.toCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                if (treatment.cost > 0)
                    Text("Custo: ${treatment.cost.toCurrency()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (treatment.price > 0 || treatment.cost > 0)
                    Text("Lucro: ${(treatment.price - treatment.cost).toCurrency()}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.height(4.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.clickable { showStatusMenu = true }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(treatment.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        TreatmentStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.label) },
                                onClick = { pendingStatus = status; showStatusMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentFinancialCard(
    appointment: Appointment,
    patientName: String,
    onPatientClick: () -> Unit,
    onTogglePaid: () -> Unit,
    onStatusChange: (AppointmentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var pendingStatus by remember { mutableStateOf<AppointmentStatus?>(null) }

    pendingStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { pendingStatus = null },
            title = { Text("Alterar status") },
            text = { Text("Alterar status de \"${appointment.status.label}\" para \"${status.label}\"?") },
            confirmButton = {
                TextButton(onClick = { onStatusChange(status); pendingStatus = null }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatus = null }) { Text("Cancelar") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(patientName, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable(onClick = onPatientClick))
                Text(appointment.procedureType, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(appointment.dateTime.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (appointment.price > 0)
                    Text(appointment.price.toCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // isPaid toggle
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (appointment.isPaid) androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(onClick = onTogglePaid)
                    ) {
                        Text(
                            if (appointment.isPaid) "Pago" else "Não pago",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (appointment.isPaid) androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Status dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { showStatusMenu = true }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appointment.status.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Filled.ArrowDropDown, null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                            AppointmentStatus.entries.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.label) },
                                    onClick = { pendingStatus = status; showStatusMenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TurnoFinancialCard(
    turno: Turno,
    onStatusChange: (TurnoStatus) -> Unit
) {
    val isConfirmado = turno.status == TurnoStatus.CONFIRMADO
    val statusColor = if (isConfirmado) androidx.compose.ui.graphics.Color(0xFF00897B)
                      else androidx.compose.ui.graphics.Color(0xFF00BCD4)
    var pendingStatus by remember { mutableStateOf<TurnoStatus?>(null) }

    pendingStatus?.let { status ->
        AlertDialog(
            onDismissRequest = { pendingStatus = null },
            title = { Text("Alterar status") },
            text = { Text("Alterar de \"${turno.status.label}\" para \"${status.label}\"?\n\n" +
                if (status == TurnoStatus.CONFIRMADO) "O valor será contabilizado no financeiro."
                else "O valor será removido do financeiro.") },
            confirmButton = {
                TextButton(onClick = { onStatusChange(status); pendingStatus = null }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStatus = null }) { Text("Cancelar") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(turno.name, style = MaterialTheme.typography.titleMedium)
                if (turno.description.isNotBlank()) {
                    Text(turno.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(turno.date.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (turno.valor > 0)
                    Text(turno.valor.toCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isConfirmado) statusColor else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Box {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { pendingStatus = if (isConfirmado) TurnoStatus.PENDENTE else TurnoStatus.CONFIRMADO }
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(turno.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Filled.ArrowDropDown, null,
                                Modifier.size(14.dp), tint = statusColor)
                        }
                    }
                }
            }
        }
    }
}
