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
import com.meuconsultorio.data.entity.Payment
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.PaymentViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onPatientClick: (Long) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel()
) {
    val payments by viewModel.allPayments.collectAsState()

    val patients by patientViewModel.patients.collectAsState()
    val allTreatments by treatmentViewModel.allTreatments.collectAsState()
    val totalTreatmentCost by treatmentViewModel.totalTreatmentCost.collectAsState()
    val totalTreatmentPrice by treatmentViewModel.totalTreatmentPrice.collectAsState()
    val totalAppointmentPrice by appointmentViewModel.totalAppointmentPrice.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var filterPaid by remember { mutableStateOf<Boolean?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Payment?>(null) }

    showDeleteDialog?.let { payment ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Excluir pagamento") },
            text = { Text("Deseja excluir este registro de pagamento?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePayment(payment); showDeleteDialog = null }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") } }
        )
    }

    val patientMap = patients.associateBy { it.id }

    val displayedPayments = when (filterPaid) {
        true -> payments.filter { it.isPaid }
        false -> payments.filter { !it.isPaid }
        null -> payments
    }

    Scaffold(
        topBar = { AppTopBar(title = "Financeiro") },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onAddPayment) {
                    Icon(Icons.Filled.Add, contentDescription = "Novo pagamento")
                }
            }
        }
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
                        icon = Icons.Filled.EventAvailable,
                        label = "Total consultas",
                        value = totalAppointmentPrice.toCurrency(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.MedicalServices,
                        label = "Valor tratamentos",
                        value = totalTreatmentPrice.toCurrency(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.MoneyOff,
                        label = "Custo materiais",
                        value = totalTreatmentCost.toCurrency(),
                        color = MaterialTheme.colorScheme.error
                    )
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.TrendingUp,
                        label = "Lucro tratamentos",
                        value = (totalTreatmentPrice - totalTreatmentCost).toCurrency(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Pagamentos") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Tratamentos") })
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(selected = filterPaid == null, onClick = { filterPaid = null },
                                label = { Text("Todos") })
                            FilterChip(selected = filterPaid == true,
                                onClick = { filterPaid = if (filterPaid == true) null else true },
                                label = { Text("Pagos") },
                                leadingIcon = if (filterPaid == true) ({
                                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                                }) else null)
                            FilterChip(selected = filterPaid == false,
                                onClick = { filterPaid = if (filterPaid == false) null else false },
                                label = { Text("Pendentes") })
                        }
                    }
                    if (displayedPayments.isEmpty()) {
                        item { EmptyState("Nenhum pagamento registrado", Modifier.height(200.dp)) }
                    } else {
                        items(displayedPayments, key = { it.id }) { payment ->
                            FullPaymentCard(
                                payment = payment,
                                patientName = patientMap[payment.patientId]?.name ?: "Paciente",
                                onEdit = { onEditPayment(payment.id) },
                                onPatientClick = { onPatientClick(payment.patientId) },
                                onDelete = { showDeleteDialog = payment }
                            )
                        }
                    }
                }
                1 -> {
                    if (allTreatments.isEmpty()) {
                        item { EmptyState("Nenhum tratamento registrado", Modifier.height(200.dp)) }
                    } else {
                        items(allTreatments, key = { it.id }) { treatment ->
                            TreatmentFinancialCard(
                                treatment = treatment,
                                patientName = patientMap[treatment.patientId]?.name ?: "Paciente",
                                onPatientClick = { onPatientClick(treatment.patientId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FullPaymentCard(
    payment: Payment,
    patientName: String,
    onEdit: () -> Unit,
    onPatientClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(patientName, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable(onClick = onPatientClick))
                    Text(payment.description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        payment.amount.toCurrency(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (payment.isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        if (payment.isPaid) "Pago" else "Pendente",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (payment.isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CreditCard, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(payment.method.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(payment.date.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            if (payment.notes.isNotBlank()) {
                Text(payment.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun TreatmentFinancialCard(
    treatment: Treatment,
    patientName: String,
    onPatientClick: () -> Unit
) {
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
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(treatment.status.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}
