package com.meuconsultorio.ui.prontuario

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.meuconsultorio.data.entity.ProntuarioEntry
import com.meuconsultorio.ui.components.AppTopBar
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.ProntuarioViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProntuarioFormScreen(
    patientId: Long,
    appointmentId: Long?,
    entryId: Long?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProntuarioViewModel = hiltViewModel(),
    patientViewModel: PatientViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedEntry by viewModel.selectedEntry.collectAsState()
    val patient by patientViewModel.selectedPatient.collectAsState()
    val appointments by appointmentViewModel.patientAppointments.collectAsState()

    var text by remember { mutableStateOf("") }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    var previewUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAppointmentId by remember { mutableStateOf(appointmentId) }
    var showAppointmentDropdown by remember { mutableStateOf(false) }
    var isLoadingImage by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isLoadingImage = true
                val path = viewModel.saveImageToInternalStorage(context, it)
                if (path != null) {
                    savedImagePath?.let { oldPath ->
                        val oldFile = File(oldPath)
                        if (oldFile.exists()) oldFile.delete()
                    }
                    savedImagePath = path
                    previewUri = Uri.fromFile(File(path))
                }
                isLoadingImage = false
            }
        }
    }

    LaunchedEffect(patientId) {
        patientViewModel.loadPatient(patientId)
        appointmentViewModel.loadPatientAppointments(patientId)
    }

    LaunchedEffect(entryId) {
        if (entryId != null) viewModel.loadEntry(entryId)
    }

    LaunchedEffect(selectedEntry) {
        selectedEntry?.let { entry ->
            if (entryId != null && text.isEmpty() && savedImagePath == null) {
                text = entry.text
                savedImagePath = entry.imagePath
                selectedAppointmentId = entry.appointmentId
                entry.imagePath?.let { path -> previewUri = Uri.fromFile(File(path)) }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (entryId != null) "Editar Prontuário" else "Nova Entrada de Prontuário",
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (text.isBlank() && savedImagePath == null) return@ExtendedFloatingActionButton
                    val entry = ProntuarioEntry(
                        id = entryId ?: 0L,
                        patientId = patientId,
                        appointmentId = selectedAppointmentId,
                        text = text.trim(),
                        imagePath = savedImagePath
                    )
                    viewModel.saveEntry(entry) { onSave() }
                },
                icon = { Icon(Icons.Filled.Save, contentDescription = null) },
                text = { Text("Salvar") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Paciente vinculado
            patient?.let { p ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // Seletor de consulta
            ExposedDropdownMenuBox(
                expanded = showAppointmentDropdown,
                onExpandedChange = { showAppointmentDropdown = it }
            ) {
                OutlinedTextField(
                    value = appointments.find { it.id == selectedAppointmentId }?.let {
                        "${it.procedureType} — ${formatDate(it.dateTime)}"
                    } ?: "Nenhuma consulta (opcional)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Consulta relacionada") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAppointmentDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = showAppointmentDropdown,
                    onDismissRequest = { showAppointmentDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma (entrada geral)") },
                        onClick = { selectedAppointmentId = null; showAppointmentDropdown = false }
                    )
                    appointments.forEach { appt ->
                        DropdownMenuItem(
                            text = { Text("${appt.procedureType} — ${formatDate(appt.dateTime)}") },
                            onClick = { selectedAppointmentId = appt.id; showAppointmentDropdown = false }
                        )
                    }
                }
            }

            // Campo de texto
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Anotações / Evolução clínica") },
                placeholder = { Text("Descreva o procedimento, observações do paciente...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            // Seção de imagem
            Text("Imagem", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            when {
                isLoadingImage -> {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                previewUri != null -> {
                    Box(Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = "Imagem do prontuário",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                savedImagePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) file.delete()
                                }
                                savedImagePath = null
                                previewUri = null
                            },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remover imagem",
                                    modifier = Modifier.padding(4.dp).size(20.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Trocar imagem")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null,
                                modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Selecionar imagem")
                        }
                    }
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}

private fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR"))
    return sdf.format(java.util.Date(millis))
}
