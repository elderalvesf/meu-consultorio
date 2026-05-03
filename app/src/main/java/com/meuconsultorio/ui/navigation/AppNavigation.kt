package com.meuconsultorio.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.meuconsultorio.ui.appointments.AppointmentFormScreen
import com.meuconsultorio.ui.appointments.AppointmentListScreen
import com.meuconsultorio.ui.auth.LoginScreen
import com.meuconsultorio.ui.financial.FinancialScreen
import com.meuconsultorio.ui.financial.PaymentFormScreen
import com.meuconsultorio.ui.home.HomeScreen
import com.meuconsultorio.ui.patients.PatientDetailScreen
import com.meuconsultorio.ui.patients.PatientFormScreen
import com.meuconsultorio.ui.patients.PatientListScreen
import com.meuconsultorio.ui.prontuario.ProntuarioFormScreen
import com.meuconsultorio.ui.treatments.TreatmentFormScreen
import com.meuconsultorio.ui.util.isTablet
import com.meuconsultorio.viewmodel.AuthViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Início", Icons.Filled.Home)
    object Patients : Screen("patients", "Pacientes", Icons.Filled.People)
    object Appointments : Screen("appointments", "Agenda", Icons.Filled.CalendarMonth)
    object Financial : Screen("financial", "Financeiro", Icons.Filled.AttachMoney)

    object PatientDetail : Screen("patients/{patientId}", "Paciente", Icons.Filled.Person) {
        fun createRoute(id: Long) = "patients/$id"
    }
    object PatientForm : Screen("patient-form?patientId={patientId}", "Paciente", Icons.Filled.Person) {
        fun createRoute(id: Long? = null) = if (id != null) "patient-form?patientId=$id" else "patient-form"
    }
    object AppointmentForm : Screen(
        "appointment-form?appointmentId={appointmentId}&patientId={patientId}",
        "Consulta", Icons.Filled.Add
    ) {
        fun createRoute(appointmentId: Long? = null, patientId: Long? = null): String {
            val params = buildList {
                if (appointmentId != null) add("appointmentId=$appointmentId")
                if (patientId != null) add("patientId=$patientId")
            }.joinToString("&")
            return if (params.isNotEmpty()) "appointment-form?$params" else "appointment-form"
        }
    }
    object TreatmentForm : Screen(
        "treatment-form?treatmentId={treatmentId}&patientId={patientId}",
        "Tratamento", Icons.Filled.Add
    ) {
        fun createRoute(treatmentId: Long? = null, patientId: Long? = null): String {
            val params = buildList {
                if (treatmentId != null) add("treatmentId=$treatmentId")
                if (patientId != null) add("patientId=$patientId")
            }.joinToString("&")
            return if (params.isNotEmpty()) "treatment-form?$params" else "treatment-form"
        }
    }
    object PaymentForm : Screen(
        "payment-form?paymentId={paymentId}&patientId={patientId}",
        "Pagamento", Icons.Filled.Add
    ) {
        fun createRoute(paymentId: Long? = null, patientId: Long? = null): String {
            val params = buildList {
                if (paymentId != null) add("paymentId=$paymentId")
                if (patientId != null) add("patientId=$patientId")
            }.joinToString("&")
            return if (params.isNotEmpty()) "payment-form?$params" else "payment-form"
        }
    }
    object ProntuarioForm : Screen(
        "prontuario-form?patientId={patientId}&appointmentId={appointmentId}&entryId={entryId}",
        "Prontuário", Icons.Filled.MedicalServices
    ) {
        fun createRoute(
            patientId: Long,
            appointmentId: Long? = null,
            entryId: Long? = null
        ): String {
            val params = buildList {
                add("patientId=$patientId")
                if (appointmentId != null) add("appointmentId=$appointmentId")
                if (entryId != null) add("entryId=$entryId")
            }.joinToString("&")
            return "prontuario-form?$params"
        }
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Patients, Screen.Appointments, Screen.Financial)

