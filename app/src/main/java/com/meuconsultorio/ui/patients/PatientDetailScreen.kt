package com.meuconsultorio.ui.patients

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meuconsultorio.data.entity.*
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.PaymentViewModel
import com.meuconsultorio.viewmodel.ProntuarioViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onEdit: () -> Unit,
    onAddAppointment: () -> Unit,
    onEditAppointment: (Long) -> Unit,
    onAddTreatment: () -> Unit,
    onAddPayment: () -> Unit,
    onEditTreatment: (Long) -> Unit,
    onEditPayment: (Long) -> Unit,
    onOpenProntuario: (appointmentId: Long?) -> Unit,
    onEditProntuario: (entryId: Long) -> Unit,
    onBack: () -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    paymentViewModel: PaymentViewModel = hiltViewModel(),
    prontuarioViewModel: ProntuarioViewModel = hiltViewModel()
) {
    val patient by patientViewModel.selectedPatient.collectAsState()
    val appointments by appointmentViewModel.patientAppointments.collectAsState()
    val treatments by treatmentViewModel.patientTreatments.collectAsState()
    val payments by paymentViewModel.patientPayments.collectAsState()
    val totalCost by treatmentViewModel.patientTotalCost.collectAsState()
    val prontuarioEntries by prontuarioViewModel.patientEntries.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(patientId) {
        patientViewModel.loadPatient(patientId)
        appointmentViewModel.loadPatientAppointments(patientId)
        treatmentViewModel.loadPatientTreatments(patientId)
        paymentViewModel.loadPatientPayments(patientId)
        prontuarioViewModel.loadPatientEntries(patientId)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir paciente") },
            text = { Text("Deseja excluir este paciente? Todas as consultas, tratamentos e pagamentos serão excluídos.") },
            confirmButton = {
                TextButton(onClick = {
                    patient?.let { patientViewModel.deletePatient(it); onBack() }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = patient?.name ?: "Paciente",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Filled.Delete, contentDescription = "Excluir") }
                }
            )
        }
    ) { padding ->
        if (patient == null) {
            LoadingIndicator()
            return@Scaffold
        }

        val entriesByAppointment = prontuarioEntries.groupBy { it.appointmentId }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                PatientInfoCard(patient = patient!!, totalCost = totalCost)
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Prontuário (${appointments.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Tratamentos (${treatments.size})") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        text = { Text("Pagamentos (${payments.size})") })
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                            FilledTonalButton(onClick = onAddAppointment) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Nova consulta")
                            }
                        }
                    }
                    if (appointments.isEmpty()) {
                        item { EmptyState("Nenhuma consulta registrada", Modifier.height(200.dp)) }
                    } else {
                        items(appointments, key = { it.id }) { appointment ->
                            val appointmentEntries = entriesByAppointment[appointment.id] ?: emptyList()
                            AppointmentProntuarioCard(
                                appointment = appointment,
                                prontuarioEntries = appointmentEntries,
                                onEditAppointment = { onEditAppointment(appointment.id) },
                                onOpenProntuario = { onOpenProntuario(appointment.id) },
                                onEditProntuario = onEditProntuario,
                                onDeleteEntry = { prontuarioViewModel.deleteEntry(it) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                1 -> {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                            FilledTonalButton(onClick = onAddTreatment) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Novo tratamento")
                            }
                        }
                    }
                    if (treatments.isEmpty()) {
                        item { EmptyState("Nenhum tratamento registrado", Modifier.height(200.dp)) }
                    } else {
                        items(treatments) { treatment ->
                            TreatmentItemCard(
                                treatment = treatment,
                                onEdit = { onEditTreatment(treatment.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                2 -> {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                            FilledTonalButton(onClick = onAddPayment) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Novo pagamento")
                            }
                        }
                    }
                    if (payments.isEmpty()) {
                        item { EmptyState("Nenhum pagamento registrado", Modifier.height(200.dp)) }
                    } else {
                        items(payments) { payment ->
                            PaymentItemCard(
                                payment = payment,
                                onEdit = { onEditPayment(payment.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AppointmentProntuarioCard(
    appointment: Appointment,
    prontuarioEntries: List<ProntuarioEntry>,
    onEditAppointment: () -> Unit,
    onOpenProntuario: () -> Unit,
    onEditProntuario: (Long) -> Unit,
    onDeleteEntry: (ProntuarioEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(appointment.procedureType, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(appointment.dateTime.toFormattedDateTime(), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (appointment.notes.isNotBlank()) {
                        Text(appointment.notes, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(appointment.status.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        IconButton(onClick = onEditAppointment, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar consulta",
                                modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onOpenProntuario, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.MedicalServices, contentDescription = "Adicionar anotação",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (prontuarioEntries.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                prontuarioEntries.forEach { entry ->
                    InlineProntuarioEntry(
                        entry = entry,
                        onEdit = { onEditProntuario(entry.id) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                    if (entry != prontuarioEntries.last()) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InlineProntuarioEntry(
    entry: ProntuarioEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    if (showFullImage && entry.imagePath != null) {
        Dialog(
            onDismissRequest = { showFullImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { showFullImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(entry.imagePath),
                    contentDescription = "Imagem expandida",
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar",
                            tint = Color.White, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir anotação") },
            text = { Text("Deseja excluir esta anotação do prontuário?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.createdAt.toFormattedDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Excluir",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            if (entry.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(entry.text, style = MaterialTheme.typography.bodyMedium)
            }
            entry.imagePath?.let { path ->
                Spacer(Modifier.height(6.dp))
                AsyncImage(
                    model = File(path),
                    contentDescription = "Imagem do prontuário",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullImage = true },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun PatientInfoCard(patient: com.meuconsultorio.data.entity.Patient, totalCost: Double) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(patient.name.take(2).uppercase(), style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(patient.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (patient.birthDate.isNotBlank())
                        Text(patient.birthDate, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (patient.phone.isNotBlank() || patient.cpf.isNotBlank() || patient.email.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                if (patient.phone.isNotBlank()) InfoRow(Icons.Filled.Phone, patient.phone)
                if (patient.email.isNotBlank()) InfoRow(Icons.Filled.Email, patient.email)
                if (patient.cpf.isNotBlank()) InfoRow(Icons.Filled.Badge, patient.cpf)
                if (patient.address.isNotBlank()) InfoRow(Icons.Filled.LocationOn, patient.address)
                if (totalCost > 0) {
                    InfoRow(Icons.Filled.AttachMoney, "Total em tratamentos: ${totalCost.toCurrency()}")
                }
            }

            if (patient.notes.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Observações:", style = MaterialTheme.typography.labelMedium)
                Text(patient.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TreatmentItemCard(treatment: Treatment, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(treatment.procedure, style = MaterialTheme.typography.titleMedium)
                if (treatment.tooth.isNotBlank())
                    Text("Dente: ${treatment.tooth}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(treatment.date.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(treatment.cost.toCurrency(), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(treatment.status.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PaymentItemCard(payment: Payment, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(payment.description, style = MaterialTheme.typography.titleMedium)
                Text(payment.method.label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(payment.date.toFormattedDate(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(payment.amount.toCurrency(), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (payment.isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                Text(if (payment.isPaid) "Pago" else "Pendente",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (payment.isPaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ProntuarioEntryCard(
    entry: ProntuarioEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    InlineProntuarioEntry(entry = entry, onEdit = onEdit, onDelete = onDelete)
}
