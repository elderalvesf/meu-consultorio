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
import com.meuconsultorio.data.entity.Treatment
import com.meuconsultorio.ui.components.*
import com.meuconsultorio.viewmodel.AppointmentViewModel
import com.meuconsultorio.viewmodel.PatientViewModel
import com.meuconsultorio.viewmodel.TreatmentViewModel

@Composable
fun FinancialScreen(
    onPatientClick: (Long) -> Unit,
    patientViewModel: PatientViewModel = hiltViewModel(),
    treatmentViewModel: TreatmentViewModel = hiltViewModel(),
    appointmentViewModel: AppointmentViewModel = hiltViewModel()
) {
    val patients by patientViewModel.patients.collectAsState()
    val allTreatments by treatmentViewModel.allTreatments.collectAsState()
    val totalTreatmentCost by treatmentViewModel.totalTreatmentCost.collectAsState()
    val totalTreatmentPrice by treatmentViewModel.totalTreatmentPrice.collectAsState()
    val totalAppointmentPrice by appointmentViewModel.totalAppointmentPrice.collectAsState()

    val patientMap = patients.associateBy { it.id }
    val totalRevenue = totalTreatmentPrice + totalAppointmentPrice

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
                        icon = Icons.Filled.MedicalServices,
                        label = "Valor tratamentos",
                        value = totalRevenue.toCurrency(),
                        color = MaterialTheme.colorScheme.secondary
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
