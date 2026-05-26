package com.meuconsultorio.ui.appointments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AppointmentDetailScreen(
    appointmentId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onPatientClick: (Long) -> Unit,
    viewModel: AppointmentViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel()
) {
    val appointment by viewModel.selectedAppointment.collectAsState()
    val patients by patientViewModel.patients.collectAsState()
    val context = LocalContext.current

    var imageToView by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appointmentId) {
        viewModel.loadAppointment(appointmentId)
    }

    // Fullscreen image viewer
    imageToView?.let { path ->
        Dialog(
            onDismissRequest = { imageToView = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { imageToView = null }
            ) {
                AsyncImage(
                    model = Uri.fromFile(File(path)),
                    contentDescription = "Anexo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { imageToView = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Close, "Fechar", tint = Color.White)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Detalhes da Consulta",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Editar consulta",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        val appt = appointment
        if (appt == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val patient = patients.find { it.id == appt.patientId }
        val attachments = appt.attachments.split(";").filter { it.isNotBlank() }
        val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()) }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Paciente ────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { patient?.let { onPatientClick(it.id) } }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Person, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Paciente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            patient?.name ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (patient != null) {
                        Icon(
                            Icons.Filled.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Procedimento + Status ────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(
                        icon = Icons.Filled.MedicalServices,
                        label = "Procedimento",
                        value = appt.procedureType
                    )
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(statusColor(appt.status))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(appt.status.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // ── Data / Duração ──────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(
                        icon = Icons.Filled.CalendarMonth,
                        label = "Data e hora",
                        value = dateFmt.format(Date(appt.dateTime))
                    )
                    HorizontalDivider()
                    DetailRow(
                        icon = Icons.Filled.Schedule,
                        label = "Duração",
                        value = "${appt.durationMinutes} minutos"
                    )
                }
            }

            // ── Valor + Pagamento ───────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(
                        icon = Icons.Filled.AttachMoney,
                        label = "Valor",
                        value = if (appt.price > 0) "R$ ${"%.2f".format(appt.price)}"
                                else "Não informado"
                    )
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (appt.isPaid) Icons.Filled.CheckCircle
                            else Icons.Filled.RadioButtonUnchecked,
                            null,
                            tint = if (appt.isPaid) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (appt.isPaid) "Consulta paga" else "Consulta não paga",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── Observações ─────────────────────────────────────────────
            if (appt.notes.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Notes, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Observações",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(appt.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ── Google Calendar ─────────────────────────────────────────
            if (appt.calendarEventId > 0L) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth, null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sincronizado com Google Calendar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── Anexos ──────────────────────────────────────────────────
            if (attachments.isNotEmpty()) {
                Text(
                    "Anexos (${attachments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                attachments.forEach { path ->
                    val file = File(path)
                    val isPdf = path.endsWith(".pdf", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPdf) {
                                    try {
                                        val contentUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, "application/pdf")
                                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                                    Intent.FLAG_ACTIVITY_NO_HISTORY
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Abrir PDF")
                                        )
                                    } catch (_: Exception) {}
                                } else {
                                    imageToView = path
                                }
                            }
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPdf) {
                                Icon(
                                    Icons.Filled.PictureAsPdf, null,
                                    modifier = Modifier.size(52.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else {
                                AsyncImage(
                                    model = Uri.fromFile(file),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                                Text(
                                    if (isPdf) "Toque para abrir" else "Toque para visualizar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.OpenInNew, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
