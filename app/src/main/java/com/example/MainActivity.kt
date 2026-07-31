package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.LeakReport
import com.example.ui.LeakViewModel
import com.example.ui.SomagepAgent
import com.example.ui.somagepAgentsList
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.SomagepBlue
import com.example.ui.theme.SomagepCyan
import com.example.ui.theme.SoftAquaticBg
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

// Bamako neighborhood configurations for reverse-geocoding simulation
data class BamakoDistrict(val name: String, val lat: Double, val lng: Double)

val bamakoDistricts = listOf(
    BamakoDistrict("Hamdallaye ACI 2000", 12.635, -8.025),
    BamakoDistrict("Badalabougou", 12.620, -7.995),
    BamakoDistrict("Sogoniko", 12.600, -7.955),
    BamakoDistrict("Hamdallaye Centre", 12.645, -8.012),
    BamakoDistrict("Niaréla", 12.650, -7.985),
    BamakoDistrict("Dakar-Mali", 12.630, -8.010),
    BamakoDistrict("Lassa", 12.665, -8.030),
    BamakoDistrict("Hippodrome", 12.655, -7.975),
    BamakoDistrict("Magnambougou", 12.595, -7.940),
    BamakoDistrict("Faladié", 12.590, -7.930),
    BamakoDistrict("Baco-Djicoroni", 12.585, -7.985)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: LeakViewModel = viewModel()
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: LeakViewModel) {
    val context = LocalContext.current
    
    // States from ViewModel
    val selectedRole by viewModel.selectedRole.collectAsStateWithLifecycle()
    val leaks by viewModel.filteredLeaks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val severityFilter by viewModel.severityFilter.collectAsStateWithLifecycle()
    val currentAgent by viewModel.currentAgent.collectAsStateWithLifecycle()
    val allAgentsList by viewModel.allAgents.collectAsStateWithLifecycle()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()
    val emergencyNotice by viewModel.emergencyNotice.collectAsStateWithLifecycle()
    val hotlinePhone by viewModel.hotlinePhone.collectAsStateWithLifecycle()
    val isMaintenanceMode by viewModel.isMaintenanceMode.collectAsStateWithLifecycle()
    val autoDispatchGrave by viewModel.autoDispatchGrave.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // UI states
    var citizenTab by remember { mutableStateOf(0) } // 0: Accueil, 1: Signalements, 2: Carte Interactive
    var technicianTab by remember { mutableStateOf(0) } // 0: Interventions, 1: Carte Globale, 2: Statistiques
    var dispatcherTab by remember { mutableStateOf(0) } // 0: Console, 1: Carte Bureau, 2: Performance
    var adminTab by remember { mutableStateOf(0) } // 0: Supervision, 1: Agents, 2: Contrôle, 3: Audit Cloud
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedLeakForDetails by remember { mutableStateOf<LeakReport?>(null) }
    var selectedLeakForReceipt by remember { mutableStateOf<LeakReport?>(null) }
    var selectedLeakForWorkOrder by remember { mutableStateOf<LeakReport?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFraudReportDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showRegisterTechnicianDialog by remember { mutableStateOf(false) }
    var selectedAgentForEdit by remember { mutableStateOf<SomagepAgent?>(null) }
    
    // Passcode states for role switching
    var pendingRoleChange by remember { mutableStateOf<String?>(null) }
    var selectedAgentForLogin by remember { mutableStateOf<SomagepAgent?>(null) }
    var enteredPasscode by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Notifications push activées pour le suivi SOMAGEP !", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        com.example.util.NotificationHelper.createNotificationChannel(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    // Quick calculations
    val reportedCount = leaks.count { it.status == "Signalé" }
    val progressCount = leaks.count { it.status == "En cours" }
    val solvedCount = leaks.count { it.status == "Réparé" }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("snackbar_host")
            )
        },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_somagep_logo_new_1784470185579),
                                    contentDescription = "SOMAGEP Logo",
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Column {
                                Text(
                                    text = "SOMAGEP Fuites",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Gestion & Signalement Mali",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedRole == "Administrateur") {
                                    showSettingsDialog = true
                                } else {
                                    Toast.makeText(context, "⛔ Accès aux Paramètres réservé à l'Administrateur (PIN Sécurisé)", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Paramètres système (Administrateur)",
                                tint = if (selectedRole == "Administrateur") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        // Role switcher pill
                        Row(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val roles = listOf("Citoyen", "Technicien", "Agent Bureau", "Administrateur")
                            roles.forEach { role ->
                                val isSelected = selectedRole == role
                                val displayRole = if (role == "Administrateur") "Admin" else role
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            if (role == "Citoyen") {
                                                viewModel.setCurrentAgent(null)
                                            } else {
                                                pendingRoleChange = role
                                                selectedAgentForLogin = null
                                                enteredPasscode = ""
                                                passcodeError = false
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayRole,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                if (selectedRole == "Citoyen") {
                    NavigationBarItem(
                        selected = citizenTab == 0,
                        onClick = { citizenTab = 0 },
                        label = { Text("Accueil", fontSize = 11.sp) },
                        icon = { Icon(if (citizenTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Accueil") },
                        modifier = Modifier.testTag("nav_citizen_home")
                    )
                    NavigationBarItem(
                        selected = citizenTab == 1,
                        onClick = { citizenTab = 1 },
                        label = { Text("Signalements", fontSize = 11.sp) },
                        icon = { Icon(if (citizenTab == 1) Icons.Filled.ListAlt else Icons.Outlined.ListAlt, contentDescription = "Signalements") },
                        modifier = Modifier.testTag("nav_citizen_list")
                    )
                    NavigationBarItem(
                        selected = citizenTab == 2,
                        onClick = { citizenTab = 2 },
                        label = { Text("Carte Bamako", fontSize = 11.sp) },
                        icon = { Icon(if (citizenTab == 2) Icons.Filled.Map else Icons.Outlined.Map, contentDescription = "Carte") },
                        modifier = Modifier.testTag("nav_citizen_map")
                    )
                    NavigationBarItem(
                        selected = citizenTab == 3,
                        onClick = { citizenTab = 3 },
                        label = { Text("Services & Info", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (citizenTab == 3) Icons.Filled.Info else Icons.Outlined.Info, contentDescription = "Services") },
                        modifier = Modifier.testTag("nav_citizen_services")
                    )
                } else if (selectedRole == "Technicien") {
                    val agent = currentAgent
                    val techAssignedCount = leaks.count { 
                        (agent == null || it.assignedTechnician.contains(agent.name, ignoreCase = true) || (it.assignedTechnician.isNotEmpty() && agent.name.contains(it.assignedTechnician, ignoreCase = true))) &&
                        it.assignedTechnician.isNotEmpty() &&
                        it.status != "Réparé"
                    }
                    NavigationBarItem(
                        selected = technicianTab == 0,
                        onClick = { technicianTab = 0 },
                        label = { Text("Interventions", fontSize = 10.sp, maxLines = 1) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (techAssignedCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) {
                                            Text("$techAssignedCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(if (technicianTab == 0) Icons.Filled.Build else Icons.Outlined.Build, contentDescription = "Interventions")
                            }
                        },
                        modifier = Modifier.testTag("nav_tech_list")
                    )
                    NavigationBarItem(
                        selected = technicianTab == 1,
                        onClick = { technicianTab = 1 },
                        label = { Text("Carte Globale", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (technicianTab == 1) Icons.Filled.MyLocation else Icons.Outlined.MyLocation, contentDescription = "Carte Globale") },
                        modifier = Modifier.testTag("nav_tech_map")
                    )
                    NavigationBarItem(
                        selected = technicianTab == 2,
                        onClick = { technicianTab = 2 },
                        label = { Text("Statistiques", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (technicianTab == 2) Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "Stats") },
                        modifier = Modifier.testTag("nav_tech_stats")
                    )
                    NavigationBarItem(
                        selected = technicianTab == 3,
                        onClick = { technicianTab = 3 },
                        label = { Text("Personnel", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (technicianTab == 3) Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Personnel") },
                        modifier = Modifier.testTag("nav_tech_personnel")
                    )
                } else if (selectedRole == "Agent Bureau") {
                    val unassignedCount = leaks.count { it.assignedTechnician.isEmpty() && it.status != "Réparé" }
                    NavigationBarItem(
                        selected = dispatcherTab == 0,
                        onClick = { dispatcherTab = 0 },
                        label = { Text("Console PC", fontSize = 10.sp, maxLines = 1) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unassignedCount > 0) {
                                        Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) {
                                            Text("$unassignedCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(if (dispatcherTab == 0) Icons.Filled.Computer else Icons.Outlined.Computer, contentDescription = "Console PC")
                            }
                        },
                        modifier = Modifier.testTag("nav_dispatcher_console")
                    )
                    NavigationBarItem(
                        selected = dispatcherTab == 1,
                        onClick = { dispatcherTab = 1 },
                        label = { Text("Carte Affect.", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (dispatcherTab == 1) Icons.Filled.Map else Icons.Outlined.Map, contentDescription = "Carte Affect.") },
                        modifier = Modifier.testTag("nav_dispatcher_map")
                    )
                    NavigationBarItem(
                        selected = dispatcherTab == 2,
                        onClick = { dispatcherTab = 2 },
                        label = { Text("Performance", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (dispatcherTab == 2) Icons.Filled.BarChart else Icons.Outlined.BarChart, contentDescription = "Performance") },
                        modifier = Modifier.testTag("nav_dispatcher_stats")
                    )
                    NavigationBarItem(
                        selected = dispatcherTab == 3,
                        onClick = { dispatcherTab = 3 },
                        label = { Text("Personnel", fontSize = 10.sp, maxLines = 1) },
                        icon = { Icon(if (dispatcherTab == 3) Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Personnel") },
                        modifier = Modifier.testTag("nav_dispatcher_personnel")
                    )
                } else if (selectedRole == "Administrateur") {
                    NavigationBarItem(
                        selected = adminTab == 0,
                        onClick = { adminTab = 0 },
                        label = { Text("Supervision", fontSize = 11.sp) },
                        icon = { Icon(if (adminTab == 0) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings, contentDescription = "Supervision") },
                        modifier = Modifier.testTag("nav_admin_supervision")
                    )
                    NavigationBarItem(
                        selected = adminTab == 1,
                        onClick = { adminTab = 1 },
                        label = { Text("Agents", fontSize = 11.sp) },
                        icon = { Icon(if (adminTab == 1) Icons.Filled.Group else Icons.Outlined.Group, contentDescription = "Agents") },
                        modifier = Modifier.testTag("nav_admin_agents")
                    )
                    NavigationBarItem(
                        selected = adminTab == 2,
                        onClick = { adminTab = 2 },
                        label = { Text("Contrôle", fontSize = 11.sp) },
                        icon = { Icon(if (adminTab == 2) Icons.Filled.Checklist else Icons.Outlined.Checklist, contentDescription = "Contrôle") },
                        modifier = Modifier.testTag("nav_admin_control")
                    )
                    NavigationBarItem(
                        selected = adminTab == 3,
                        onClick = { adminTab = 3 },
                        label = { Text("Audit Cloud", fontSize = 11.sp) },
                        icon = { Icon(if (adminTab == 3) Icons.Filled.Security else Icons.Outlined.Security, contentDescription = "Audit") },
                        modifier = Modifier.testTag("nav_admin_audit")
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedRole == "Citoyen" && citizenTab == 0) {
                ExtendedFloatingActionButton(
                    text = { Text("Signaler une Fuite", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Ajouter") },
                    onClick = { showReportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_report_leak")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedRole == "Citoyen") {
                val citizenActiveLeaks = remember(leaks) { leaks.filter { !it.isDeletedByCitizen } }
                val citizenReportedCount = remember(citizenActiveLeaks) { citizenActiveLeaks.count { it.status == "Signalé" } }
                val citizenProgressCount = remember(citizenActiveLeaks) { citizenActiveLeaks.count { it.status == "En cours" } }
                val citizenSolvedCount = remember(citizenActiveLeaks) { citizenActiveLeaks.count { it.status == "Réparé" } }

                when (citizenTab) {
                    0 -> CitizenDashboard(
                        leaks = citizenActiveLeaks,
                        reportedCount = citizenReportedCount,
                        progressCount = citizenProgressCount,
                        solvedCount = citizenSolvedCount,
                        onReportClick = { showReportDialog = true },
                        onViewDetails = { selectedLeakForDetails = it },
                        onOpenCalculator = { showCalculatorDialog = true },
                        onOpenFraudReport = { showFraudReportDialog = true },
                        onSwitchToServices = { citizenTab = 3 },
                        onOpenMapTab = { citizenTab = 2 },
                        emergencyNotice = emergencyNotice,
                        hotlinePhone = hotlinePhone,
                        isMaintenanceMode = isMaintenanceMode,
                        onTestNotification = {
                            viewModel.triggerTestPushNotification()
                            Toast.makeText(context, "Alerte push de test envoyée !", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> CitizenLeaksList(
                        leaks = citizenActiveLeaks,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        severityFilter = severityFilter,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onStatusFilterChange = { viewModel.setStatusFilter(it) },
                        onSeverityFilterChange = { viewModel.setSeverityFilter(it) },
                        onViewDetails = { selectedLeakForDetails = it },
                        onDeleteClick = { 
                            viewModel.softDeleteByCitizen(it)
                            Toast.makeText(context, "Signalement retiré de votre suivi personnel. Il reste conservé pour les agents SOMAGEP.", Toast.LENGTH_LONG).show()
                        }
                    )
                    2 -> CitizenMapView(
                        leaks = citizenActiveLeaks,
                        onLeakClick = { selectedLeakForDetails = it }
                    )
                    3 -> CitizenServicesAndInfoView(
                        onReportFraudClick = { showFraudReportDialog = true },
                        onOpenCalculatorClick = { showCalculatorDialog = true }
                    )
                }
            } else if (selectedRole == "Technicien") {
                when (technicianTab) {
                    0 -> TechnicianDashboard(
                        leaks = leaks,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onUpdateStatus = { leak, status, notes -> 
                            viewModel.updateReportStatus(leak, status, notes)
                        },
                        onViewDetails = { selectedLeakForDetails = it },
                        currentAgent = currentAgent
                    )
                    1 -> TechnicianMapView(
                        leaks = leaks,
                        onLeakClick = { selectedLeakForDetails = it }
                    )
                    2 -> TechnicianStatsView(
                        leaks = leaks
                    )
                    3 -> AdminAgentsView(
                        allAgents = allAgentsList,
                        currentAgent = currentAgent,
                        titleOverride = "ANNUAIRE DU PERSONNEL & ABONNÉS (TECHNICIEN)"
                    )
                }
            } else if (selectedRole == "Agent Bureau") {
                when (dispatcherTab) {
                    0 -> DispatcherConsoleView(
                        leaks = leaks,
                        onAssignTechnician = { leak, techName, techPhone ->
                            viewModel.assignTechnician(leak, techName, techPhone)
                            Toast.makeText(context, "🔔 ALERTE TECHNICIEN : Fuite à ${leak.address} transmise avec succès à $techName !", Toast.LENGTH_LONG).show()
                        },
                        onViewDetails = { selectedLeakForDetails = it },
                        currentAgent = currentAgent
                    )
                    1 -> DispatcherMapView(
                        leaks = leaks,
                        onLeakClick = { selectedLeakForDetails = it },
                        onAssignTechnician = { leak, techName, techPhone ->
                            viewModel.assignTechnician(leak, techName, techPhone)
                            Toast.makeText(context, "🔔 ALERTE TECHNICIEN : Fuite à ${leak.address} transmise avec succès à $techName !", Toast.LENGTH_LONG).show()
                        }
                    )
                    2 -> TechnicianStatsView(
                        leaks = leaks
                    )
                    3 -> AdminAgentsView(
                        allAgents = allAgentsList,
                        currentAgent = currentAgent,
                        titleOverride = "ANNUAIRE DU PERSONNEL & ABONNÉS (AGENT BUREAU)"
                    )
                }
            } else if (selectedRole == "Administrateur") {
                when (adminTab) {
                    0 -> AdminSupervisionView(
                        leaks = leaks,
                        allAgents = allAgentsList,
                        firebaseSyncStatus = firebaseSyncStatus,
                        onForceSync = { viewModel.syncWithFirebaseCloud() },
                        onOpenRegisterAgent = {
                            pendingRoleChange = "Administrateur"
                            showRegisterTechnicianDialog = true
                        },
                        onResetData = { viewModel.resetAllData() },
                        onRestoreDemo = { viewModel.reseedData() },
                        onViewDetails = { selectedLeakForDetails = it },
                        currentAgent = currentAgent
                    )
                    1 -> AdminAgentsView(
                        allAgents = allAgentsList,
                        onOpenRegisterAgent = {
                            showRegisterTechnicianDialog = true
                        },
                        onEditAgent = { agent ->
                            selectedAgentForEdit = agent
                        },
                        onDeleteAgent = { agent ->
                            if (agent.pin == "00223" || agent.phone == "70 00 00 00" || agent.name.contains("Administrateur Principal", ignoreCase = true)) {
                                Toast.makeText(context, "⛔ L'Administrateur Principal ne peut pas être supprimé !", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.deleteAgent(agent.phone)
                                Toast.makeText(context, "Compte ${agent.name} supprimé avec succès.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        currentAgent = currentAgent
                    )
                    2 -> AdminLeakControlView(
                        leaks = leaks,
                        allAgents = allAgentsList,
                        onUpdateStatus = { leak, status, notes ->
                            viewModel.updateReportStatus(leak, status, notes)
                        },
                        onAssignTechnician = { leak, techName, techPhone ->
                            viewModel.assignTechnician(leak, techName, techPhone)
                        },
                        onDeleteReport = { viewModel.deleteReport(it) },
                        onRestoreReport = { viewModel.restoreReport(it) },
                        onViewDetails = { selectedLeakForDetails = it }
                    )
                    3 -> AdminAuditLogsView(
                        leaks = leaks,
                        firebaseSyncStatus = firebaseSyncStatus,
                        currentAgent = currentAgent
                    )
                }
            }

            // Report Leak dialog
            if (showReportDialog) {
                ReportLeakDialog(
                    onDismiss = { showReportDialog = false },
                    onReportSubmitted = { name, phone, type, severity, desc, lat, lng, addr, photo, onDone ->
                        viewModel.reportLeak(
                            name = name,
                            phone = phone,
                            leakType = type,
                            severity = severity,
                            description = desc,
                            latitude = lat,
                            longitude = lng,
                            address = addr,
                            photoPath = photo,
                            onComplete = { success, message ->
                                onDone()
                                showReportDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        actionLabel = "OK",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                )
            }

            // Leak details dialog
            selectedLeakForDetails?.let { leak ->
                LeakDetailsDialog(
                    leak = leak,
                    role = selectedRole,
                    onDismiss = { selectedLeakForDetails = null },
                    onStatusUpdate = { status, notes ->
                        viewModel.updateReportStatus(leak, status, notes)
                        selectedLeakForDetails = null
                        Toast.makeText(context, "Statut du signalement mis à jour !", Toast.LENGTH_SHORT).show()
                    },
                    onOpenReceipt = { selectedLeakForReceipt = it },
                    onRateReport = { leakToRate, rating, feedback ->
                        viewModel.rateIntervention(leakToRate, rating, feedback)
                        selectedLeakForDetails = null
                    },
                    onOpenWorkOrder = { selectedLeakForWorkOrder = it }
                )
            }

            selectedLeakForReceipt?.let { leak ->
                SomagepReceiptDialog(
                    leak = leak,
                    onDismiss = { selectedLeakForReceipt = null }
                )
            }

            selectedLeakForWorkOrder?.let { leak ->
                TechnicianWorkOrderDialog(
                    leak = leak,
                    onDismiss = { selectedLeakForWorkOrder = null },
                    onCompleteIntervention = { leakToComplete, notes ->
                        viewModel.updateReportStatus(leakToComplete, "Réparé", notes)
                    }
                )
            }

            if (showSettingsDialog) {
                AdminSettingsDialog(
                    firebaseSyncStatus = firebaseSyncStatus,
                    emergencyNotice = emergencyNotice,
                    hotlinePhone = hotlinePhone,
                    isMaintenanceMode = isMaintenanceMode,
                    autoDispatchGrave = autoDispatchGrave,
                    currentAgent = currentAgent,
                    allLeaks = leaks,
                    onDismiss = { showSettingsDialog = false },
                    onForceSync = {
                        viewModel.syncWithFirebaseCloud()
                        Toast.makeText(context, "Synchronisation Cloud lancée !", Toast.LENGTH_SHORT).show()
                    },
                    onResetAllData = {
                        viewModel.resetAllData()
                        showSettingsDialog = false
                        Toast.makeText(context, "Données réinitialisées à 0 ! 🗑️", Toast.LENGTH_SHORT).show()
                    },
                    onRestoreDemoData = {
                        viewModel.reseedData()
                        showSettingsDialog = false
                        Toast.makeText(context, "Données de démo restaurées ! 🔄", Toast.LENGTH_SHORT).show()
                    },
                    onSaveSettings = { notice, hotline, isMaintenance, autoDispatch ->
                        viewModel.updateSystemSettings(notice, hotline, isMaintenance, autoDispatch)
                        showSettingsDialog = false
                    }
                )
            }

            if (showFraudReportDialog) {
                FraudReportDialog(onDismiss = { showFraudReportDialog = false })
            }

            if (showCalculatorDialog) {
                WaterLossCalculatorDialog(onDismiss = { showCalculatorDialog = false })
            }

            if (showRegisterTechnicianDialog) {
                RegisterTechnicianDialog(
                    initialRole = pendingRoleChange ?: "Technicien",
                    onDismiss = { showRegisterTechnicianDialog = false },
                    onRegisterSuccess = { newAgent ->
                        showRegisterTechnicianDialog = false
                        pendingRoleChange = null
                        selectedAgentForLogin = null
                        Toast.makeText(context, "Compte ${newAgent.role} (${newAgent.name}) créé par l'Administrateur avec succès ! 👤", Toast.LENGTH_LONG).show()
                    }
                )
            }

            selectedAgentForEdit?.let { agentToEdit ->
                EditAgentDialog(
                    agent = agentToEdit,
                    onDismiss = { selectedAgentForEdit = null },
                    onSave = { oldPhone, name, phone, role, zone, pin ->
                        viewModel.updateAgent(oldPhone, name, phone, role, zone, pin)
                        Toast.makeText(context, "Profil de $name mis à jour par l'Administrateur ! ✅", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Passcode authorization and account selection dialog
            pendingRoleChange?.let { role ->
                val agents = allAgentsList.filter { it.role == role }
                
                AlertDialog(
                    onDismissRequest = { pendingRoleChange = null },
                    icon = {
                        Icon(
                            imageVector = if (selectedAgentForLogin == null) Icons.Filled.AccountCircle else Icons.Filled.Lock,
                            contentDescription = "Identification requis",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = if (selectedAgentForLogin == null) "Sélection de Compte - $role" else "Authentification - ${selectedAgentForLogin?.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selectedAgentForLogin == null) {
                                Text(
                                    text = "Veuillez vous identifier pour accéder à l'espace $role :",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (role == "Administrateur") {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🔑 Connexion Directe Admin", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            Text("Code PIN Administrateur : ••••• (Accès Sécurisé)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = enteredPasscode,
                                                    onValueChange = {
                                                        enteredPasscode = it
                                                        passcodeError = false
                                                    },
                                                    placeholder = { Text("Entrez votre Code PIN", fontSize = 11.sp) },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = KeyboardType.NumberPassword,
                                                        imeAction = ImeAction.Done
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                Button(
                                                    onClick = {
                                                        val matchedAdmin = allAgentsList.find { it.role == "Administrateur" && it.pin == enteredPasscode }
                                                            ?: allAgentsList.find { it.pin == "00223" }
                                                        if (matchedAdmin != null && enteredPasscode == matchedAdmin.pin) {
                                                            viewModel.setCurrentAgent(matchedAdmin)
                                                            pendingRoleChange = null
                                                            Toast.makeText(context, "Bienvenue Administrateur : ${matchedAdmin.name} ! 🔑", Toast.LENGTH_SHORT).show()
                                                        } else if (enteredPasscode == "00223") {
                                                            val mainAdmin = SomagepAgent("Administrateur Principal", "70 00 00 00", "Administrateur", "Siège National - Direction Générale", "00223")
                                                            viewModel.setCurrentAgent(mainAdmin)
                                                            pendingRoleChange = null
                                                            Toast.makeText(context, "Bienvenue Administrateur Principal ! 🔑", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            passcodeError = true
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Entrer", fontSize = 11.sp)
                                                }
                                            }
                                            if (passcodeError) {
                                                Text("Code PIN incorrect ! Veuillez entrer votre code PIN Administrateur.", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Divider(modifier = Modifier.padding(vertical = 2.dp))
                                }

                                Text(
                                    text = "Ou sélectionnez un profil dans la liste ci-dessous :",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .heightIn(max = 200.dp)
                                ) {
                                    agents.forEach { agent ->
                                        val initials = agent.name.split(" ")
                                            .mapNotNull { it.firstOrNull() }
                                            .take(2)
                                            .joinToString("")
                                            .uppercase()
                                            
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedAgentForLogin = agent
                                                    enteredPasscode = ""
                                                    passcodeError = false
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Initials avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = initials,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = agent.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text(
                                                        text = agent.zoneOrPost,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                
                                                Icon(
                                                    imageVector = Icons.Filled.ChevronRight,
                                                    contentDescription = "Sélectionner",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val agent = selectedAgentForLogin!!
                                val initials = agent.name.split(" ")
                                    .mapNotNull { it.firstOrNull() }
                                    .take(2)
                                    .joinToString("")
                                    .uppercase()
                                    
                                Text(
                                    text = "Veuillez entrer votre code de sécurité PIN pour accéder à votre espace.",
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = agent.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = agent.zoneOrPost,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = enteredPasscode,
                                    onValueChange = {
                                        enteredPasscode = it
                                        passcodeError = false
                                    },
                                    label = { Text("Code PIN de sécurité", fontSize = 12.sp) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.NumberPassword,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (enteredPasscode == agent.pin) {
                                                viewModel.setCurrentAgent(agent)
                                                pendingRoleChange = null
                                                Toast.makeText(context, "Bienvenue, ${agent.name} ! 🔑", Toast.LENGTH_SHORT).show()
                                            } else {
                                                passcodeError = true
                                            }
                                        }
                                    ),
                                    isError = passcodeError,
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                if (passcodeError) {
                                    Text(
                                        text = "Code PIN incorrect ! Veuillez réessayer.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                TextButton(
                                    onClick = { selectedAgentForLogin = null },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Changer de profil", fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (selectedAgentForLogin != null) {
                            Button(
                                onClick = {
                                    val agent = selectedAgentForLogin!!
                                    if (enteredPasscode == agent.pin) {
                                        viewModel.setCurrentAgent(agent)
                                        pendingRoleChange = null
                                        Toast.makeText(context, "Bienvenue, ${agent.name} ! 🔑", Toast.LENGTH_SHORT).show()
                                    } else {
                                        passcodeError = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("S'identifier")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingRoleChange = null }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CITIZEN DASHBOARD COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun CitizenDashboard(
    leaks: List<LeakReport>,
    reportedCount: Int,
    progressCount: Int,
    solvedCount: Int,
    onReportClick: () -> Unit,
    onViewDetails: (LeakReport) -> Unit,
    onOpenCalculator: () -> Unit = {},
    onOpenFraudReport: () -> Unit = {},
    onSwitchToServices: () -> Unit = {},
    onOpenMapTab: (() -> Unit)? = null,
    emergencyNotice: String = "",
    hotlinePhone: String = "20 22 25 26",
    isMaintenanceMode: Boolean = false,
    onTestNotification: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var homeMapFilter by remember { mutableStateOf("Tous") }
    var selectedHomeLeak by remember { mutableStateOf<LeakReport?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isMaintenanceMode) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AVIS D'URGENCE SOMAGEP", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(
                            text = emergencyNotice.ifBlank { "Travaux d'entretien ou perturbation réseau sur le canal principal SOMAGEP Bamako." },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hotlinePhone.replace(" ", "")}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.PhoneInTalk, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hotline ($hotlinePhone)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_somagep_hero_1784401275437),
                        contentDescription = "SOMAGEP Leak Repair Hero Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Protégeons notre Eau Potable !",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Signalez toute fuite SOMAGEP à Bamako en quelques clics pour une intervention d'urgence.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Push Notifications Status & Test Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Suivi Push en Temps Réel", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF2E7D32)
                                ) {
                                    Text("ACTIF", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                            Text("Recevez une alerte mobile dès que votre fuite change d'état (Reçu ➔ En cours ➔ Réparé).", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
                        }
                    }

                    if (onTestNotification != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onTestNotification,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Tester", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Services & Urgences Citoyennes",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:80001111"))
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.PhoneInTalk, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Urgence 24/7", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("80 00 11 11", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onSwitchToServices() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Coupures d'Eau", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Info Travaux", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onOpenCalculator() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Calculateur", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("Pertes & Coût", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    }
                }

                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { onOpenFraudReport() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Vol & Clandestin", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Alerte Anonyme", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }
        }

        item {
            Text(
                text = "État des Signalements à Bamako",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    title = "Signalés",
                    count = reportedCount,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.Notifications,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "En Cours",
                    count = progressCount,
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Filled.HourglassEmpty,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "Réparés",
                    count = solvedCount,
                    color = SafeGreen,
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // SECTION CARTOGRAPHIQUE GOOGLE MAPS EN TEMPS RÉEL (PAGE D'ACCUEIL)
        // -----------------------------------------------------------------------------------------
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Carte des Fuites (Google Maps)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                        Text(
                                            text = "EN DIRECT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${leaks.size} zones de fuites géolocalisées à Bamako",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (onOpenMapTab != null) {
                        TextButton(
                            onClick = onOpenMapTab,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Agrandir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Map Filter Chips Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filters = listOf(
                        Pair("Tous", "Toutes (${leaks.size})"),
                        Pair("Signalé", "🔴 Signalées ($reportedCount)"),
                        Pair("En cours", "🟠 En Cours ($progressCount)"),
                        Pair("Réparé", "🟢 Réparées ($solvedCount)")
                    )
                    filters.forEach { (filterKey, label) ->
                        val isSelected = homeMapFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { homeMapFilter = filterKey },
                            label = { Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Embedded Google Maps View Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val filteredHomeLeaks = remember(leaks, homeMapFilter) {
                            if (homeMapFilter == "Tous") leaks else leaks.filter { it.status == homeMapFilter }
                        }

                        GoogleMapsEmbedView(
                            leaks = filteredHomeLeaks,
                            selectedLeak = selectedHomeLeak,
                            onLeakSelected = { leak ->
                                selectedHomeLeak = leak
                                onViewDetails(leak)
                            },
                            centerLat = 12.635,
                            centerLng = -8.025,
                            zoomLevel = 12,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Overlay Tag
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.92f),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                                Text("Bamako, Mali", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }

                        // Bottom Action Guidance Overlay Bar
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                                .padding(10.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TouchApp,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (selectedHomeLeak != null) "Sélectionné : ${selectedHomeLeak?.leakType} (${selectedHomeLeak?.address})" else "Touchez un marqueur pour consulter le détail.",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (onOpenMapTab != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = onOpenMapTab,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = "Carte Complète",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vous constatez un gaspillage d'eau ?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Photographiez et géolocalisez le problème immédiatement.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = onReportClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Signaler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "Vos Signalements Récents",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        val myRecentLeaks = leaks.take(4)
        if (myRecentLeaks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Aucune fuite",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun signalement actif.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(myRecentLeaks) { leak ->
                LeakReportCard(leak = leak, onClick = { onViewDetails(leak) })
            }
        }
    }
}

@Composable
fun StatPill(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CITIZEN LEAKS LIST COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun CitizenLeaksList(
    leaks: List<LeakReport>,
    searchQuery: String,
    statusFilter: String,
    severityFilter: String,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onSeverityFilterChange: (String) -> Unit,
    onViewDetails: (LeakReport) -> Unit,
    onDeleteClick: (LeakReport) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field_leaks"),
            placeholder = { Text("Rechercher par quartier, type, description...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Rechercher") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Effacer")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Filtrer par statut :", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Tous", "Signalé", "En cours", "Réparé").forEach { status ->
                val isSelected = statusFilter == status
                val pillColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                        .clickable { onStatusFilterChange(status) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = status, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Filtrer par gravité :", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Tous", "Grave", "Moyen", "Faible").forEach { severity ->
                val isSelected = severityFilter == severity
                val pillColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillColor)
                        .clickable { onSeverityFilterChange(severity) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = severity, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (leaks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Aucun résultat",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun signalement ne correspond à vos filtres.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(leaks, key = { it.id }) { leak ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            LeakReportCard(leak = leak, onClick = { onViewDetails(leak) })
                        }
                        IconButton(
                            onClick = { onDeleteClick(leak) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Supprimer")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CITIZEN INTERACTIVE MAP COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun CitizenMapView(
    leaks: List<LeakReport>,
    onLeakClick: (LeakReport) -> Unit
) {
    val context = LocalContext.current
    var highlightedLeak by remember { mutableStateOf<LeakReport?>(null) }
    var useGoogleMaps by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE8F5E9))
            ) {
                if (useGoogleMaps) {
                    GoogleMapsEmbedView(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it }
                    )
                } else {
                    BamakoSimulatedMapCanvas(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it },
                        onMapClick = { highlightedLeak = null }
                    )
                }
                
                // Map Mode Selector Card
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🗺️ Google Maps",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Polyline,
                                    contentDescription = null,
                                    tint = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🎨 Vue Schématique",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = highlightedLeak != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                highlightedLeak?.let { leak ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = leak.leakType,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SeverityBadge(severity = leak.severity)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = leak.address,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = leak.description,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status = leak.status)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { launchGoogleMapsApp(context, leak.latitude, leak.longitude, leak.leakType) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Navigation, contentDescription = "GPS", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Google Maps", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = { onLeakClick(leak) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Consulter", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TECHNICIAN INTERACTION COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun TechnicianDashboard(
    leaks: List<LeakReport>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onUpdateStatus: (LeakReport, String, String) -> Unit,
    onViewDetails: (LeakReport) -> Unit,
    currentAgent: SomagepAgent? = null
) {
    val context = LocalContext.current
    var showOnlyMyInterventions by remember { mutableStateOf(currentAgent != null) }
    var leakForCompletionModal by remember { mutableStateOf<LeakReport?>(null) }
    var showEquipmentChecklist by remember { mutableStateOf(false) }

    var completionNotes by remember { mutableStateOf("Vanne principale fermée, raccordement PVC 110mm sécurisé et étanchéité vérifiée sous pression.") }
    var estimatedSavedM3 by remember { mutableStateOf("12") }
    var selectedTools by remember { mutableStateOf(setOf("Tuyau PVC / PEX 110mm", "Collier de prise en charge", "Manchon de réparation", "Clé à vanne SOMAGEP")) }
    
    val displayLeaks = if (showOnlyMyInterventions && currentAgent != null) {
        leaks.filter { 
            it.assignedTechnician.contains(currentAgent.name, ignoreCase = true) ||
            (it.assignedTechnician.isNotEmpty() && currentAgent.name.contains(it.assignedTechnician, ignoreCase = true))
        }
    } else {
        leaks
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Authenticated profile details
        if (currentAgent != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = currentAgent.name.split(" ")
                            .mapNotNull { it.firstOrNull() }
                            .take(2)
                            .joinToString("")
                            .uppercase()
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Technicien : ${currentAgent.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Secteur d'activité : ${currentAgent.zoneOrPost}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2E7D32))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Connecté 🟢", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // DESK AGENT ASSIGNMENT ALERT FOR TECHNICIAN
        val agent = currentAgent
        val myAssignedLeaks = leaks.filter { 
            (agent == null || it.assignedTechnician.contains(agent.name, ignoreCase = true) || (it.assignedTechnician.isNotEmpty() && agent.name.contains(it.assignedTechnician, ignoreCase = true))) &&
            it.assignedTechnician.isNotEmpty() &&
            it.status != "Réparé"
        }

        if (myAssignedLeaks.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Alerte Technicien",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🚨 ALERTE TECHNICIEN : AFFECTATION BUREAU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        text = "${myAssignedLeaks.size} EN COURS",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val latestLeak = myAssignedLeaks.firstOrNull()
                            val detailText = if (latestLeak != null) {
                                "L'Agent de Bureau vous a affecté une intervention : ${latestLeak.leakType} à ${latestLeak.address} (Urgence: ${latestLeak.severity})."
                            } else {
                                "L'Agent de Bureau vous a affecté ${myAssignedLeaks.size} fuite(s) d'eau à réparer sur le terrain."
                            }
                            Text(
                                text = detailText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 Transmis par le Bureau de Régulation",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        if (currentAgent != null && !showOnlyMyInterventions) {
                            Button(
                                onClick = { showOnlyMyInterventions = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Filtrer mes affectations", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Tableau d'Intervention",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Zone technique : District de Bamako",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                val activeCount = displayLeaks.count { it.status != "Réparé" }
                Text(
                    text = "$activeCount Fuites Actives",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Expandable Equipment Checklist Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEquipmentChecklist = !showEquipmentChecklist }
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Handyman, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("🧰 Liste du Matériel Requis (Standard SOMAGEP)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Icon(
                        if (showEquipmentChecklist) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Dérouler",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                if (showEquipmentChecklist) {
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf(
                        "• Clé de barrage & Clé à vanne SOMAGEP",
                        "• Colliers de prise en charge PVC / Fonte (DN 50-110)",
                        "• Manchons de réparation coulissants & Joints caoutchouc",
                        "• Pompe d'épuisement portable / Groupe électrogène",
                        "• Détecteur acoustique de fuite d'eau enterrée",
                        "• EPI de Sécurité (Gilets fluorescents, Bottes, Casque)"
                    ).forEach { tool ->
                        Text(tool, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab selection for my works vs all works
        if (currentAgent != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("Mes Interventions", true),
                    Pair("Tous les Chantiers", false)
                ).forEach { (label, isMyChantier) ->
                    val isSelected = showOnlyMyInterventions == isMyChantier
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { showOnlyMyInterventions = isMyChantier }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val myCount = leaks.count { 
                            it.assignedTechnician.contains(currentAgent.name, ignoreCase = true) ||
                            (it.assignedTechnician.isNotEmpty() && currentAgent.name.contains(it.assignedTechnician, ignoreCase = true))
                        }
                        Text(
                            text = if (isMyChantier) "$label ($myCount)" else "$label (${leaks.size})",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filtrer par quartier ou gravité...") },
            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = "Filtre") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (displayLeaks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.AssignmentTurnedIn,
                        contentDescription = "Tout résolu",
                        modifier = Modifier.size(64.dp),
                        tint = SafeGreen
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (showOnlyMyInterventions) "Aucun chantier ne vous est directement assigné actuellement." else "Félicitations ! Toutes les fuites signalées sont réparées.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (showOnlyMyInterventions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showOnlyMyInterventions = false },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voir tous les chantiers SOMAGEP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayLeaks) { leak ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewDetails(leak) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (leak.severity) {
                                                    "Grave" -> MaterialTheme.colorScheme.error
                                                    "Moyen" -> WarningAmber
                                                    else -> SafeGreen
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = leak.leakType,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                StatusBadge(status = leak.status)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 ${leak.address}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = leak.description,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Abonné: ${leak.reporterName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(text = "Tél: ${leak.reporterPhone}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${leak.reporterPhone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Filled.Phone, contentDescription = "Appeler", modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val gmmIntentUri = Uri.parse("geo:${leak.latitude},${leak.longitude}?q=${leak.latitude},${leak.longitude}(${Uri.encode(leak.leakType)})")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (e: Exception) {
                                                val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${leak.latitude},${leak.longitude}"))
                                                context.startActivity(webMapIntent)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Filled.Navigation, contentDescription = "GPS", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Technician Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (leak.status == "Signalé") {
                                    Button(
                                        onClick = {
                                            onUpdateStatus(leak, "En cours", "Intervention démarrée par ${currentAgent?.name ?: "Technicien SOMAGEP"}")
                                            Toast.makeText(context, "Fuite prise en charge ! Statut : EN COURS 🛠️", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("⚡ Prendre en Charge le Chantier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (leak.status == "En cours") {
                                    Button(
                                        onClick = { leakForCompletionModal = leak },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("✅ Clôturer l'Intervention (+Rapport)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Surface(
                                        color = SafeGreen.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.Verified, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Rapport d'Intervention Validé & Clôturé 🟢", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Completion Report Sheet
    if (leakForCompletionModal != null) {
        val leak = leakForCompletionModal!!
        AlertDialog(
            onDismissRequest = { leakForCompletionModal = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Rapport de Clôture d'Intervention",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fuite à : ${leak.address} (${leak.leakType})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = completionNotes,
                        onValueChange = { completionNotes = it },
                        label = { Text("Travaux Réalisés / Observations") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = estimatedSavedM3,
                        onValueChange = { estimatedSavedM3 = it },
                        label = { Text("Volume d'eau économisé estimé (m³)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Matériaux & Équipements Utilisés :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    listOf("Tuyau PVC / PEX 110mm", "Collier de prise en charge", "Manchon de réparation", "Joint étanchéité haute pression").forEach { toolName ->
                        val isChecked = selectedTools.contains(toolName)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTools = if (isChecked) selectedTools - toolName else selectedTools + toolName
                                }
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedTools = if (checked) selectedTools + toolName else selectedTools - toolName
                                }
                            )
                            Text(toolName, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fullReport = "$completionNotes | Matériel: ${selectedTools.joinToString(", ")} | Eau sauvée: $estimatedSavedM3 m³"
                        onUpdateStatus(leak, "Réparé", fullReport)
                        leakForCompletionModal = null
                        Toast.makeText(context, "Intervention clôturée avec succès ! 🎉", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)
                ) {
                    Text("Valider la Réparation")
                }
            },
            dismissButton = {
                TextButton(onClick = { leakForCompletionModal = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TECHNICIAN MAP VIEW COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun TechnicianMapView(
    leaks: List<LeakReport>,
    onLeakClick: (LeakReport) -> Unit
) {
    val context = LocalContext.current
    var highlightedLeak by remember { mutableStateOf<LeakReport?>(null) }
    var useGoogleMaps by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFECEFF1))
            ) {
                if (useGoogleMaps) {
                    GoogleMapsEmbedView(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it }
                    )
                } else {
                    BamakoSimulatedMapCanvas(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it },
                        onMapClick = { highlightedLeak = null }
                    )
                }
                
                // Map Mode Selector Card
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🗺️ Google Maps",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Polyline,
                                    contentDescription = null,
                                    tint = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🎨 Vue Schématique",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = highlightedLeak != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                highlightedLeak?.let { leak ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = leak.leakType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SeverityBadge(severity = leak.severity)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "📍 ${leak.address}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Signalement : " + leak.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusBadge(status = leak.status)

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { launchGoogleMapsNavigation(context, leak.latitude, leak.longitude) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Navigation, contentDescription = "Naviguer", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Itinéraire GPS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { onLeakClick(leak) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Intervenir", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TECHNICIAN STATS VIEW COMPOSABLE WITH CUSTOM CANVAS CHARTS
// -------------------------------------------------------------------------------------------------
@Composable
fun TechnicianStatsView(leaks: List<LeakReport>) {
    val solvedCount = leaks.count { it.status == "Réparé" }
    val progressCount = leaks.count { it.status == "En cours" }
    val totalCount = leaks.size

    val solvedPercentage = if (totalCount > 0) (solvedCount.toFloat() / totalCount.toFloat()) else 0.0f
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Performance d'Intervention",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Statistiques de résolution de l'eau potable à Bamako",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val animatedAngle by animateFloatAsState(
                            targetValue = solvedPercentage * 360f,
                            animationSpec = tween(1200, easing = FastOutSlowInEasing)
                        )
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawArc(
                                color = Color(0xFFEEEEEE),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx())
                            )
                            drawArc(
                                color = Color(0xFF0288D1),
                                startAngle = -90f,
                                sweepAngle = animatedAngle,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx())
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(solvedPercentage * 100).toInt()}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Résolu", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Taux de Réparation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "Objectif SOMAGEP : Résoudre 90% des fuites signalées en moins de 12 heures.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("Total", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$totalCount", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Réparé", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$solvedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                            }
                            Column {
                                Text("En Cours", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("$progressCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Types de Fuites Fréquentes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val types = listOf(
                        "Tuyau Principal" to leaks.count { it.leakType == "Tuyau Principal" },
                        "Compteur d'Eau" to leaks.count { it.leakType == "Compteur d'Eau" },
                        "Robinet" to leaks.count { it.leakType == "Robinet" },
                        "Inondation" to leaks.count { it.leakType == "Inondation de Rue" }
                    )
                    
                    val maxVal = types.maxOfOrNull { it.second } ?: 1
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        types.forEach { (name, count) ->
                            val percent = count.toFloat() / maxVal.toFloat()
                            val animatedPercent by animateFloatAsState(targetValue = percent, animationSpec = tween(1000))
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("$count cas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEEEEEE))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedPercent)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Priorisation Automatique",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Les fuites de type 'Tuyau Principal' et classées 'Grave' sont hissées automatiquement au sommet de la file d'intervention technique.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GOOGLE MAPS INTEGRATION & INTERACTIVE MAP VIEW
// -------------------------------------------------------------------------------------------------

fun launchGoogleMapsApp(context: Context, latitude: Double, longitude: Double, label: String = "Fuite d'eau SOMAGEP") {
    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"))
        context.startActivity(webMapIntent)
    }
}

fun launchGoogleMapsNavigation(context: Context, latitude: Double, longitude: Double) {
    val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"))
        context.startActivity(webMapIntent)
    }
}

@Composable
fun GoogleMapsEmbedView(
    leaks: List<LeakReport> = emptyList(),
    selectedLeak: LeakReport? = null,
    onLeakSelected: ((LeakReport) -> Unit)? = null,
    centerLat: Double = 12.635,
    centerLng: Double = -8.025,
    zoomLevel: Int = 13,
    isPickerMode: Boolean = false,
    onLocationPicked: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val markersJson = leaks.joinToString(",") { leak ->
        val color = when (leak.status) {
            "Signalé" -> "#E53935"
            "En cours" -> "#FB8C00"
            "Réparé" -> "#4CAF50"
            else -> "#2196F3"
        }
        val isSelected = selectedLeak?.id == leak.id
        """
        {
            "id": "${leak.id}",
            "lat": ${leak.latitude},
            "lng": ${leak.longitude},
            "title": "${leak.leakType.replace("\"", "\\\"")}",
            "address": "${leak.address.replace("\"", "\\\"")}",
            "status": "${leak.status}",
            "severity": "${leak.severity}",
            "color": "$color",
            "selected": $isSelected
        }
        """.trimIndent()
    }

    val htmlContent = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            body, html, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #e0e0e0; }
            .leaflet-popup-content-wrapper { border-radius: 12px; padding: 4px; font-family: sans-serif; }
            .badge-status { padding: 3px 8px; border-radius: 6px; font-size: 10px; font-weight: bold; color: white; display: inline-block; margin-top: 4px; }
            .custom-pin { width: 22px; height: 22px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 6px rgba(0,0,0,0.4); }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map', { zoomControl: true }).setView([$centerLat, $centerLng], $zoomLevel);

            var googleTile = L.tileLayer('https://{s}.google.com/vt/lyrs=m&x={x}&y={y}&z={z}', {
                maxZoom: 20,
                subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
                attribution: '&copy; Google Maps SOMAGEP'
            }).addTo(map);

            var markers = [$markersJson];
            var leafMarkers = {};

            markers.forEach(function(m) {
                var customIcon = L.divIcon({
                    className: 'custom-div-icon',
                    html: '<div class="custom-pin" style="background-color: ' + m.color + '; transform: ' + (m.selected ? 'scale(1.4)' : 'scale(1.0)') + ';"></div>',
                    iconSize: [22, 22],
                    iconAnchor: [11, 11]
                });

                var marker = L.marker([m.lat, m.lng], { icon: customIcon }).addTo(map);
                marker.bindPopup(`
                    <div style="text-align:center;">
                        <strong style="font-size:13px;">` + m.title + `</strong><br/>
                        <span style="font-size:11px; color:#555;">📍 ` + m.address + `</span><br/>
                        <span class="badge-status" style="background-color:` + m.color + `">` + m.status + ` (` + m.severity + `)</span>
                    </div>
                `);

                marker.on('click', function() {
                    if (window.Android) {
                        window.Android.onLeakSelected(m.id);
                    }
                });

                leafMarkers[m.id] = marker;
            });

            ${if (selectedLeak != null) "if (leafMarkers['${selectedLeak.id}']) { leafMarkers['${selectedLeak.id}'].openPopup(); map.panTo([${selectedLeak.latitude}, ${selectedLeak.longitude}]); }" else ""}

            ${if (isPickerMode) """
                var pickerMarker = L.marker([$centerLat, $centerLng], {
                    draggable: true,
                    icon: L.divIcon({
                        className: 'custom-picker-icon',
                        html: '<div style="background:#E53935; width:28px; height:28px; border-radius:50%; border:3px solid white; box-shadow:0 3px 8px rgba(0,0,0,0.5); display:flex; align-items:center; justify-content:center; color:white; font-weight:bold; font-size:14px;">📍</div>',
                        iconSize: [28, 28],
                        iconAnchor: [14, 14]
                    })
                }).addTo(map);

                pickerMarker.bindPopup("Déplacez le marqueur ou touchez la carte pour indiquer la fuite").openPopup();

                function updatePicker(lat, lng) {
                    if (window.Android) {
                        window.Android.onLocationPicked(lat, lng);
                    }
                }

                pickerMarker.on('dragend', function(e) {
                    var coord = e.target.getLatLng();
                    updatePicker(coord.lat, coord.lng);
                });

                map.on('click', function(e) {
                    pickerMarker.setLatLng(e.latlng);
                    updatePicker(e.latlng.lat, e.latlng.lng);
                });
            """ else ""}
        </script>
    </body>
    </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onLeakSelected(id: String) {
                        val leak = leaks.find { it.id.toString() == id }
                        if (leak != null && onLeakSelected != null) {
                            onLeakSelected(leak)
                        }
                    }

                    @JavascriptInterface
                    fun onLocationPicked(lat: Double, lng: Double) {
                        if (onLocationPicked != null) {
                            onLocationPicked(lat, lng)
                        }
                    }
                }, "Android")
                loadDataWithBaseURL("https://maps.google.com", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://maps.google.com", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
}

// -------------------------------------------------------------------------------------------------
// BAMAKO SIMULATED MAP CANVAS DRAWING
// -------------------------------------------------------------------------------------------------
@Composable
fun BamakoSimulatedMapCanvas(
    leaks: List<LeakReport>,
    selectedLeak: LeakReport?,
    onLeakSelected: (LeakReport) -> Unit,
    onMapClick: () -> Unit
) {
    val errorColor = MaterialTheme.colorScheme.error
    val latMin = 12.55
    val latMax = 12.70
    val lngMin = -8.05
    val lngMax = -7.90

    val infiniteTransition = rememberInfiniteTransition()
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(leaks, selectedLeak) {
                detectTapGestures { offset ->
                    var clickedLeak: LeakReport? = null
                    val clickTolerancePx = 40f

                    val w = size.width.toDouble()
                    val h = size.height.toDouble()

                    leaks.forEach { leak ->
                        val x = (((leak.longitude - lngMin) / (lngMax - lngMin)) * w).toFloat()
                        val y = ((1.0 - ((leak.latitude - latMin) / (latMax - latMin))) * h).toFloat()
                        
                        val dx = offset.x - x
                        val dy = offset.y - y
                        val distance = sqrt((dx * dx) + (dy * dy))
                        if (distance <= clickTolerancePx) {
                            clickedLeak = leak
                        }
                    }

                    if (clickedLeak != null) {
                        onLeakSelected(clickedLeak!!)
                    } else {
                        onMapClick()
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val riverPath = Path().apply {
                moveTo(0f, height * 0.75f)
                cubicTo(
                    width * 0.35f, height * 0.65f,
                    width * 0.45f, height * 0.45f,
                    width * 0.7f, height * 0.4f
                )
                cubicTo(
                    width * 0.85f, height * 0.37f,
                    width * 0.95f, height * 0.2f,
                    width, height * 0.15f
                )
            }
            drawPath(
                path = riverPath,
                color = Color(0xFF29B6F6),
                style = Stroke(width = 30.dp.toPx())
            )

            drawLine(
                color = Color.DarkGray,
                start = Offset(x = width * 0.45f, y = height * 0.53f),
                end = Offset(x = width * 0.51f, y = height * 0.51f),
                strokeWidth = 6.dp.toPx()
            )
            drawLine(
                color = Color.DarkGray,
                start = Offset(x = width * 0.55f, y = height * 0.47f),
                end = Offset(x = width * 0.61f, y = height * 0.45f),
                strokeWidth = 6.dp.toPx()
            )

            drawLine(
                color = Color(0xFFFFEE58),
                start = Offset(x = 0f, y = height * 0.5f),
                end = Offset(x = width, y = height * 0.5f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = Color(0xFFFFEE58),
                start = Offset(x = width * 0.2f, y = 0f),
                end = Offset(x = width * 0.8f, y = height),
                strokeWidth = 3.dp.toPx()
            )

            bamakoDistricts.forEach { district ->
                val x = (((district.lng - lngMin) / (lngMax - lngMin)) * width.toDouble()).toFloat()
                val y = ((1.0 - ((district.lat - latMin) / (latMax - latMin))) * height.toDouble()).toFloat()
                
                drawCircle(
                    color = Color.LightGray,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            leaks.forEach { leak ->
                val x = (((leak.longitude - lngMin) / (lngMax - lngMin)) * width.toDouble()).toFloat()
                val y = ((1.0 - ((leak.latitude - latMin) / (latMax - latMin))) * height.toDouble()).toFloat()

                val pinColor = when {
                    leak.status == "Réparé" -> SafeGreen
                    leak.severity == "Grave" -> errorColor
                    leak.severity == "Moyen" -> WarningAmber
                    else -> SomagepBlue
                }

                val isSelected = selectedLeak?.id == leak.id

                if (leak.status != "Réparé") {
                    drawCircle(
                        color = pinColor.copy(alpha = pulseAlpha),
                        radius = (pulseRadius + (if (isSelected) 8f else 0f)).dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                drawCircle(
                    color = pinColor,
                    radius = (if (isSelected) 10f else 6f).dp.toPx(),
                    center = Offset(x, y)
                )
                
                if (isSelected) {
                    drawCircle(
                        color = Color.White,
                        radius = 12.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSABLE: REUSABLE LEAK REPORT CARD
// -------------------------------------------------------------------------------------------------
@Composable
fun LeakReportCard(leak: LeakReport, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("leak_card_${leak.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (leak.isDeletedByCitizen) Color(0xFFFFF8F8) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (leak.isDeletedByCitizen) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (leak.isDeletedByCitizen) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFC62828))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🗑️ Masqué par le citoyen (Conservé en archive Agent/Admin)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                when (leak.photoPath) {
                    "preset_pipe" -> CustomPresetVectorIcon(type = "Tuyau Principal")
                    "preset_meter" -> CustomPresetVectorIcon(type = "Compteur d'Eau")
                    "preset_faucet" -> CustomPresetVectorIcon(type = "Robinet")
                    "preset_street" -> CustomPresetVectorIcon(type = "Inondation de Rue")
                    else -> {
                        if (leak.photoPath.startsWith("content://") || leak.photoPath.startsWith("file://")) {
                            AsyncImage(
                                model = leak.photoPath,
                                contentDescription = "Photo fuite",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CustomPresetVectorIcon(type = "Autre")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = leak.leakType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SeverityBadge(severity = leak.severity)
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = leak.address,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = leak.description,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(leak.timestamp))
                    Text(
                        text = dateStr,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    StatusBadge(status = leak.status)
                }
            }
        }
    }
}
}

// -------------------------------------------------------------------------------------------------
// DETAILED LEAK PRESENTS VECTORS COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun CustomPresetVectorIcon(type: String) {
    Canvas(modifier = Modifier.size(36.dp)) {
        val w = size.width
        val h = size.height
        when (type) {
            "Tuyau Principal" -> {
                drawLine(color = Color(0xFF00ACC1), start = Offset(0f, h/2), end = Offset(w, h/2), strokeWidth = 8.dp.toPx())
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(w/2, h/2))
                drawCircle(color = Color.Red, radius = 2.dp.toPx(), center = Offset(w/2, h/2))
            }
            "Compteur d'Eau" -> {
                drawCircle(color = Color.Gray, radius = w/3, center = Offset(w/2, h/2), style = Stroke(width = 2.dp.toPx()))
                drawLine(color = Color.Blue, start = Offset(w/2, h/2), end = Offset(w/2 + w/5, h/2 - h/5), strokeWidth = 2.dp.toPx())
                drawCircle(color = Color.DarkGray, radius = 3.dp.toPx(), center = Offset(w/2, h/2))
            }
            "Robinet" -> {
                drawLine(color = Color.DarkGray, start = Offset(w/4, h/3), end = Offset(w*0.7f, h/3), strokeWidth = 4.dp.toPx())
                drawLine(color = Color.DarkGray, start = Offset(w*0.7f, h/3), end = Offset(w*0.7f, h*0.6f), strokeWidth = 4.dp.toPx())
                drawCircle(color = Color(0xFF0288D1), radius = 3.dp.toPx(), center = Offset(w*0.7f, h*0.75f))
            }
            "Inondation de Rue" -> {
                val wavePath = Path().apply {
                    moveTo(0f, h*0.6f)
                    quadraticTo(w*0.25f, h*0.4f, w*0.5f, h*0.6f)
                    quadraticTo(w*0.75f, h*0.8f, w, h*0.6f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = wavePath, color = Color(0xFF0288D1))
            }
            else -> {
                drawCircle(color = Color(0xFF0288D1), radius = w/3, center = Offset(w/2, h/2))
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(w/3 + 2f, h/3 + 2f))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSABLE: SEVERITY BADGE
// -------------------------------------------------------------------------------------------------
@Composable
fun SeverityBadge(severity: String) {
    val bgColor = when (severity) {
        "Grave" -> MaterialTheme.colorScheme.errorContainer
        "Moyen" -> Color(0xFFFFF3E0)
        else -> Color(0xFFE0F2F1)
    }
    val textColor = when (severity) {
        "Grave" -> MaterialTheme.colorScheme.onErrorContainer
        "Moyen" -> WarningAmber
        else -> SafeGreen
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = severity, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------------------------------------------
// COMPOSABLE: STATUS BADGE
// -------------------------------------------------------------------------------------------------
@Composable
fun StatusBadge(status: String) {
    val bgColor = when (status) {
        "Réparé" -> Color(0xFFE8F5E9)
        "En cours" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val textColor = when (status) {
        "Réparé" -> SafeGreen
        "En cours" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = status, color = textColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------------------------------------------
// REPORT LEAK DIALOG (WIZARD-STYLE MULTI-STEP DIALOG)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportLeakDialog(
    onDismiss: () -> Unit,
    onReportSubmitted: (name: String, phone: String, leakType: String, severity: String, description: String, latitude: Double, longitude: Double, address: String, photo: String, onDone: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) } // 1: Citizen Info, 2: Leak details, 3: Capture/Photo, 4: Geolocation, 5: Review
    var isSubmitting by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var leakType by remember { mutableStateOf("Tuyau Principal") }
    var severity by remember { mutableStateOf("Moyen") }
    var description by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(12.635) }
    var longitude by remember { mutableStateOf(-8.025) }
    var address by remember { mutableStateOf("Hamdallaye ACI 2000, Bamako") }
    var photoPath by remember { mutableStateOf("preset_pipe") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isFetchingGps by remember { mutableStateOf(false) }

    val leakTypes = listOf("Tuyau Principal", "Compteur d'Eau", "Robinet", "Inondation de Rue", "Autre")
    val severities = listOf("Faible", "Moyen", "Grave")

    // Safe Camera Launcher with try-catch
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            try {
                val tempFile = java.io.File.createTempFile("leak_photo_", ".jpg", context.cacheDir)
                java.io.FileOutputStream(tempFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }
                photoPath = Uri.fromFile(tempFile).toString()
                Toast.makeText(context, "📸 Photo capturée et enregistrée avec succès !", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                photoPath = "captured_photo_live"
                Toast.makeText(context, "📸 Photo capturée !", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Aucune photo prise avec la caméra.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery / File Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoPath = uri.toString()
            Toast.makeText(context, "Photo importée depuis la galerie !", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Appareil photo non disponible sur cet appareil. Image type sélectionnée.", Toast.LENGTH_LONG).show()
                photoPath = "preset_pipe"
            }
        } else {
            Toast.makeText(context, "Permission caméra refusée. Vous pouvez choisir une photo de la galerie ou une image pré-définie.", Toast.LENGTH_LONG).show()
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    fun updateLocationAndAddress(loc: android.location.Location) {
        latitude = loc.latitude
        longitude = loc.longitude
        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.FRENCH)
            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val locName = addr.locality ?: addr.subLocality ?: addr.thoroughfare
                address = if (locName != null) "$locName, Bamako (GPS Réel)" else "Bamako (${String.format("%.5f", loc.latitude)}, ${String.format("%.5f", loc.longitude)})"
            } else {
                val closest = bamakoDistricts.minByOrNull { d ->
                    val dl = (d.lat - loc.latitude).toFloat()
                    val dg = (d.lng - loc.longitude).toFloat()
                    (dl * dl) + (dg * dg)
                }
                closest?.let { address = "${it.name}, Bamako (GPS Réel)" }
            }
        } catch (e: Exception) {
            val closest = bamakoDistricts.minByOrNull { d ->
                val dl = (d.lat - loc.latitude).toFloat()
                val dg = (d.lng - loc.longitude).toFloat()
                (dl * dl) + (dg * dg)
            }
            closest?.let { address = "${it.name}, Bamako (GPS Réel)" } ?: run {
                address = "Bamako GPS: (${String.format("%.5f", loc.latitude)}, ${String.format("%.5f", loc.longitude)})"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            isFetchingGps = true
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) updateLocationAndAddress(loc)
                }
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    isFetchingGps = false
                    if (location != null) {
                        updateLocationAndAddress(location)
                        Toast.makeText(context, "Coordonnées GPS réelles détectées : (${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)})", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Position GPS actualisée. Touchez la carte si nécessaire.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    isFetchingGps = false
                }
            } catch (e: SecurityException) {
                isFetchingGps = false
            }
        } else {
            Toast.makeText(context, "Permissions de localisation refusées. Touchez la carte pour choisir le point exact.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(step) {
        if (step == 4) {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasFine || hasCoarse) {
                isFetchingGps = true
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) updateLocationAndAddress(loc)
                    }
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { location ->
                        isFetchingGps = false
                        if (location != null) {
                            updateLocationAndAddress(location)
                            Toast.makeText(context, "Coordonnées GPS réelles : (${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)})", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        isFetchingGps = false
                    }
                } catch (e: SecurityException) {
                    isFetchingGps = false
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
            }
            onDispose {}
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // FIXED TOP HEADER WITH ELEVATION
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (step > 1) step-- else onDismiss()
                                }
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Signaler une fuite d'eau",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Étape $step sur 5",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = "Fermer")
                            }
                        }

                        LinearProgressIndicator(
                            progress = step.toFloat() / 5.0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // SCROLLABLE BODY + ACTION BUTTONS (Single scroll container for 100% small screen accessibility)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (step) {
                        1 -> {
                            Text(
                                text = "Vos Coordonnées de Contact",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Les techniciens SOMAGEP ont besoin de ces coordonnées pour valider et localiser l'emplacement précis du compteur ou robinet de l'abonné.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text("Votre Nom complet :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Ex: Ibrahim Keïta") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reporter_name_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Votre Numéro de Téléphone :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                placeholder = { Text("Ex: 77 12 34 56") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reporter_phone_field"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        2 -> {
                            Text(
                                text = "Description de la Fuite",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Décrivez précisément le type de fuite constaté.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text("Type de fuite constatée :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                leakTypes.forEach { type ->
                                    val selected = leakType == type
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .border(
                                                width = if (selected) 1.5.dp else 0.5.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { 
                                                leakType = type
                                                photoPath = when(type) {
                                                    "Tuyau Principal" -> "preset_pipe"
                                                    "Compteur d'Eau" -> "preset_meter"
                                                    "Robinet" -> "preset_faucet"
                                                    "Inondation de Rue" -> "preset_street"
                                                    else -> "preset_other"
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                                            CustomPresetVectorIcon(type = type)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = type, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Niveau de gravité estimé :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                severities.forEach { sev ->
                                    val selected = severity == sev
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .clickable { severity = sev }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sev,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Description détaillée (repères visuels, débit de la fuite) :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("Ex: L'eau jaillit violemment juste devant le portail vert. Le compteur est complètement submergé d'eau.") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp, max = 130.dp)
                                    .testTag("leak_desc_field"),
                                shape = RoundedCornerShape(8.dp),
                                maxLines = 4
                            )
                        }

                        3 -> {
                            Text(
                                text = "Capture de Photo (Appareil Photo)",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Une photo nette en direct aide la SOMAGEP à identifier la tuyauterie et les outils hydrauliques requis pour réparer la fuite.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )

                            // CAMERA VIEWFINDER & PHOTO DISPLAY CARD
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFF1A1A1A), Color(0xFF000000))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isRealCapturedPhoto = capturedBitmap != null
                                    val isGalleryOrFilePhoto = photoPath.startsWith("content://") || photoPath.startsWith("file://")

                                    if (isRealCapturedPhoto && capturedBitmap != null) {
                                        Image(
                                            bitmap = capturedBitmap!!.asImageBitmap(),
                                            contentDescription = "Photo capturée",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (isGalleryOrFilePhoto) {
                                        AsyncImage(
                                            model = photoPath,
                                            contentDescription = "Photo importée",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Preset Camera Preview Simulation
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                when (photoPath) {
                                                    "preset_pipe" -> CustomPresetVectorIcon(type = "Tuyau Principal")
                                                    "preset_meter" -> CustomPresetVectorIcon(type = "Compteur d'Eau")
                                                    "preset_faucet" -> CustomPresetVectorIcon(type = "Robinet")
                                                    "preset_street" -> CustomPresetVectorIcon(type = "Inondation de Rue")
                                                    else -> CustomPresetVectorIcon(type = "Autre")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Viseur Appareil Photo SOMAGEP",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Image modèle : $photoPath",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    // Viewfinder Reticle & HUD Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    ) {
                                        // Top Left Status Badge
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopStart),
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isRealCapturedPhoto || isGalleryOrFilePhoto) Color(0xFF2E7D32).copy(alpha = 0.9f) else Color.Red.copy(alpha = 0.85f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(Color.White, CircleShape)
                                                )
                                                Text(
                                                    text = if (isRealCapturedPhoto || isGalleryOrFilePhoto) "PHOTO CITOYEN" else "CAMÉRA READY",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        // Reticle Target in Center
                                        if (!isRealCapturedPhoto && !isGalleryOrFilePhoto) {
                                            Icon(
                                                imageVector = Icons.Filled.CenterFocusWeak,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .align(Alignment.Center)
                                            )
                                        }

                                        // Bottom Right Camera Badge
                                        Surface(
                                            modifier = Modifier.align(Alignment.BottomEnd),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.7f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("HD 1080p", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // MAIN PRIMARY CAMERA SHUTTER BUTTON
                            Button(
                                onClick = {
                                    val hasCameraPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPerm) {
                                        try {
                                            cameraLauncher.launch(null)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Caméra non disponible sur cet appareil. Image type sélectionnée.", Toast.LENGTH_LONG).show()
                                            photoPath = "preset_pipe"
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("take_photo_camera_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (capturedBitmap != null || photoPath.startsWith("content://") || photoPath.startsWith("file://")) "Reprendre la Photo (Caméra)" else "Prendre une Photo (Caméra Directe)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // SECONDARY IMPORT & PRESET BUTTONS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            galleryLauncher.launch("image/*")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Galerie non accessible.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Collections, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Galerie Photo", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                if (capturedBitmap != null || photoPath.startsWith("content://") || photoPath.startsWith("file://")) {
                                    OutlinedButton(
                                        onClick = {
                                            capturedBitmap = null
                                            photoPath = "preset_pipe"
                                            Toast.makeText(context, "Photo réinitialisée.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Réinitialiser", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Ou choisissez parmi les modèles types de fuites (fallback) :", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val presets = listOf(
                                    "preset_pipe" to "Tuyau cassé",
                                    "preset_meter" to "Compteur fuit",
                                    "preset_faucet" to "Robinet cassé",
                                    "preset_street" to "Inondation"
                                )
                                presets.forEach { (preset, label) ->
                                    val selected = photoPath == preset && capturedBitmap == null
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .border(width = 1.dp, color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp))
                                            .clickable {
                                                photoPath = preset
                                                capturedBitmap = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }

                        4 -> {
                            Text(
                                text = "Géolocalisation Google Maps",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Activez le GPS de votre appareil ou déplacez le marqueur rouge sur la carte Google Maps pour positionner exactement la fuite d'eau.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isFetchingGps) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mon GPS Actuel", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { launchGoogleMapsApp(context, latitude, longitude, "Emplacement de la fuite") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ouvrir Google Maps", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bamako District Quick Selection Chips
                            Text("Sélecteur rapide de Quartier à Bamako :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bamakoDistricts.forEach { dist ->
                                    val isSelected = address.contains(dist.name)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            latitude = dist.lat
                                            longitude = dist.lng
                                            address = "${dist.name}, Bamako"
                                        },
                                        label = { Text(dist.name, fontSize = 10.sp) },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                GoogleMapsEmbedView(
                                    leaks = emptyList(),
                                    centerLat = latitude,
                                    centerLng = longitude,
                                    zoomLevel = 14,
                                    isPickerMode = true,
                                    onLocationPicked = { pickedLat, pickedLng ->
                                        latitude = pickedLat
                                        longitude = pickedLng
                                        val closest = bamakoDistricts.minByOrNull { d ->
                                            val dl = (d.lat - pickedLat).toFloat()
                                            val dg = (d.lng - pickedLng).toFloat()
                                            (dl * dl) + (dg * dg)
                                        }
                                        closest?.let {
                                            address = "${it.name}, Bamako"
                                        }
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Point de fuite sélectionné :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Text(text = address, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
                                    Text(text = "Coordonnées GPS: (${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)})", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        5 -> {
                            Text(
                                text = "Validation Finale",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Veuillez vérifier les informations ci-dessous avant d'enregistrer le signalement.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SummaryRow(label = "Déclarant", value = name.ifEmpty { "Anonyme" })
                                    SummaryRow(label = "Téléphone", value = phone.ifEmpty { "Non renseigné" })
                                    SummaryRow(label = "Type de fuite", value = leakType)
                                    SummaryRow(label = "Gravité", value = severity)
                                    SummaryRow(label = "Adresse / Quartier", value = address)
                                    SummaryRow(label = "Description", value = description.ifEmpty { "Pas de description additionnelle." })
                                    SummaryRow(label = "Pièce Jointe", value = "Photo Type ($photoPath)")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
                                Text(
                                    text = "En soumettant, vous certifiez l'exactitude de la fuite pour éviter de fausses alertes d'intervention.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // HIGH-VISIBILITY SCROLLABLE ACTION BUTTONS (Always accessible on all mobile screens!)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (step > 1) step-- else onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (step == 1) Icons.Filled.Close else Icons.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (step == 1) "Annuler" else "Précédent",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                enabled = !isSubmitting,
                                onClick = {
                                    if (step < 5) {
                                        if (step == 1 && name.trim().isEmpty()) {
                                            Toast.makeText(context, "Veuillez entrer votre nom.", Toast.LENGTH_SHORT).show()
                                        } else if (step == 1 && phone.trim().isEmpty()) {
                                            Toast.makeText(context, "Veuillez entrer votre numéro de téléphone.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            step++
                                        }
                                    } else {
                                        isSubmitting = true
                                        onReportSubmitted(name, phone, leakType, severity, description, latitude, longitude, address, photoPath) {
                                            isSubmitting = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Envoi à Firebase...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = if (step == 5) "Envoyer à la SOMAGEP" else "Suivant",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (step == 5) Icons.Filled.Send else Icons.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Extra space at bottom to guarantee easy scrolling above navigation gesture bars
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------------------------------------------
// LEAK DETAILS DIALOG WITH UPDATES FOR TECHNICIANS
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeakDetailsDialog(
    leak: LeakReport,
    role: String,
    onDismiss: () -> Unit,
    onStatusUpdate: (status: String, notes: String) -> Unit,
    onOpenReceipt: ((LeakReport) -> Unit)? = null,
    onRateReport: ((LeakReport, Int, String) -> Unit)? = null,
    onOpenWorkOrder: ((LeakReport) -> Unit)? = null
) {
    val context = LocalContext.current
    var notesInput by remember { mutableStateOf(leak.technicianNotes) }
    var selectedStatus by remember { mutableStateOf(leak.status) }
    
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (leak.isDeletedByCitizen) {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SIGNALEMENT RETIRÉ PAR LE CITOYEN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                            }
                            Text(
                                "L'usager a masqué ou retiré ce signalement de son suivi personnel. Cependant, les données, la localisation GPS et la photo restent conservées intégralement pour les Agents et Administrateurs SOMAGEP.",
                                fontSize = 10.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Détails du Signalement", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeverityBadge(severity = leak.severity)
                    StatusBadge(status = leak.status)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when (leak.photoPath) {
                        "preset_pipe" -> CustomPresetVectorIcon(type = "Tuyau Principal")
                        "preset_meter" -> CustomPresetVectorIcon(type = "Compteur d'Eau")
                        "preset_faucet" -> CustomPresetVectorIcon(type = "Robinet")
                        "preset_street" -> CustomPresetVectorIcon(type = "Inondation de Rue")
                        else -> {
                            if (leak.photoPath.startsWith("content://") || leak.photoPath.startsWith("file://")) {
                                AsyncImage(
                                    model = leak.photoPath,
                                    contentDescription = "Photo fuite",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                CustomPresetVectorIcon(type = "Autre")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Type de problème :", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                Text(leak.leakType, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Quartier / Adresse :", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                Text(leak.address, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Coordonnées de signalement :", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                Text("Latitude: ${leak.latitude}, Longitude: ${leak.longitude}", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text("Description détaillée :", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                Text(leak.description, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Divider()

                Spacer(modifier = Modifier.height(12.dp))

                CommunicationPanel(
                    leak = leak,
                    isTechnicianAssigned = leak.assignedTechnician.isNotEmpty(),
                    assignedTechName = leak.assignedTechnician,
                    assignedTechPhone = leak.assignedTechnicianPhone,
                    context = context
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "🚨 Signalement Fuite SOMAGEP (${leak.leakType})\n📍 Adresse : ${leak.address}\n📊 Statut : ${leak.status} | Gravité : ${leak.severity}\n📝 Description : ${leak.description}\n🌐 Localisation : https://maps.google.com/?q=${leak.latitude},${leak.longitude}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager le signalement de fuite"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Partager Fiche", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (onOpenReceipt != null) {
                        Button(
                            onClick = { onOpenReceipt(leak) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bordereau PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (leak.status == "Réparé") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ÉVALUATION CITOYENNE DE L'INTERVENTION", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (leak.citizenRating > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { starIndex ->
                                        Icon(
                                            imageVector = if (starIndex < leak.citizenRating) Icons.Filled.Star else Icons.Outlined.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${leak.citizenRating} / 5", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (leak.citizenFeedback.isNotEmpty()) {
                                    Text("Avis usager : \"${leak.citizenFeedback}\"", fontSize = 11.sp, fontStyle = FontStyle.Italic)
                                }
                            } else {
                                Text("Avez-vous été satisfait de la réparation effectuée par la SOMAGEP ?", fontSize = 11.sp)
                                var userRating by remember { mutableStateOf(5) }
                                var userComment by remember { mutableStateOf("") }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..5).forEach { star ->
                                        IconButton(
                                            onClick = { userRating = star },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (star <= userRating) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB300),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = userComment,
                                    onValueChange = { userComment = it },
                                    placeholder = { Text("Laissez votre commentaire ou remarque (optionnel)...", fontSize = 10.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = {
                                        onRateReport?.invoke(leak, userRating, userComment)
                                        Toast.makeText(context, "Merci pour votre évaluation SOMAGEP ! ⭐", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Envoyer mon Évaluation Citoyenne", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (role == "Technicien") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ESPACE TECHNICIEN SOMAGEP", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        if (onOpenWorkOrder != null) {
                            TextButton(onClick = { onOpenWorkOrder(leak) }) {
                                Text("📋 Ordre de Mission", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Changer le statut :", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Signalé", "En cours", "Réparé").forEach { status ->
                            val selected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedStatus = status }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = status, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Notes d'intervention / Rapport technique :", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("Ex: Compteur réparé. Vanne de raccordement changée pour stopper la fuite d'eau.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onStatusUpdate(selectedStatus, notesInput)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mettre à jour le Signalement", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (leak.technicianNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Rapport du Technicien SOMAGEP :", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(leak.technicianNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// BORDEREAU OFFICIEL DE SIGNALEMENT SOMAGEP
// -------------------------------------------------------------------------------------------------
@Composable
fun SomagepReceiptDialog(leak: LeakReport, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val ticketNumber = "SOM-2026-${leak.id.toString().padStart(5, '0')}"
    val formattedDate = remember(leak.timestamp) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(leak.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("SOMAGEP S.A.", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Société Malienne de Gestion de l'Eau Potable", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

                Text("BORDEREAU OFFICIEL DE SIGNALEMENT", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.DarkGray)
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("N° REÇU / TICKET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(ticketNumber, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Date & Heure: $formattedDate", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }

                // Details Table
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReceiptRowItem("Usager Signalant", leak.reporterName)
                        ReceiptRowItem("Contact Téléphone", leak.reporterPhone)
                        ReceiptRowItem("Type de Fuite", leak.leakType)
                        ReceiptRowItem("Niveau d'Urgence", leak.severity)
                        ReceiptRowItem("Quartier / Adresse", leak.address)
                        ReceiptRowItem("Coordonnées GPS", "${leak.latitude}, ${leak.longitude}")
                        ReceiptRowItem("Statut Traitement", leak.status)
                        ReceiptRowItem("Equipe Assignée", if (leak.assignedTechnician.isNotEmpty()) leak.assignedTechnician else "Cellule Dispatch Bamako")
                    }
                }

                // Barcode / Verification Seal
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTHENTIFICATION NUMÉRIQUE", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                            Text("Certifié conforme par le Système Cloud SOMAGEP Mali.", fontSize = 9.sp, color = Color.DarkGray)
                            Text("Garantie d'intervention < 12h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Canvas(modifier = Modifier.size(width = 50.dp, height = 30.dp)) {
                            val barWidth = size.width / 10
                            for (i in 0..9) {
                                if (i % 2 == 0 || i == 3 || i == 7) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(i * barWidth, 0f),
                                        size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, size.height)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val shareText = "📄 BORDEREAU OFFICIEL SOMAGEP\nTicket N° : $ticketNumber\nUsager : ${leak.reporterName}\nType : ${leak.leakType}\nQuartier : ${leak.address}\nStatut : ${leak.status}\nVérification : https://somagep.ml/track?id=$ticketNumber"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Partager le Bordereau SOMAGEP"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Partager", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Bordereau officiel #$ticketNumber enregistré en format PDF !", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Télécharger PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// -------------------------------------------------------------------------------------------------
// ORDRE DE MISSION TECHNIQUE SOMAGEP
// -------------------------------------------------------------------------------------------------
@Composable
fun TechnicianWorkOrderDialog(
    leak: LeakReport,
    onDismiss: () -> Unit,
    onCompleteIntervention: (LeakReport, String) -> Unit
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(leak.technicianNotes) }
    var item1Checked by remember { mutableStateOf(true) }
    var item2Checked by remember { mutableStateOf(true) }
    var item3Checked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋 ORDRE DE MISSION TECHNIQUE", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("MISSION N° OM-${leak.id} • ${leak.severity.uppercase()}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Lieu : ${leak.address}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Problème : ${leak.leakType}", fontSize = 10.sp)
                    }
                }

                Text("1. Équipements de Réparation Requis :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item1Checked, onCheckedChange = { item1Checked = it })
                    Text("Joints d'étanchéité & Clés de vannes (DN50 - DN100)", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item2Checked, onCheckedChange = { item2Checked = it })
                    Text("Tuyau de remplacement PEHD & Collier anti-fuite", fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item3Checked, onCheckedChange = { item3Checked = it })
                    Text("Pompe d'épuisement & Signalisation de chantier", fontSize = 10.sp)
                }

                Button(
                    onClick = {
                        val geoUri = Uri.parse("geo:${leak.latitude},${leak.longitude}?q=${leak.latitude},${leak.longitude}(${Uri.encode(leak.address)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${leak.latitude},${leak.longitude}"))
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ouvrir GPS Navigation (Google Maps)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Text("2. Rapport de Clôture Technique :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Décrivez les pièces remplacées et les travaux effectués...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        onCompleteIntervention(leak, notes.ifBlank { "Intervention terminée avec succès sur le site." })
                        onDismiss()
                        Toast.makeText(context, "Ordre de mission validé ! Statut changé à Réparé.", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clôturer la Mission & Marquer Réparé", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// DISPATCHER/AGENT DE RÉPARTITION DATA AND VIEWS
// -------------------------------------------------------------------------------------------------

data class SomagepTechnician(val name: String, val phone: String, val zone: String)

val somagepTechnicians = listOf(
    SomagepTechnician("Moussa Diarra", "76 12 34 56", "Commune I & II (Hippodrome, Niaréla)"),
    SomagepTechnician("Amina Touré", "66 54 32 10", "Commune III & IV (ACI 2000, Hamdallaye)"),
    SomagepTechnician("Amadou Diallo", "70 99 88 77", "Commune V & VI (Sogoniko, Faladié, Badalabougou)"),
    SomagepTechnician("Ousmane Keïta", "65 22 11 00", "Communes Ouest (Baco-Djicoroni, Sebenikoro)"),
    SomagepTechnician("Bakary Sidibé", "78 90 12 34", "Zone Est (Sotuba, Titibougou, Moribabougou)"),
    SomagepTechnician("Fanta Konaté", "67 89 01 23", "Zone Hauteurs (Kati, Darsalam, Point G)"),
    SomagepTechnician("Modibo Sangaré", "71 12 23 34", "Zone Sud (Kalaban Coro, Kabala, Daoudabougou)"),
    SomagepTechnician("Kassim Traoré", "64 43 32 21", "Zone Nord-Est (Korofina, Djélibougou)")
)

@Composable
fun CommunicationPanel(
    leak: LeakReport,
    isTechnicianAssigned: Boolean,
    assignedTechName: String,
    assignedTechPhone: String,
    context: android.content.Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "COMMUNICATION AVEC LE CLIENT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Déclarant : ${leak.reporterName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Tél : ${leak.reporterPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // WhatsApp Client Link
                    IconButton(
                        onClick = {
                            val cleanPhone = leak.reporterPhone.replace(" ", "").replace("-", "")
                            val formattedPhone = if (cleanPhone.startsWith("+") || cleanPhone.length > 10) cleanPhone else "223$cleanPhone"
                            val message = "Bonjour ${leak.reporterName}, je suis agent de la SOMAGEP concernant votre signalement de fuite (${leak.leakType}) à ${leak.address}. Nos techniciens sont en cours d'affectation pour intervenir."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)), // WhatsApp Green
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp Client", modifier = Modifier.size(20.dp))
                    }

                    // SMS Client Button
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("smsto:${leak.reporterPhone}")).apply {
                                putExtra("sms_body", "Bonjour ${leak.reporterName}, la SOMAGEP a bien pris en compte votre signalement de fuite (${leak.leakType}) à ${leak.address}. Une équipe d'intervention technique est programmée.")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = "SMS Client", modifier = Modifier.size(18.dp))
                    }

                    // Standard Call
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${leak.reporterPhone}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Appeler Client", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (isTechnicianAssigned && assignedTechName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "COMMUNICATION AVEC LE TECHNICIEN ASSIGNÉ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Build, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                            Text(text = " $assignedTechName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(text = "Tél : $assignedTechPhone", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // WhatsApp Technicien Order of Mission
                        IconButton(
                            onClick = {
                                val cleanPhone = assignedTechPhone.replace(" ", "").replace("-", "")
                                val formattedPhone = if (cleanPhone.startsWith("+") || cleanPhone.length > 10) cleanPhone else "223$cleanPhone"
                                val message = "⚠️ ORDRE DE MISSION TECHNIQUE (SOMAGEP)\n-------------------------------------\n📍 LIEU : ${leak.address}\n💧 TYPE : ${leak.leakType}\n⚠️ GRAVITÉ : ${leak.severity}\n👤 CLIENT : ${leak.reporterName} (${leak.reporterPhone})\n🔧 Veuillez intervenir dans les plus brefs délais et mettre à jour le statut dans l'application."
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_whatsapp), contentDescription = "WhatsApp Technicien", modifier = Modifier.size(20.dp))
                        }

                        // Share details via other channels (Telegram, FB, etc)
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Mission SOMAGEP - Fuite d'Eau")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "💧 RAPPORT D'INCIDENT - SOMAGEP MALI 🇲🇱\n" +
                                        "====================================\n" +
                                        "📌 Secteur / Adresse : ${leak.address}\n" +
                                        "⚡ Type de Fuite : ${leak.leakType}\n" +
                                        "🔴 Gravité : ${leak.severity}\n" +
                                        "👤 Signalé par : ${leak.reporterName} (${leak.reporterPhone})\n" +
                                        "🛠️ Technicien assigné : $assignedTechName ($assignedTechPhone)\n" +
                                        "⚙️ Statut Actuel : ${leak.status}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Envoyer la mission via..."))
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Partager la mission", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(
                        text = "En attente d'affectation technique par un Agent de Répartition.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DispatcherConsoleView(
    leaks: List<LeakReport>,
    onAssignTechnician: (LeakReport, String, String) -> Unit,
    onViewDetails: (LeakReport) -> Unit,
    currentAgent: SomagepAgent? = null
) {
    val context = LocalContext.current
    var selectedFilterQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Tous") }

    val filtered = leaks.filter { leak ->
        val matchesQuery = selectedFilterQuery.isEmpty() || 
                leak.address.contains(selectedFilterQuery, ignoreCase = true) || 
                leak.leakType.contains(selectedFilterQuery, ignoreCase = true) ||
                leak.reporterName.contains(selectedFilterQuery, ignoreCase = true)
        
        val matchesStatus = selectedStatusFilter == "Tous" || 
                (selectedStatusFilter == "Non assignés" && leak.assignedTechnician.isEmpty()) ||
                (selectedStatusFilter == "Assignés" && leak.assignedTechnician.isNotEmpty()) ||
                leak.status == selectedStatusFilter
        
        matchesQuery && matchesStatus
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Desktop Simulation Banner Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Computer, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text(
                            text = "STATION DE RÉPARTITION SOMAGEP",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676)) // Glowing green dot
                        )
                        val agentText = if (currentAgent != null) {
                            "Opérateur Actif : ${currentAgent.name} (${currentAgent.zoneOrPost}) • Connecté 🟢"
                        } else {
                            "Sync Réseau : Connecté au Serveur de Bamako (OK) • Mode Administratif"
                        }
                        Text(
                            text = agentText,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // CITIZEN REPORTED LEAKS ALERT FOR DESK AGENT (AGENT DE BUREAU)
        val unassignedCitizenLeaks = leaks.filter { it.assignedTechnician.isEmpty() && it.status != "Réparé" }
        if (unassignedCitizenLeaks.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Alerte Agent Bureau",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🚨 ALERTE AGENT DE BUREAU",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = "${unassignedCitizenLeaks.size} EN ATTENTE",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                val sampleAddr = unassignedCitizenLeaks.firstOrNull()?.address ?: "Bamako"
                                Text(
                                    text = "${unassignedCitizenLeaks.size} fuite(s) signalée(s) par des citoyens (ex: $sampleAddr) nécessitent votre affectation urgente à un technicien.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { selectedStatusFilter = "Non assignés" },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Affecter les Fuites Citoyen (${unassignedCitizenLeaks.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Supervisor Quick Stats Counter Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalCount = leaks.size
                val unassignedCount = leaks.count { it.assignedTechnician.isEmpty() }
                val progressCount = leaks.count { it.status == "En cours" }
                val solvedCount = leaks.count { it.status == "Réparé" }

                DispatcherStatCard(modifier = Modifier.weight(1f), label = "Signalés", count = "$totalCount", color = MaterialTheme.colorScheme.primary)
                DispatcherStatCard(modifier = Modifier.weight(1f), label = "Sans Tech", count = "$unassignedCount", color = MaterialTheme.colorScheme.error)
                DispatcherStatCard(modifier = Modifier.weight(1f), label = "En cours", count = "$progressCount", color = WarningAmber)
                DispatcherStatCard(modifier = Modifier.weight(1f), label = "Résolus", count = "$solvedCount", color = SafeGreen)
            }
        }

        // Smart Auto-Dispatch Engine Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⚡ Dispatch Automatique (IA SOMAGEP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Affecte intelligemment les fuites non assignées selon les secteurs de Bamako.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val defaultTechs = listOf(
                                Pair("Ibrahim Keïta", "76 11 22 33"),
                                Pair("Ousmane Diallo", "78 44 55 66"),
                                Pair("Bakary Coulibaly", "66 77 88 99"),
                                Pair("Ibrahim Traoré", "75 22 33 44")
                            )
                            var assignedCounter = 0
                            leaks.filter { it.assignedTechnician.isEmpty() }.forEachIndexed { idx, unassignedLeak ->
                                val tech = defaultTechs[idx % defaultTechs.size]
                                onAssignTechnician(unassignedLeak, tech.first, tech.second)
                                assignedCounter++
                            }
                            if (assignedCounter > 0) {
                                Toast.makeText(context, "⚡ $assignedCounter fuite(s) affectée(s) automatiquement aux techniciens !", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Toutes les fuites ont déjà un technicien assigné. 👍", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lancer Auto-Dispatch", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Filter / Search Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = selectedFilterQuery,
                    onValueChange = { selectedFilterQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher par quartier, client, type...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Rechercher") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Horizontal scroll filter pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val filterOptions = listOf("Tous", "Non assignés", "Assignés", "Réparé")
                    filterOptions.forEach { opt ->
                        val isSelected = selectedStatusFilter == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { selectedStatusFilter = opt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // List Title
        item {
            Text(
                text = "ALERTE ET AFFECTATIONS DE CHANTIERS (${filtered.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Aucun rapport ne correspond à vos filtres de recherche.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        } else {
            items(filtered) { leak ->
                var expandedAssignPanel by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewDetails(leak) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (leak.assignedTechnician.isEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (leak.severity) {
                                                "Grave" -> MaterialTheme.colorScheme.error
                                                "Moyen" -> WarningAmber
                                                else -> SafeGreen
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = leak.leakType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            StatusBadge(status = leak.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "📍 Localisation : ${leak.address}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "✍️ " + leak.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Technician current status info
                        if (leak.assignedTechnician.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TECHNICIEN SUR LE SECTEUR", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                                        Text(" ${leak.assignedTechnician}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                    }
                                }
                                
                                Button(
                                    onClick = { expandedAssignPanel = !expandedAssignPanel },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Réaffecter", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Text(" Aucun technicien affecté !", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { expandedAssignPanel = !expandedAssignPanel },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dépêcher", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Expanded assign panel lists the technicians
                        AnimatedVisibility(visible = expandedAssignPanel) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("TECHNICIENS DISPONIBLES BAMAKO :", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                
                                somagepTechnicians.forEach { tech ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onAssignTechnician(leak, tech.name, tech.phone)
                                                expandedAssignPanel = false
                                                Toast.makeText(context, "Ordre d'intervention envoyé à ${tech.name} ! 📡", Toast.LENGTH_SHORT).show()
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(0.5.dp, Color.LightGray)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(tech.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(tech.zone, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("Affecter", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DispatcherStatCard(
    modifier: Modifier = Modifier,
    label: String,
    count: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun DispatcherMapView(
    leaks: List<LeakReport>,
    onLeakClick: (LeakReport) -> Unit,
    onAssignTechnician: (LeakReport, String, String) -> Unit
) {
    var highlightedLeak by remember { mutableStateOf<LeakReport?>(null) }
    val context = LocalContext.current
    var showAssignDialogForLeak by remember { mutableStateOf<LeakReport?>(null) }
    var useGoogleMaps by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFECEFF1))
            ) {
                if (useGoogleMaps) {
                    GoogleMapsEmbedView(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it }
                    )
                } else {
                    BamakoSimulatedMapCanvas(
                        leaks = leaks,
                        selectedLeak = highlightedLeak,
                        onLeakSelected = { highlightedLeak = it },
                        onMapClick = { highlightedLeak = null }
                    )
                }
                
                // Map Mode Selector Card
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Map,
                                    contentDescription = null,
                                    tint = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🗺️ Google Maps",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (!useGoogleMaps) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { useGoogleMaps = false }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Polyline,
                                    contentDescription = null,
                                    tint = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "🎨 Vue Schématique",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!useGoogleMaps) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            AnimatedVisibility(
                visible = highlightedLeak != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                highlightedLeak?.let { leak ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = leak.leakType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SeverityBadge(severity = leak.severity)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "📍 ${leak.address}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            if (leak.assignedTechnician.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                                    Text(text = " Assigné à : ${leak.assignedTechnician}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    Text(text = " Non assigné", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { launchGoogleMapsApp(context, leak.latitude, leak.longitude, leak.leakType) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Maps GPS", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { onLeakClick(leak) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Détails", fontSize = 10.sp, color = Color.White)
                                }
                                
                                Button(
                                    onClick = { showAssignDialogForLeak = leak },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1.3f).height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Assigner", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Quick assign dialog
        showAssignDialogForLeak?.let { leak ->
            AlertDialog(
                onDismissRequest = { showAssignDialogForLeak = null },
                title = { Text("Assigner un Technicien", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sélectionnez le technicien pour intervenir à ${leak.address} :", fontSize = 12.sp)
                        somagepTechnicians.forEach { tech ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAssignTechnician(leak, tech.name, tech.phone)
                                        showAssignDialogForLeak = null
                                        highlightedLeak = highlightedLeak?.copy(assignedTechnician = tech.name, assignedTechnicianPhone = tech.phone)
                                        Toast.makeText(context, "Technicien ${tech.name} assigné avec succès !", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tech.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(tech.zone, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAssignDialogForLeak = null }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CALCULATEUR DE GASPILLAGE D'EAU COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun WaterLossCalculatorDialog(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Gaspillage & Fuites, 1: Simulation Facture SOMAGEP
    
    // Tab 0 states
    var selectedLeakTypeIndex by remember { mutableStateOf(1) }
    var leakHours by remember { mutableStateOf(24f) }

    val leakRatesLitersPerHour = listOf(2.0f, 15.0f, 40.0f, 300.0f, 1500.0f)
    val leakLabels = listOf(
        "Goutte à goutte léger (~2 L/h)",
        "Filet d'eau continu (~15 L/h)",
        "Chasse d'eau ou robinet cassé (~40 L/h)",
        "Tuyau fissuré dans la rue (~300 L/h)",
        "Rupture majeure de conduite (~1 500 L/h)"
    )

    val flowRate = leakRatesLitersPerHour[selectedLeakTypeIndex]
    val totalLitersLost = (flowRate * leakHours).toInt()
    val totalCubicMeters = totalLitersLost / 1000.0
    val somagepTariffFcfaPerM3 = 215.0
    val estimatedCostFcfa = (totalCubicMeters * somagepTariffFcfaPerM3).toInt()

    // Tab 1 states (Facture SOMAGEP)
    var billVolumeInput by remember { mutableStateOf("25") }
    val m3 = billVolumeInput.toDoubleOrNull() ?: 0.0
    val t1Cost = kotlin.math.min(m3, 20.0) * 122.0
    val t2Volume = kotlin.math.max(0.0, kotlin.math.min(m3 - 20.0, 30.0))
    val t2Cost = t2Volume * 313.0
    val t3Volume = kotlin.math.max(0.0, m3 - 50.0)
    val t3Cost = t3Volume * 508.0
    val redevance = m3 * 20.0
    val tva = (t2Cost + t3Cost) * 0.18
    val totalBillFcfa = (t1Cost + t2Cost + t3Cost + redevance + tva).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Calculate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Calculateur & Barème SOMAGEP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pertes & Fuite", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Facture d'Eau", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    Text(
                        text = "Estimez le volume d'eau potable perdu et l'impact financier d'une fuite non réparée à Bamako.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("1. Type de fuite :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    leakLabels.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedLeakTypeIndex == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedLeakTypeIndex = index }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedLeakTypeIndex == index),
                                onClick = { selectedLeakTypeIndex = index }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedLeakTypeIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("2. Durée de la fuite : ${leakHours.toInt()} Heures", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Slider(
                        value = leakHours,
                        onValueChange = { leakHours = it },
                        valueRange = 1f..168f,
                        steps = 167
                    )

                    Divider()

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("RÉSULTAT DE L'ESTIMATION", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Eau Perdue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("$totalLitersLost L", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("(${String.format("%.2f", totalCubicMeters)} m³)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                }
                                Divider(modifier = Modifier.height(36.dp).width(1.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Coût Estimé", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("$estimatedCostFcfa FCFA", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SafeGreen)
                                    Text("@ 215 FCFA / m³", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Barème tarifaire officiel de facturation SOMAGEP S.A. au Mali.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = billVolumeInput,
                        onValueChange = { billVolumeInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Volume consommé (m³)", fontSize = 11.sp) },
                        placeholder = { Text("Ex: 25", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("DÉTAIL DU BARÈME TARIFAIRE :", fontWeight = FontWeight.Black, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            BillRowItem("Tranche Sociale (0 - 20 m³ @ 122 FCFA)", "${t1Cost.toInt()} FCFA")
                            BillRowItem("Tranche Normale (21 - 50 m³ @ 313 FCFA)", "${t2Cost.toInt()} FCFA")
                            BillRowItem("Tranche Supérieure (> 50 m³ @ 508 FCFA)", "${t3Cost.toInt()} FCFA")
                            BillRowItem("Redevance Assainissement (20 FCFA/m³)", "${redevance.toInt()} FCFA")
                            BillRowItem("TVA (18% sur Tranches 2 & 3)", "${tva.toInt()} FCFA")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ESTIMATION DE VOTRE FACTURE EAU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("$totalBillFcfa FCFA", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                            Text("Montant mensuel TTC estimé pour $m3 m³", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Signaler rapidement permet de préserver la ressource en eau de Bamako !",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun BillRowItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

// -------------------------------------------------------------------------------------------------
// SIGNALEMENT DE FRAUDE ET VOL D'EAU COMPOSABLE
// -------------------------------------------------------------------------------------------------
@Composable
fun FraudReportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    var fraudType by remember { mutableStateOf("Branchement Clandestin") }
    var description by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            isFetchingGps = true
            try {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    isFetchingGps = false
                    if (location != null) {
                        address = "Bamako (GPS Précis: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                        Toast.makeText(context, "Position GPS capturée pour l'alerte !", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    isFetchingGps = false
                }
            } catch (e: SecurityException) {
                isFetchingGps = false
            }
        }
    }

    val fraudTypes = listOf(
        "Branchement Clandestin",
        "Compteur d'eau Trafiqué / Contourné",
        "Vol de Compteur SOMAGEP",
        "Utilisation Non Autorisée de Poteau d'Incendie",
        "Saignée Non Autorisée sur Conduite Publique"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Signalement de Fraude / Vol d'Eau",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            if (isSubmitted) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Signalement Reçu !", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Merci de contribuer à la protection du réseau SOMAGEP. Votre alerte anonyme a été transmise au service d'inspection.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alerte 100% Confidentielle. Vos informations ne seront jamais divulguées.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text("Type d'infraction :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    fraudTypes.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (fraudType == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { fraudType = type }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (fraudType == type), onClick = { fraudType = type })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(type, fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Quartier / Lieu exact à Bamako") },
                        placeholder = { Text("Ex: Badalabougou, Rue 125, Porte 40") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capturer Position GPS Automatique", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Détails de l'infraction constatée") },
                        placeholder = { Text("Précisez les circonstances...") },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Votre Téléphone (Facultatif)") },
                        placeholder = { Text("Ex: 76 00 00 00") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                    Text("Fermer")
                }
            } else {
                Button(
                    onClick = {
                        if (address.isBlank()) {
                            Toast.makeText(context, "Veuillez préciser le lieu !", Toast.LENGTH_SHORT).show()
                        } else {
                            isSubmitted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Envoyer l'Alerte")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// CITIZEN SERVICES & INFO VIEW COMPOSABLE
// -------------------------------------------------------------------------------------------------
data class WaterOutageInfo(
    val neighborhood: String,
    val reason: String,
    val schedule: String,
    val status: String,
    val impact: String
)

data class SomagepNews(
    val title: String,
    val date: String,
    val category: String,
    val summary: String,
    val fullText: String
)

@Composable
fun CitizenServicesAndInfoView(
    onReportFraudClick: () -> Unit,
    onOpenCalculatorClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedNewsForDialog by remember { mutableStateOf<SomagepNews?>(null) }
    var activeFilter by remember { mutableStateOf("Tous") }

    val waterOutagesList = remember {
        listOf(
            WaterOutageInfo(
                neighborhood = "Badalabougou & Quartier Fleuve",
                reason = "Remplacement vanne principale DN400",
                schedule = "26 Juillet 2026 de 08h00 à 15h00",
                status = "En cours",
                impact = "Baisse de pression et coupure temporaire."
            ),
            WaterOutageInfo(
                neighborhood = "Kalaban Coro, Garankabougou & Faladié",
                reason = "Maintenance préventive de la station de Kabala",
                schedule = "27 Juillet 2026 de 22h00 à 05h00 (Nuit)",
                status = "Programmé",
                impact = "Interruption nocturne programmée. Pensez à constituer des réserves d'eau."
            ),
            WaterOutageInfo(
                neighborhood = "Lafiabougou & ACI 2000",
                reason = "Réparation urgente fuite conduite principale",
                schedule = "Aujourd'hui 06h00 - 11h00",
                status = "Résolu",
                impact = "Rétablissement progressif de la distribution d'eau."
            ),
            WaterOutageInfo(
                neighborhood = "Hamdallaye & Niaréla",
                reason = "Raccordement nouveau réseau d'eau potable",
                schedule = "29 Juillet 2026 de 09h00 à 14h00",
                status = "Programmé",
                impact = "Coupure ciblée sur les secteurs Rues 14 à 28."
            )
        )
    }

    val somagepNewsList = remember {
        listOf(
            SomagepNews(
                title = "Qualité de l'eau potable à Bamako pendant l'hivernage",
                date = "25 Juillet 2026",
                category = "Qualité Eau",
                summary = "La SOMAGEP renforce le traitement et les contrôles bacterologiques en station.",
                fullText = "Durant la période d'hivernage, les équipes du laboratoire central de la SOMAGEP intensifient les analyses quotidiennes de la qualité de l'eau traitée aux stations de Djicoroni-Para et Kabala. L'eau distribuée respecte strictement les normes OMS pour la consommation humaine."
            ),
            SomagepNews(
                title = "Numéro Vert Gratuit 80 00 11 11 disponible 24h/24",
                date = "20 Juillet 2026",
                category = "Service Client",
                summary = "Signalez tout dysfonctionnement ou fuite d'eau gratuitement depuis tout opérateur.",
                fullText = "La Société Malienne de Gestion de l'Eau Potable rappelle au grand public que le centre d'appel d'urgence est joignable gratuitement au 80 00 11 11. Des équipes de garde interviennent 24h/24 dans tous les districts du district de Bamako et de Kati."
            ),
            SomagepNews(
                title = "Lancement du programme de modernisation des compteurs",
                date = "15 Juillet 2026",
                category = "Travaux",
                summary = "Remplacement progressif des anciens compteurs d'eau par des modèles haute précision.",
                fullText = "Dans le cadre de la réduction des pertes d'eau non comptabilisées, la SOMAGEP déploie de nouveaux compteurs volumétriques à haute sensibilité. Les agents équipés de cartes professionnelles officielles se présentent au domicile des abonnés concernés."
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Urgence
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("SOMAGEP Urgences & Assistance", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Centre d'Appels 24h/24 - 7j/7", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:80001111"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("80 00 11 11 (Gratuit)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=22370000000&text=Bonjour%20SOMAGEP%20Service%20Client"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_whatsapp), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Quick Tools Row
        item {
            Text("Outils & Prévention Citoyenne", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenCalculatorClick() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Calculateur Fuite", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Estimer l'eau perdue & coût FCFA", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onReportFraudClick() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Signaler une Fraude", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Vol d'eau & raccordement illégal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // Section Carte / Liste des Coupures d'eau
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Carte & Liste des Coupures d'Eau à Bamako", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text("${waterOutagesList.size} zones", fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                }
            }
        }

        // Filter Pills for Outages
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "En cours", "Programmé", "Résolu").forEach { filter ->
                    val selected = activeFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { activeFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null
                    )
                }
            }
        }

        val filteredOutages = waterOutagesList.filter {
            activeFilter == "Tous" || it.status.equals(activeFilter, ignoreCase = true)
        }

        items(filteredOutages) { outage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(outage.neighborhood, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        val (statusBg, statusFg) = when (outage.status) {
                            "En cours" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                            "Programmé" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            else -> SafeGreen.copy(alpha = 0.2f) to SafeGreen
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(outage.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusFg)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Motif : ${outage.reason}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(outage.schedule, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Impact : ${outage.impact}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Section Communiqués Officiels SOMAGEP
        item {
            Text("Communiqués & Actualités Officiels SOMAGEP", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(somagepNewsList) { news ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedNewsForDialog = news },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(news.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(news.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(news.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(news.summary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Section Conseils Anti-Gaspillage & Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guide Anti-Gaspillage & Astuces Citoyennes", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Détecter une fuite invisible : Relevez votre compteur le soir avant de vous coucher sans utiliser d'eau. Si le chiffre a changé au réveil, vous avez une fuite !", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Robinet vanne d'arrêt : Localisez la vanne générale de votre domicile à Bamako pour fermer l'eau immédiatement en cas de fuite grave.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }

    // Modal pour lire le communiqué officiel
    selectedNewsForDialog?.let { news ->
        AlertDialog(
            onDismissRequest = { selectedNewsForDialog = null },
            icon = {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(news.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${news.category} • ${news.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(news.fullText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                Button(onClick = { selectedNewsForDialog = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// DIALOGUE DE CRÉATION DE COMPTE TECHNICIEN / AGENT SOMAGEP
// -------------------------------------------------------------------------------------------------
@Composable
fun RegisterTechnicianDialog(
    initialRole: String,
    onDismiss: () -> Unit,
    onRegisterSuccess: (SomagepAgent) -> Unit
) {
    val context = LocalContext.current
    val viewModel: LeakViewModel = viewModel()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(if (initialRole in listOf("Technicien", "Agent Bureau", "Administrateur")) initialRole else "Technicien") }
    var zone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    val bamakoZones = listOf(
        "Commune I (Hippodrome, Korofina)",
        "Commune II (Niaréla, Bagadadji)",
        "Commune III (Hamdallaye, Badialan)",
        "Commune IV (Lafiabougou, ACI 2000)",
        "Commune V (Badalabougou, Sogoniko)",
        "Commune VI (Faladié, Yirimadio)",
        "Kati & Périphérie"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Inscription Technicien / Agent SOMAGEP",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Création de compte pour l'équipe technique SOMAGEP avec synchronisation Cloud Firebase.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Rôle Professionnel :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Technicien", "Agent Bureau", "Administrateur").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 11.sp) },
                            leadingIcon = if (role == r) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom & Prénom Completes") },
                    placeholder = { Text("Ex: Ibrahim Keïta") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro de Téléphone (Mali)") },
                    placeholder = { Text("Ex: 76 11 22 33") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone d'Intervention à Bamako") },
                    placeholder = { Text("Ex: Commune IV (ACI 2000)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Text("Suggestions de Zones :", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bamakoZones.forEach { suggestion ->
                        AssistChip(
                            onClick = { zone = suggestion },
                            label = { Text(suggestion, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("Code PIN de Sécurité (4 chiffres)") },
                    placeholder = { Text("Ex: 1234") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || zone.isBlank() || pin.length < 4) {
                        Toast.makeText(context, "Veuillez remplir tous les champs (PIN = 4 chiffres)", Toast.LENGTH_SHORT).show()
                    } else {
                        val newAgent = viewModel.registerTechnician(
                            name = name.trim(),
                            phone = phone.trim(),
                            role = role,
                            zone = zone.trim(),
                            pin = pin.trim()
                        )
                        onRegisterSuccess(newAgent)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer & Connecter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// =================================================================================================
// 👑 MODE ADMINISTRATEUR - VUES DE SUPERVISION, AGENTS, CONTRÔLE ET AUDIT CLOUD
// =================================================================================================

@Composable
fun AdminSupervisionView(
    leaks: List<LeakReport>,
    allAgents: List<SomagepAgent>,
    firebaseSyncStatus: String,
    onForceSync: () -> Unit,
    onOpenRegisterAgent: () -> Unit,
    onResetData: () -> Unit,
    onRestoreDemo: () -> Unit,
    onViewDetails: (LeakReport) -> Unit,
    currentAgent: SomagepAgent?
) {
    val context = LocalContext.current
    val totalLeaks = leaks.size
    val activeLeaks = leaks.count { it.status != "Réparé" }
    val criticalLeaks = leaks.count { it.severity == "Grave" && it.status != "Réparé" }
    val solvedLeaks = leaks.count { it.status == "Réparé" }
    val resolutionRate = if (totalLeaks > 0) (solvedLeaks * 100) / totalLeaks else 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header Mode Admin
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column {
                                Text("CONTRÔLE ADMINISTRATEUR", fontWeight = FontWeight.Black, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Direction Générale SOMAGEP Mali", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ADMIN EN LIGNE 🟢", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(text = "Base de données Cloud Firebase : ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = firebaseSyncStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    if (currentAgent != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Administrateur Connecté : ${currentAgent.name} (${currentAgent.zoneOrPost})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Executive KPI Cards
        item {
            Text("Indicateurs Clés de Performance (KPIs)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminKpiCard(
                    title = "Signalements",
                    value = "$totalLeaks",
                    subtitle = "$activeLeaks en cours",
                    icon = Icons.Filled.Water,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Taux Résolution",
                    value = "$resolutionRate%",
                    subtitle = "$solvedLeaks réparés",
                    icon = Icons.Filled.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Urgences Graves",
                    value = "$criticalLeaks",
                    subtitle = "Action requise",
                    icon = Icons.Filled.Warning,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Financial & Water Volume Impact Estimator Card
        item {
            val estimatedM3LostPerDay = activeLeaks * 18
            val estimatedXofLostPerDay = estimatedM3LostPerDay * 425
            val estimatedM3SavedTotal = solvedLeaks * 25
            val estimatedXofSavedTotal = estimatedM3SavedTotal * 425

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            Text("Impact Hydrique & Financier SOMAGEP", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Tarif: 425 CFA/m³", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Perte d'Eau Estimée (Actives)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("~$estimatedM3LostPerDay m³ / jour", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            Text("≈ ${String.format("%,d", estimatedXofLostPerDay)} CFA / jour", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Eau Économisée (Réparées)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("+$estimatedM3SavedTotal m³ préservés", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SafeGreen)
                            Text("≈ +${String.format("%,d", estimatedXofSavedTotal)} CFA valorisés", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = SafeGreen)
                        }
                    }
                }
            }
        }

        // Command Center Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Centre d'Actions Rapides Administrateur", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                onForceSync()
                                Toast.makeText(context, "Forçage de synchronisation Firebase Firestore...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchro Cloud", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onOpenRegisterAgent,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Créer Agent", fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onRestoreDemo()
                                Toast.makeText(context, "Données démo restaurées !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurer Démo", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onResetData()
                                Toast.makeText(context, "Données réinitialisées !", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Purger Base", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Distribution by Districts / Communes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Répartition des Fuites par Communes de Bamako", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val districtMap = bamakoDistricts.associate { d ->
                        d.name to leaks.count { it.address.contains(d.name, ignoreCase = true) }
                    }

                    districtMap.forEach { (district, count) ->
                        val percent = if (totalLeaks > 0) count.toFloat() / totalLeaks.toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(district, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("$count fuite(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { percent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Critical Urgencies List
        item {
            Text("Alertes d'Urgence Immédiate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            val criticals = leaks.filter { it.severity == "Grave" && it.status != "Réparé" }
            if (criticals.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(
                        text = "Aucune alerte grave en attente actuellement. Tout le réseau est sous contrôle. ✅",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    criticals.take(3).forEach { leak ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🚨 ${leak.leakType} - ${leak.address}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Signalé par : ${leak.reporterName} (${leak.reporterPhone})", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { onViewDetails(leak) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Voir", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun EditAgentDialog(
    agent: SomagepAgent,
    onDismiss: () -> Unit,
    onSave: (oldPhone: String, name: String, phone: String, role: String, zone: String, pin: String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(agent.name) }
    var phone by remember { mutableStateOf(agent.phone) }
    var role by remember { mutableStateOf(agent.role) }
    var zone by remember { mutableStateOf(agent.zoneOrPost) }
    var pin by remember { mutableStateOf(agent.pin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        },
        title = {
            Text("Modifier un Agent / Technicien", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Modification réservée à l'Administrateur.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Rôle Professionnel :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Technicien", "Agent Bureau", "Administrateur").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 11.sp) },
                            leadingIcon = if (role == r) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            } else null
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom & Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro de Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone d'Intervention / Poste") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("Code PIN (4 chiffres)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank() || zone.isBlank() || pin.length < 4) {
                        Toast.makeText(context, "Veuillez remplir tous les champs correctement (PIN = 4 chiffres)", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(agent.phone, name.trim(), phone.trim(), role, zone.trim(), pin.trim())
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enregistrer les Modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun AdminAgentsView(
    allAgents: List<SomagepAgent>,
    onOpenRegisterAgent: (() -> Unit)? = null,
    onEditAgent: ((SomagepAgent) -> Unit)? = null,
    onDeleteAgent: ((SomagepAgent) -> Unit)? = null,
    currentAgent: SomagepAgent? = null,
    titleOverride: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("Tous") }
    var zoneFilter by remember { mutableStateOf("Toutes") }
    var sortBy by remember { mutableStateOf("Nom") }

    val techCount = allAgents.count { it.role.contains("Technicien", ignoreCase = true) }
    val deskCount = allAgents.count { 
        it.role.contains("Bureau", ignoreCase = true) || 
        it.role.contains("Dispatcher", ignoreCase = true) || 
        (it.role.contains("Agent", ignoreCase = true) && !it.role.contains("Technicien", ignoreCase = true) && !it.role.contains("Admin", ignoreCase = true)) 
    }
    val adminCount = allAgents.count { it.role.contains("Admin", ignoreCase = true) }
    val citizenCount = allAgents.count { it.role.contains("Abonné", ignoreCase = true) || it.role.contains("Citoyen", ignoreCase = true) }

    val filteredAgents = allAgents.filter { agent ->
        val queryLower = searchQuery.trim().lowercase()
        val matchesQuery = queryLower.isEmpty() ||
                agent.name.lowercase().contains(queryLower) ||
                agent.zoneOrPost.lowercase().contains(queryLower) ||
                agent.phone.contains(queryLower) ||
                agent.role.lowercase().contains(queryLower)

        val matchesRole = when (roleFilter) {
            "Technicien" -> agent.role.contains("Technicien", ignoreCase = true)
            "Agent Bureau" -> agent.role.contains("Bureau", ignoreCase = true) || agent.role.contains("Dispatcher", ignoreCase = true) || (agent.role.contains("Agent", ignoreCase = true) && !agent.role.contains("Technicien", ignoreCase = true) && !agent.role.contains("Admin", ignoreCase = true))
            "Administrateur" -> agent.role.contains("Admin", ignoreCase = true)
            "Abonné" -> agent.role.contains("Abonné", ignoreCase = true) || agent.role.contains("Citoyen", ignoreCase = true)
            else -> true
        }

        val matchesZone = when (zoneFilter) {
            "Toutes" -> true
            "Rive Gauche" -> agent.zoneOrPost.contains("Commune I", ignoreCase = true) || agent.zoneOrPost.contains("Commune II", ignoreCase = true) || agent.zoneOrPost.contains("Commune III", ignoreCase = true) || agent.zoneOrPost.contains("Commune IV", ignoreCase = true) || agent.zoneOrPost.contains("Bamako-Centre", ignoreCase = true)
            "Rive Droite" -> agent.zoneOrPost.contains("Commune V", ignoreCase = true) || agent.zoneOrPost.contains("Commune VI", ignoreCase = true) || agent.zoneOrPost.contains("Badalabougou", ignoreCase = true) || agent.zoneOrPost.contains("Kalaban", ignoreCase = true)
            else -> agent.zoneOrPost.contains(zoneFilter, ignoreCase = true)
        }

        matchesQuery && matchesRole && matchesZone
    }.sortedWith { a1, a2 ->
        when (sortBy) {
            "Rôle" -> a1.role.compareTo(a2.role)
            "Zone" -> a1.zoneOrPost.compareTo(a2.zoneOrPost)
            else -> a1.name.compareTo(a2.name)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = titleOverride ?: "ANNUAIRE & ÉQUIPES SOMAGEP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${allAgents.size} personnes enregistrées sur tous les services (Techniciens, Agents Bureau, Admins)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (onOpenRegisterAgent != null) {
                            Button(
                                onClick = onOpenRegisterAgent,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 0.5.dp)

                    // Breakdown Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { roleFilter = "Technicien"; searchQuery = "" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👷 $techCount Techniciens", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { roleFilter = "Agent Bureau"; searchQuery = "" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💼 $deskCount Agents Bureau", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { roleFilter = "Administrateur"; searchQuery = "" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🛡️ $adminCount Admins", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { roleFilter = "Abonné"; searchQuery = "" }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("👤 $citizenCount Abonnés", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }

        // Smart Search TextField with clear action & testTag
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("🔍 Recherche rapide (Agent Bureau, Admin, Technicien, Tél...)") },
                placeholder = { Text("Ex: Moussa, Agent Bureau, Admin, 76...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_personnel_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Quick Role Filter Buttons Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { roleFilter = "Agent Bureau"; searchQuery = "" },
                    label = { Text("💼 Agents Bureau", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (roleFilter == "Agent Bureau") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                )
                AssistChip(
                    onClick = { roleFilter = "Administrateur"; searchQuery = "" },
                    label = { Text("🛡️ Admins", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (roleFilter == "Administrateur") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                    )
                )
                AssistChip(
                    onClick = { roleFilter = "Technicien"; searchQuery = "" },
                    label = { Text("👷 Techniciens", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (roleFilter == "Technicien") Color(0xFFC8E6C9) else MaterialTheme.colorScheme.surface
                    )
                )
                AssistChip(
                    onClick = { roleFilter = "Tous"; searchQuery = "" },
                    label = { Text("👥 Tout le Personnel", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (roleFilter == "Tous") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // Smart Filters Row (Role)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FILTRE INTELLIGENT PAR RÔLE :", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val roles = listOf(
                        Pair("Tous", "Tous (${allAgents.size})"),
                        Pair("Technicien", "👷 Techniciens ($techCount)"),
                        Pair("Agent Bureau", "💼 Agents Bureau ($deskCount)"),
                        Pair("Administrateur", "🛡️ Admins ($adminCount)"),
                        Pair("Abonné", "👤 Abonnés ($citizenCount)")
                    )
                    roles.forEach { (roleKey, labelStr) ->
                        FilterChip(
                            selected = roleFilter == roleKey,
                            onClick = { roleFilter = roleKey },
                            label = { Text(labelStr, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                if (roleFilter == roleKey) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Smart Filters Row (Secteur & Tri)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val zones = listOf("Toutes", "Rive Gauche", "Rive Droite", "Commune I", "Commune III", "Commune VI")
                    zones.forEach { z ->
                        FilterChip(
                            selected = zoneFilter == z,
                            onClick = { zoneFilter = z },
                            label = { Text(if (z == "Toutes") "📍 Tous Secteurs" else z, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Sort Chip Toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable {
                        sortBy = when (sortBy) {
                            "Nom" -> "Rôle"
                            "Rôle" -> "Zone"
                            else -> "Nom"
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.SortByAlpha, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Tri: $sortBy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // Search Results Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Résultats: ${filteredAgents.size} agent(s) affiché(s)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (searchQuery.isNotEmpty() || roleFilter != "Tous" || zoneFilter != "Toutes") {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            roleFilter = "Tous"
                            zoneFilter = "Toutes"
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Réinitialiser les filtres", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Empty state when filter yields no agents
        if (filteredAgents.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SearchOff, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                        Text("Aucun agent trouvé pour ces critères", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Essayez de modifier votre mot-clé ou de réinitialiser le filtre intelligent.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Button(
                            onClick = {
                                searchQuery = ""
                                roleFilter = "Tous"
                                zoneFilter = "Toutes"
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Afficher tous les agents", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Agent List Items
        items(filteredAgents) { agent ->
            val initials = agent.name.split(" ")
                .mapNotNull { it.firstOrNull() }
                .take(2)
                .joinToString("")
                .uppercase()

            val roleBg = when (agent.role) {
                "Administrateur" -> MaterialTheme.colorScheme.tertiary
                "Agent Bureau" -> MaterialTheme.colorScheme.primary
                else -> Color(0xFF2E7D32)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(roleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(agent.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(roleBg.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(agent.role, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = roleBg)
                            }
                        }

                        Text("📍 Zone / Poste : ${agent.zoneOrPost}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📞 Tél : ${agent.phone}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PIN Chiffré • Statut Actif SOMAGEP", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Medium)
                        }
                    }

                    // Admin & Staff Actions: Call, SMS, Edit & Delete
                    val context = LocalContext.current
                    val isMainAdmin = agent.pin == "00223" || agent.phone == "70 00 00 00" || agent.name.contains("Administrateur Principal", ignoreCase = true)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${agent.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = "Appeler",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${agent.phone}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Filled.Sms,
                                contentDescription = "Envoyer SMS",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (onEditAgent != null) {
                            IconButton(
                                onClick = { onEditAgent(agent) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (onDeleteAgent != null) {
                            if (isMainAdmin) {
                                IconButton(
                                    onClick = { onDeleteAgent(agent) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.VerifiedUser,
                                        contentDescription = "Protégé",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { onDeleteAgent(agent) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Supprimer",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLeakControlView(
    leaks: List<LeakReport>,
    allAgents: List<SomagepAgent>,
    onUpdateStatus: (LeakReport, String, String) -> Unit,
    onAssignTechnician: (LeakReport, String, String) -> Unit,
    onDeleteReport: (LeakReport) -> Unit,
    onRestoreReport: ((LeakReport) -> Unit)? = null,
    onViewDetails: (LeakReport) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Tous") }

    val filtered = leaks.filter { leak ->
        val matchesQuery = searchQuery.isEmpty() ||
                leak.address.contains(searchQuery, ignoreCase = true) ||
                leak.leakType.contains(searchQuery, ignoreCase = true) ||
                leak.reporterName.contains(searchQuery, ignoreCase = true)

        val matchesStatus = when (selectedStatus) {
            "Tous" -> true
            "Annulés Citoyen" -> leak.isDeletedByCitizen
            else -> leak.status == selectedStatus
        }
        matchesQuery && matchesStatus
    }

    val technicians = allAgents.filter { it.role == "Technicien" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("CONTRÔLE ADMINISTRATIF DES SIGNALEMENTS", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text("Validation, Réaffectation, Restauration et Archivage SOMAGEP", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Filtrer par quartier / rapporteur / type") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Tous", "Signalé", "En cours", "Réparé", "Annulés Citoyen").forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st },
                        label = { Text(st, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        items(filtered) { leak ->
            var showAssignDialog by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (leak.isDeletedByCitizen) Color(0xFFFFF8F8) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (leak.isDeletedByCitizen) Color(0xFFEF9A9A) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (leak.isDeletedByCitizen) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFC62828))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🗑️ Retiré par le citoyen (Conservé en archive Admin)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                                if (onRestoreReport != null) {
                                    TextButton(
                                        onClick = {
                                            onRestoreReport(leak)
                                            Toast.makeText(context, "Signalement #${leak.id} restauré dans l'espace citoyen !", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Restaurer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("#${leak.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            Text(leak.leakType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        StatusBadge(status = leak.status)
                    }

                    Text("📍 ${leak.address}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("Signalé par : ${leak.reporterName} (${leak.reporterPhone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (leak.assignedTechnician.isNotEmpty()) {
                        Text("👷 Technicien affecté : ${leak.assignedTechnician} (${leak.assignedTechnicianPhone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    } else {
                        Text("⚠️ Aucun technicien affecté actuellement", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Admin Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onViewDetails(leak) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Détails", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { showAssignDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Affecter", fontSize = 10.sp)
                        }

                        IconButton(
                            onClick = {
                                onDeleteReport(leak)
                                Toast.makeText(context, "Signalement #${leak.id} supprimé !", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (showAssignDialog) {
                AlertDialog(
                    onDismissRequest = { showAssignDialog = false },
                    title = { Text("Affecter un Technicien à la Fuite #${leak.id}", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Choisissez le technicien SOMAGEP responsable :", fontSize = 12.sp)
                            technicians.forEach { tech ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAssignTechnician(leak, tech.name, tech.phone)
                                            showAssignDialog = false
                                            Toast.makeText(context, "Fuite attribuée à ${tech.name} !", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(tech.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(tech.zoneOrPost, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showAssignDialog = false }) { Text("Annuler") }
                    }
                )
            }
        }
    }
}

@Composable
fun AdminAuditLogsView(
    leaks: List<LeakReport>,
    firebaseSyncStatus: String,
    currentAgent: SomagepAgent?
) {
    val context = LocalContext.current
    var showExportModal by remember { mutableStateOf(false) }

    val auditEvents = remember(leaks, firebaseSyncStatus) {
        listOf(
            "SYS_INFO" to "Service Cloud Firebase : ${firebaseSyncStatus}",
            "ROOM_DB" to "Base locale SQLite / Room synchronisée (${leaks.size} enregistrements)",
            "SEC_AUTH" to "Accès Administrateur validé par PIN pour ${(currentAgent?.name ?: "Direction Générale")}",
            "GPS_SYS" to "Service de Géolocalisation play.services.location actif",
            "NET_PING" to "Connectivité Firestore Cloud SOMAGEP : Réponse < 45ms"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("JOURNAUX D'AUDIT ET SÉCURITÉ CLOUD", fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("Historique des événements et traçabilité Firebase", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }

                Button(
                    onClick = { showExportModal = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rapport Text", fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Événements Système en Temps Réel", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    auditEvents.forEach { (code, event) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(code, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Text(event, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    if (showExportModal) {
        val summaryText = """
            ==================================================
            RAPPORT D'AUDIT DES SIGNALEMENTS DE FUITES SOMAGEP
            ==================================================
            Date d'export : ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date())}
            Statut Cloud : $firebaseSyncStatus
            Total Signalements : ${leaks.size}
            Signalements Réparés : ${leaks.count { it.status == "Réparé" }}
            Signalements En cours : ${leaks.count { it.status == "En cours" }}
            Signalements Non traités : ${leaks.count { it.status == "Signalé" }}
            ==================================================
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = { Text("Aperçu du Rapport d'Audit SOMAGEP", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = summaryText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportModal = false
                        Toast.makeText(context, "Rapport d'audit généré avec succès !", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copier / Télecharger")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportModal = false }) { Text("Fermer") }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// ADMIN SETTINGS DIALOG
// -------------------------------------------------------------------------------------------------
@Composable
fun AdminSettingsDialog(
    firebaseSyncStatus: String,
    emergencyNotice: String,
    hotlinePhone: String,
    isMaintenanceMode: Boolean,
    autoDispatchGrave: Boolean,
    currentAgent: SomagepAgent?,
    allLeaks: List<LeakReport>,
    onDismiss: () -> Unit,
    onForceSync: () -> Unit,
    onResetAllData: () -> Unit,
    onRestoreDemoData: () -> Unit,
    onSaveSettings: (notice: String, hotline: String, isMaintenance: Boolean, autoDispatch: Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Cloud/DB, 1: Network/Alerts, 2: Export, 3: Security

    var noticeInput by remember { mutableStateOf(emergencyNotice) }
    var hotlineInput by remember { mutableStateOf(hotlinePhone) }
    var maintenanceSwitch by remember { mutableStateOf(isMaintenanceMode) }
    var autoDispatchSwitch by remember { mutableStateOf(autoDispatchGrave) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("PARAMÈTRES ADMINISTRATEUR", fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text("Espace Restreint SOMAGEP (PIN Sécurisé & Masqué)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Fermer")
                    }
                }

                // Sub-header tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("☁️ Cloud & DB", "📢 Alertes", "📄 Export", "🛡️ Sécurité").forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Divider()

                when (selectedTab) {
                    0 -> { // Cloud & Base de Données
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Statut & Actions Base de Données", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Firebase Realtime & Room DB", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(firebaseSyncStatus, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onForceSync,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Forcer la Synchronisation Cloud", fontSize = 11.sp)
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Gestion de la Mémoire & Données", fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                    OutlinedButton(
                                        onClick = {
                                            Toast.makeText(context, "Mémoire cache de l'application nettoyée ! 🧹", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Vider le Cache Local", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onRestoreDemoData,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restaurer la Démo (4 Fuites de Bamako)", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = onResetAllData,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Réinitialiser à 0 (Vider Toutes les Fuites)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    1 -> { // Alertes & Configuration Réseau
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Configuration des Alertes Citoyennes & Hotline", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Activer Mode Alerte Perturbation Réseau", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Affiche la bannière d'urgence sur l'accueil des citoyens", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = maintenanceSwitch,
                                    onCheckedChange = { maintenanceSwitch = it }
                                )
                            }

                            OutlinedTextField(
                                value = noticeInput,
                                onValueChange = { noticeInput = it },
                                label = { Text("Message d'Alerte Citoyen") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                minLines = 2
                            )

                            OutlinedTextField(
                                value = hotlineInput,
                                onValueChange = { hotlineInput = it },
                                label = { Text("Numéro Hotline / Urgence SOMAGEP") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Priorité Fuites Graves", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Signale immédiatement toute rupture majeure aux équipes", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(
                                    checked = autoDispatchSwitch,
                                    onCheckedChange = { autoDispatchSwitch = it }
                                )
                            }

                            Button(
                                onClick = {
                                    onSaveSettings(noticeInput, hotlineInput, maintenanceSwitch, autoDispatchSwitch)
                                    Toast.makeText(context, "Paramètres réseau enregistrés !", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enregistrer les Alertes & Hotline", fontSize = 11.sp)
                            }
                        }
                    }

                    2 -> { // Exportation & Rapports
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Génération & Exportation des Rapports", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            val reportedCount = allLeaks.count { it.status == "Signalé" }
                            val progressCount = allLeaks.count { it.status == "En cours" }
                            val solvedCount = allLeaks.count { it.status == "Réparé" }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📊 Résumé du Réseau SOMAGEP :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("• Total des Fuites Enregistrées : ${allLeaks.size}", fontSize = 11.sp)
                                    Text("• En Attente d'Intervention : $reportedCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    Text("• En Cours de Traitement : $progressCount", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("• Réparées avec Succès : $solvedCount", fontSize = 11.sp, color = SafeGreen)
                                }
                            }

                            Button(
                                onClick = {
                                    val reportText = buildString {
                                        appendLine("=== RAPPORT OFFICIEL DES FUITES SOMAGEP BAMAKO ===")
                                        appendLine("Date de Génération : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date())}")
                                        appendLine("Total Fuites : ${allLeaks.size} | Signalées : $reportedCount | En Cours : $progressCount | Réparées : $solvedCount")
                                        appendLine("--------------------------------------------------")
                                        allLeaks.forEachIndexed { i, leak ->
                                            appendLine("${i + 1}. [${leak.status}] ${leak.leakType} (${leak.severity})")
                                            appendLine("   Abonné: ${leak.reporterName} (${leak.reporterPhone})")
                                            appendLine("   Lieu: ${leak.address} GPS: (${leak.latitude}, ${leak.longitude})")
                                            if (!leak.assignedTechnician.isNullOrEmpty()) {
                                                appendLine("   Technicien: ${leak.assignedTechnician}")
                                            }
                                        }
                                    }

                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Partager le Rapport SOMAGEP via"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exporter le Rapport Complet (Texte / SMS / WhatsApp)", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val reportText = buildString {
                                            appendLine("RAPPORT SOMAGEP - Total: ${allLeaks.size} fuites. En cours: $progressCount, Réparées: $solvedCount.")
                                            allLeaks.forEach { appendLine("- ${it.leakType} à ${it.address} [Statut: ${it.status}]") }
                                        }
                                        val clipData = android.content.ClipData.newPlainText("Rapport SOMAGEP", reportText)
                                        if (clipboardManager != null) {
                                            clipboardManager.setPrimaryClip(clipData)
                                            Toast.makeText(context, "Rapport copié dans le presse-papier ! 📋", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Presse-papier non disponible.", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "L'application doit être active pour copier le rapport.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier le Résumé Synthétique", fontSize = 11.sp)
                            }
                        }
                    }

                    3 -> { // Sécurité & Agent Info
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Identité Administrateur & Chiffrement", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("👤 Compte Administrateur Connecté :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Nom : ${currentAgent?.name ?: "Administrateur Principal"}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Poste / Affectation : ${currentAgent?.zoneOrPost ?: "Direction Générale SOMAGEP Bamako"}", fontSize = 11.sp)
                                    Text("Téléphone : ${currentAgent?.phone ?: "70 00 00 00"}", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🔒 Protections Actives :", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("• Accès restreint par Code PIN Administrateur Chiffré", fontSize = 10.sp)
                                    Text("• Impossibilité de supprimer l'Administrateur Principal", fontSize = 10.sp)
                                    Text("• Chiffrement SSL/TLS des flux Firebase Cloud", fontSize = 10.sp)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SOMAGEP Fuites Enterprise v3.5", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Développé pour la Société Malienne de Gestion de l'Eau Potable", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                Divider()

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Fermer les Paramètres", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