@Composable
fun AppNavigation(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthViewModel.AuthState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        is AuthViewModel.AuthState.Unauthenticated -> {
            LoginScreen(authViewModel)
            return
        }
        else -> Unit
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tablet = isTablet()

    val isTopLevel = bottomNavItems.any { it.route == currentDestination?.route }

    val navContent: @Composable (PaddingValues) -> Unit = { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPatients = { navController.navigate(Screen.Patients.route) },
                    onNavigateToAppointments = { navController.navigate(Screen.Appointments.route) },
                    onNavigateToAppointmentForm = { navController.navigate(Screen.AppointmentForm.createRoute()) }
                )
            }
            composable(Screen.Patients.route) {
                PatientListScreen(
                    onPatientClick = { navController.navigate(Screen.PatientDetail.createRoute(it)) },
                    onAddPatient = { navController.navigate(Screen.PatientForm.createRoute()) }
                )
            }
            composable(
                route = Screen.PatientDetail.route,
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStack ->
                val patientId = backStack.arguments?.getLong("patientId") ?: 0L
                PatientDetailScreen(
                    patientId = patientId,
                    onEdit = { navController.navigate(Screen.PatientForm.createRoute(patientId)) },
                    onAddAppointment = { navController.navigate(Screen.AppointmentForm.createRoute(patientId = patientId)) },
                    onEditAppointment = { navController.navigate(Screen.AppointmentForm.createRoute(appointmentId = it)) },
                    onAddTreatment = { navController.navigate(Screen.TreatmentForm.createRoute(patientId = patientId)) },
                    onEditTreatment = { navController.navigate(Screen.TreatmentForm.createRoute(treatmentId = it, patientId = patientId)) },
                    onOpenProntuario = { appointmentId ->
                        navController.navigate(Screen.ProntuarioForm.createRoute(patientId, appointmentId = appointmentId))
                    },
                    onEditProntuario = { entryId ->
                        navController.navigate(Screen.ProntuarioForm.createRoute(patientId, entryId = entryId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.PatientForm.route,
                arguments = listOf(navArgument("patientId") { type = NavType.LongType; defaultValue = 0L })
            ) { backStack ->
                val patientId = backStack.arguments?.getLong("patientId")?.takeIf { it != 0L }
                PatientFormScreen(
                    patientId = patientId,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Appointments.route) {
                AppointmentListScreen(
                    onAddAppointment = { navController.navigate(Screen.AppointmentForm.createRoute()) },
                    onEditAppointment = { navController.navigate(Screen.AppointmentForm.createRoute(appointmentId = it)) },
                    onPatientClick = { navController.navigate(Screen.PatientDetail.createRoute(it)) }
                )
            }
            composable(
                route = Screen.AppointmentForm.route,
                arguments = listOf(
                    navArgument("appointmentId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("patientId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStack ->
                val appointmentId = backStack.arguments?.getLong("appointmentId")?.takeIf { it != 0L }
                val patientId = backStack.arguments?.getLong("patientId")?.takeIf { it != 0L }
                AppointmentFormScreen(
                    appointmentId = appointmentId,
                    preselectedPatientId = patientId,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.TreatmentForm.route,
                arguments = listOf(
                    navArgument("treatmentId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("patientId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStack ->
                val treatmentId = backStack.arguments?.getLong("treatmentId")?.takeIf { it != 0L }
                val patientId = backStack.arguments?.getLong("patientId")?.takeIf { it != 0L }
                TreatmentFormScreen(
                    treatmentId = treatmentId,
                    preselectedPatientId = patientId,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Financial.route) {
                FinancialScreen(
                    onPatientClick = { navController.navigate(Screen.PatientDetail.createRoute(it)) }
                )
            }
            composable(
                route = Screen.PaymentForm.route,
                arguments = listOf(
                    navArgument("paymentId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("patientId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStack ->
                val paymentId = backStack.arguments?.getLong("paymentId")?.takeIf { it != 0L }
                val patientId = backStack.arguments?.getLong("patientId")?.takeIf { it != 0L }
                PaymentFormScreen(
                    paymentId = paymentId,
                    preselectedPatientId = patientId,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.ProntuarioForm.route,
                arguments = listOf(
                    navArgument("patientId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("appointmentId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("entryId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStack ->
                val patientId = backStack.arguments?.getLong("patientId") ?: 0L
                val appointmentId = backStack.arguments?.getLong("appointmentId")?.takeIf { it != 0L }
                val entryId = backStack.arguments?.getLong("entryId")?.takeIf { it != 0L }
                ProntuarioFormScreen(
                    patientId = patientId,
                    appointmentId = appointmentId,
                    entryId = entryId,
                    onSave = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (tablet && isTopLevel) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        Icons.Filled.MedicalServices,
                        contentDescription = "App",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationRailItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                navContent(PaddingValues(0.dp))
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            navContent(innerPadding)
        }
    }
}
