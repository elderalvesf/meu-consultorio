package com.meuconsultorio.ui.financial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.ui.util.isTablet
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    onAddPayment: () -> Unit,
    onEditPayment: (Long) -> Unit,
    onPatientClick: (Long) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val payments by viewModel.allPayments.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()
    val totalPending by viewModel.totalPending.collectAsState()
    val monthReceived by viewModel.monthReceived.collectAsState()
    val patients by patientViewModel.patients.collectAsState()

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
    val tablet = isTablet()

    val displayedPayments = when (filterPaid) {
        true -> payments.filter { it.isPaid }
        false -> payments.filter { !it.isPaid }
        null -> payments
    }

    Scaffold(
        topBar = { AppTopBar(title = "Financeiro") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPayment) {
                Icon(Icons.Filled.Add, contentDescription = "Novo pagamento")
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
                        icon = Icons.Filled.CheckCircle,
                        label = "Total recebido",
                        value = totalReceived.toCurrency(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    FinancialCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Pending,
                        label = "Pendente",
                        value = totalPending.toCurrency(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Recebido este mês", style = MaterialTheme.typography.labelMedium)
                            Text(monthReceived.toCurrency(), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filterPaid == null, onClick = { filterPaid = null },
                        label = { Text("Todos") })
                    FilterChip(selected = filterPaid == true, onClick = { filterPaid = if (filterPaid == true) null else true },
                        label = { Text("Pagos") }, leadingIcon = if (filterPaid == true) ({
                            Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
                        }) else null)
                    FilterChip(selected = filterPaid == false, onClick = { filterPaid = if (filterPaid == false) null else false },
                        label = { Text("Pendentes") })
                }
            }

            if (displayedPayments.isEmpty()) {
                item { EmptyState("Nenhum pagamento registrado", Modifier.height(200.dp)) }
            } else if (tablet) {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(
                            (displayedPayments.size * 120 + 32).dp.coerceAtMost(1200.dp)
                        )
                    ) {
                        gridItems(displayedPayments, key = { it.id }) { payment ->
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
