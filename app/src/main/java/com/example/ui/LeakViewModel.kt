package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LeakDatabase
import com.example.data.LeakReport
import com.example.data.LeakRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SomagepAgent(
    val name: String,
    val phone: String,
    val role: String, // "Technicien", "Agent Bureau", or "Administrateur"
    val zoneOrPost: String,
    val pin: String
)

val somagepAgentsList = listOf(
    // Administrateurs (Direction & IT) - Premier Administrateur avec PIN 00223
    SomagepAgent("Administrateur Principal", "70 00 00 00", "Administrateur", "Siège National - Direction Générale", "00223"),
    SomagepAgent("Oumar Traoré (Admin Systèmes)", "76 00 11 22", "Administrateur", "Direction des Systèmes d'Information", "1234"),

    // Techniciens Terrain (Equipes Spécialisées SOMAGEP Bamako)
    SomagepAgent("Moussa Diarra", "76 12 34 56", "Technicien", "Commune I & II (Hippodrome, Niaréla)", "7612"),
    SomagepAgent("Amina Touré", "66 54 32 10", "Technicien", "Commune III & IV (Hamdallaye, ACI)", "6654"),
    SomagepAgent("Amadou Diallo", "70 99 88 77", "Technicien", "Commune V & VI (Sogoniko, Faladié)", "7099"),
    SomagepAgent("Ousmane Keïta", "65 22 11 00", "Technicien", "Communes Ouest (Baco-Djicoroni, Sebenikoro)", "6522"),
    SomagepAgent("Bakary Sidibé", "78 90 12 34", "Technicien", "Zone Est (Sotuba, Titibougou, Moribabougou)", "7890"),
    SomagepAgent("Fanta Konaté", "67 89 01 23", "Technicien", "Zone Hauteurs (Kati, Darsalam, Point G)", "6789"),
    SomagepAgent("Modibo Sangaré", "71 12 23 34", "Technicien", "Zone Sud (Kalaban Coro, Kabala, Daoudabougou)", "7112"),
    SomagepAgent("Kassim Traoré", "64 43 32 21", "Technicien", "Zone Nord-Est (Korofina, Djélibougou)", "6443"),
    
    // Agents Bureau (Dispatchers)
    SomagepAgent("Fatoumata Coulibaly", "75 44 33 22", "Agent Bureau", "Superviseur Réseau Bamako", "7544"),
    SomagepAgent("Sékou Traoré", "68 11 22 33", "Agent Bureau", "Coordinateur des Interventions", "6811"),

    // Abonnés & Citoyens Référents
    SomagepAgent("Ibrahim Keïta", "77 12 34 56", "Abonné / Citoyen", "Bamako Coura (Abonné SOMAGEP #4492)", "0000"),
    SomagepAgent("Mariam Sidibé", "66 99 88 11", "Abonné / Citoyen", "Hippodrome (Abonnée SOMAGEP #1209)", "0000"),
    SomagepAgent("Adama Coulibaly", "76 33 22 11", "Abonné / Citoyen", "ACI 2000 (Abonné SOMAGEP #8821)", "0000")
)

class LeakViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LeakRepository
    private val prefs = application.getSharedPreferences("somagep_prefs", Context.MODE_PRIVATE)
    
    // UI state for search, filters, and role selection
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("Tous") // "Tous", "Signalé", "En cours", "Réparé"
    val statusFilter = _statusFilter.asStateFlow()

    private val _severityFilter = MutableStateFlow("Tous") // "Tous", "Grave", "Moyen", "Faible"
    val severityFilter = _severityFilter.asStateFlow()

    private val _selectedRole = MutableStateFlow("Citoyen") // "Citoyen", "Technicien", "Agent Bureau", "Administrateur"
    val selectedRole = _selectedRole.asStateFlow()

    private val _currentAgent = MutableStateFlow<SomagepAgent?>(null)
    val currentAgent = _currentAgent.asStateFlow()

    // Dynamic list of registered agents
    private val _allAgents = MutableStateFlow<List<SomagepAgent>>(somagepAgentsList)
    val allAgents = _allAgents.asStateFlow()

    // Firebase Sync Status
    private val _firebaseSyncStatus = MutableStateFlow("Firebase & Room DB Prêt")
    val firebaseSyncStatus = _firebaseSyncStatus.asStateFlow()

    // System Settings for Administrators
    private val _emergencyNotice = MutableStateFlow(
        prefs.getString("sys_notice", "Aucune perturbation réseau majeure. Service SOMAGEP opérationnel sur l'ensemble du district de Bamako.") ?: "Aucune perturbation réseau majeure."
    )
    val emergencyNotice = _emergencyNotice.asStateFlow()

    private val _hotlinePhone = MutableStateFlow(
        prefs.getString("sys_hotline", "20 22 25 26") ?: "20 22 25 26"
    )
    val hotlinePhone = _hotlinePhone.asStateFlow()

    private val _isMaintenanceMode = MutableStateFlow(
        prefs.getBoolean("sys_maintenance", false)
    )
    val isMaintenanceMode = _isMaintenanceMode.asStateFlow()

    private val _autoDispatchGrave = MutableStateFlow(
        prefs.getBoolean("sys_auto_dispatch", true)
    )
    val autoDispatchGrave = _autoDispatchGrave.asStateFlow()

    fun updateSystemSettings(
        notice: String,
        hotline: String,
        isMaintenance: Boolean,
        autoDispatch: Boolean
    ) {
        _emergencyNotice.value = notice
        _hotlinePhone.value = hotline
        _isMaintenanceMode.value = isMaintenance
        _autoDispatchGrave.value = autoDispatch

        prefs.edit()
            .putString("sys_notice", notice)
            .putString("sys_hotline", hotline)
            .putBoolean("sys_maintenance", isMaintenance)
            .putBoolean("sys_auto_dispatch", autoDispatch)
            .apply()
    }

    fun setCurrentAgent(agent: SomagepAgent?) {
        _currentAgent.value = agent
        if (agent != null) {
            _selectedRole.value = agent.role
        } else {
            _selectedRole.value = "Citoyen"
        }
    }

    init {
        val database = LeakDatabase.getDatabase(application)
        repository = LeakRepository(database.leakReportDao())
        
        loadAgents()

        // Seed some initial Bamako leaks if empty and never seeded (disabled by default to start at 0)
        viewModelScope.launch {
            repository.allLeaks.collect { list ->
                val hasSeeded = prefs.getBoolean("has_seeded", true)
                if (list.isEmpty() && !hasSeeded) {
                    seedData()
                    prefs.edit().putBoolean("has_seeded", true).apply()
                }
            }
        }

        syncWithFirebaseCloud()
    }

    private fun loadAgents() {
        try {
            val jsonStr = prefs.getString("somagep_agents_v3", null)
            if (!jsonStr.isNullOrEmpty()) {
                val jsonArray = JSONArray(jsonStr)
                val list = mutableListOf<SomagepAgent>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        SomagepAgent(
                            name = obj.getString("name"),
                            phone = obj.getString("phone"),
                            role = obj.getString("role"),
                            zoneOrPost = obj.getString("zone"),
                            pin = obj.getString("pin")
                        )
                    )
                }
                // Ensure all default agents and technicians exist
                for (defaultAgent in somagepAgentsList) {
                    if (list.none { it.name.equals(defaultAgent.name, ignoreCase = true) || it.phone == defaultAgent.phone }) {
                        list.add(defaultAgent)
                    }
                }
                _allAgents.value = list
                saveAgentsToPrefs(list)
            } else {
                _allAgents.value = somagepAgentsList
                saveAgentsToPrefs(somagepAgentsList)
            }
        } catch (e: Exception) {
            Log.e("LeakViewModel", "Error loading agents: ${e.message}")
            _allAgents.value = somagepAgentsList
        }
    }

    private fun saveAgentsToPrefs(list: List<SomagepAgent>) {
        try {
            val jsonArray = JSONArray()
            for (agent in list) {
                val obj = JSONObject().apply {
                    put("name", agent.name)
                    put("phone", agent.phone)
                    put("role", agent.role)
                    put("zone", agent.zoneOrPost)
                    put("pin", agent.pin)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("somagep_agents_v3", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("LeakViewModel", "Error saving agents: ${e.message}")
        }
    }

    fun registerTechnician(
        name: String,
        phone: String,
        role: String,
        zone: String,
        pin: String
    ): SomagepAgent {
        val newAgent = SomagepAgent(
            name = name,
            phone = phone,
            role = role,
            zoneOrPost = zone,
            pin = pin
        )

        val updatedList = _allAgents.value + newAgent
        _allAgents.value = updatedList
        saveAgentsToPrefs(updatedList)

        // Try syncing technician profile to Firebase Cloud
        syncTechnicianToFirebase(newAgent)

        return newAgent
    }

    fun updateAgent(
        oldPhone: String,
        name: String,
        phone: String,
        role: String,
        zone: String,
        pin: String
    ) {
        val updatedList = _allAgents.value.map { agent ->
            if (agent.phone == oldPhone) {
                SomagepAgent(
                    name = name,
                    phone = phone,
                    role = role,
                    zoneOrPost = zone,
                    pin = pin
                )
            } else agent
        }
        _allAgents.value = updatedList
        saveAgentsToPrefs(updatedList)

        // Update current agent if it was the edited agent
        if (_currentAgent.value?.phone == oldPhone) {
            _currentAgent.value = SomagepAgent(name, phone, role, zone, pin)
        }
    }

    fun deleteAgent(agentPhone: String) {
        val targetAgent = _allAgents.value.find { it.phone == agentPhone }
        if (targetAgent != null && (targetAgent.pin == "00223" || targetAgent.phone == "70 00 00 00" || targetAgent.name.contains("Administrateur Principal", ignoreCase = true))) {
            Log.w("LeakViewModel", "Cannot delete Main Admin")
            return
        }

        val updatedList = _allAgents.value.filter { it.phone != agentPhone }
        _allAgents.value = updatedList
        saveAgentsToPrefs(updatedList)

        if (_currentAgent.value?.phone == agentPhone) {
            setCurrentAgent(null)
        }
    }

    fun syncWithFirebaseCloud() {
        viewModelScope.launch {
            _firebaseSyncStatus.value = "Connexion Firebase..."
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("somagep_leaks")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        _firebaseSyncStatus.value = "Firebase Cloud Synchronisé (${querySnapshot.size()} fiches)"
                    }
                    .addOnFailureListener { e ->
                        _firebaseSyncStatus.value = "Base Local Room Active (Firebase Offline: ${e.localizedMessage})"
                    }
            } catch (e: Exception) {
                _firebaseSyncStatus.value = "Base Local Room Active (Firebase configuré)"
            }
        }
    }

    private fun syncTechnicianToFirebase(agent: SomagepAgent) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val techMap = hashMapOf(
                "name" to agent.name,
                "phone" to agent.phone,
                "role" to agent.role,
                "zone" to agent.zoneOrPost,
                "registeredAt" to System.currentTimeMillis()
            )
            firestore.collection("somagep_technicians")
                .document(agent.phone)
                .set(techMap)
                .addOnSuccessListener {
                    _firebaseSyncStatus.value = "Compte Technicien Synchronisé Cloud Firebase !"
                }
        } catch (e: Exception) {
            Log.w("LeakViewModel", "Firebase sync skipped: ${e.message}")
        }
    }

    // Exposed leak reports reactively filtered by search query, status and severity
    val filteredLeaks: StateFlow<List<LeakReport>> = combine(
        repository.allLeaks,
        _searchQuery,
        _statusFilter,
        _severityFilter
    ) { leaks, query, status, severity ->
        leaks.filter { leak ->
            val matchesQuery = query.isEmpty() || 
                    leak.address.contains(query, ignoreCase = true) || 
                    leak.leakType.contains(query, ignoreCase = true) || 
                    leak.description.contains(query, ignoreCase = true)
            
            val matchesStatus = status == "Tous" || leak.status == status
            
            val matchesSeverity = severity == "Tous" || leak.severity == severity
            
            matchesQuery && matchesStatus && matchesSeverity
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun setSeverityFilter(severity: String) {
        _severityFilter.value = severity
    }

    fun setRole(role: String) {
        _selectedRole.value = role
    }

    fun reportLeak(
        name: String,
        phone: String,
        leakType: String,
        severity: String,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        photoPath: String,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val report = LeakReport(
                reporterName = name,
                reporterPhone = phone,
                leakType = leakType,
                severity = severity,
                description = description,
                latitude = latitude,
                longitude = longitude,
                address = address,
                photoPath = photoPath,
                timestamp = System.currentTimeMillis(),
                status = "Signalé"
            )
            val insertedId = repository.insertLeak(report)

            try {
                val context = getApplication<Application>()
                com.example.util.NotificationHelper.sendLeakStatusNotification(
                    context = context,
                    reportId = insertedId,
                    leakType = leakType,
                    address = address,
                    newStatus = "Signalé"
                )
            } catch (e: Exception) {
                Log.w("LeakViewModel", "Local notification error: ${e.message}")
            }

            try {
                val firestore = FirebaseFirestore.getInstance()
                val leakMap = hashMapOf(
                    "id" to insertedId,
                    "reporterName" to name,
                    "reporterPhone" to phone,
                    "leakType" to leakType,
                    "severity" to severity,
                    "description" to description,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "address" to address,
                    "photoPath" to photoPath,
                    "timestamp" to report.timestamp,
                    "status" to "Signalé"
                )
                firestore.collection("somagep_leaks")
                    .add(leakMap)
                    .addOnSuccessListener { docRef ->
                        _firebaseSyncStatus.value = "Signalement transmis à Firebase Cloud (Doc ID: ${docRef.id})"
                        onComplete(true, "Signalement #$insertedId transmis avec succès à la SOMAGEP & Firebase Cloud !")
                    }
                    .addOnFailureListener { e ->
                        _firebaseSyncStatus.value = "Signalement sauvegardé localement (Room DB)"
                        onComplete(true, "Signalement #$insertedId enregistré localement (Base Room). Synchro Cloud en attente.")
                    }
            } catch (e: Exception) {
                Log.w("LeakViewModel", "Firebase push skipped: ${e.message}")
                onComplete(true, "Signalement #$insertedId enregistré localement avec succès (Room DB).")
            }
        }
    }

    fun updateReportStatus(report: LeakReport, newStatus: String, notes: String) {
        viewModelScope.launch {
            val updated = report.copy(
                status = newStatus,
                technicianNotes = notes
            )
            repository.updateLeak(updated)

            try {
                val context = getApplication<Application>()
                com.example.util.NotificationHelper.sendLeakStatusNotification(
                    context = context,
                    reportId = report.id.toLong(),
                    leakType = report.leakType,
                    address = report.address,
                    newStatus = newStatus,
                    notes = notes
                )
            } catch (e: Exception) {
                Log.w("LeakViewModel", "Local notification error: ${e.message}")
            }
        }
    }

    fun assignTechnician(report: LeakReport, techName: String, techPhone: String) {
        viewModelScope.launch {
            val newStatus = if (report.status == "Signalé") "En cours" else report.status
            val updated = report.copy(
                assignedTechnician = techName,
                assignedTechnicianPhone = techPhone,
                status = newStatus
            )
            repository.updateLeak(updated)

            try {
                val context = getApplication<Application>()
                com.example.util.NotificationHelper.sendLeakStatusNotification(
                    context = context,
                    reportId = report.id.toLong(),
                    leakType = report.leakType,
                    address = report.address,
                    newStatus = newStatus,
                    notes = "Technicien $techName affecté à l'intervention."
                )
            } catch (e: Exception) {
                Log.w("LeakViewModel", "Local notification error: ${e.message}")
            }
        }
    }

    fun triggerTestPushNotification(report: LeakReport? = null) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (report != null) {
                com.example.util.NotificationHelper.sendLeakStatusNotification(
                    context = context,
                    reportId = report.id.toLong(),
                    leakType = report.leakType,
                    address = report.address,
                    newStatus = report.status,
                    notes = report.technicianNotes
                )
            } else {
                com.example.util.NotificationHelper.sendLeakStatusNotification(
                    context = context,
                    reportId = 9999L,
                    leakType = "Tuyau Principal",
                    address = "Hamdallaye ACI 2000, Bamako",
                    newStatus = "En cours",
                    notes = "Test de notification push locale SOMAGEP"
                )
            }
        }
    }

    fun softDeleteByCitizen(report: LeakReport) {
        viewModelScope.launch {
            val updated = report.copy(isDeletedByCitizen = true)
            repository.updateLeak(updated)
        }
    }

    fun rateIntervention(report: LeakReport, rating: Int, feedback: String) {
        viewModelScope.launch {
            val updated = report.copy(
                citizenRating = rating,
                citizenFeedback = feedback
            )
            repository.updateLeak(updated)
        }
    }

    fun restoreReport(report: LeakReport) {
        viewModelScope.launch {
            val updated = report.copy(isDeletedByCitizen = false)
            repository.updateLeak(updated)
        }
    }

    fun deleteReport(report: LeakReport) {
        viewModelScope.launch {
            repository.deleteLeak(report)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            prefs.edit().putBoolean("has_seeded", true).apply()
            repository.deleteAll()
        }
    }

    fun reseedData() {
        viewModelScope.launch {
            prefs.edit().putBoolean("has_seeded", false).apply()
            repository.deleteAll()
        }
    }

    private suspend fun seedData() {
        val seedReports = listOf(
            LeakReport(
                reporterName = "Mamadou Diallo",
                reporterPhone = "76 54 32 10",
                leakType = "Tuyau Principal",
                severity = "Grave",
                description = "Un tuyau d'adduction principal s'est rompu sous la chaussée. L'eau jaillit en abondance et commence à inonder la route principale.",
                latitude = 12.6354,
                longitude = -8.0256,
                address = "Hamdallaye ACI 2000, près du Monument de l'Obélisque",
                photoPath = "preset_pipe",
                status = "Signalé"
            ),
            LeakReport(
                reporterName = "Awa Keïta",
                reporterPhone = "66 99 88 77",
                leakType = "Compteur d'Eau",
                severity = "Moyen",
                description = "Fuite continue au niveau du raccord de notre compteur d'abonnement SOMAGEP. Le sol est détrempé tout autour.",
                latitude = 12.6205,
                longitude = -7.9958,
                address = "Badalabougou, Rue de l'Université, face au fleuve Niger",
                photoPath = "preset_meter",
                status = "En cours",
                technicianNotes = "Équipe technique SOMAGEP en route pour remplacer le joint d'étanchéité."
            ),
            LeakReport(
                reporterName = "Oumar Traoré",
                reporterPhone = "70 12 34 56",
                leakType = "Robinet",
                severity = "Faible",
                description = "Robinet de purge de la borne fontaine publique qui goutte sans arrêt, gaspillant des dizaines de litres d'eau potable par jour.",
                latitude = 12.6002,
                longitude = -7.9551,
                address = "Sogoniko, ruelle adjacente à la grande gare routière",
                photoPath = "preset_faucet",
                status = "Réparé",
                technicianNotes = "Robinet réparé le 18 Juillet 2026. Remplacement de la tête de robinet défectueuse."
            ),
            LeakReport(
                reporterName = "Fatoumata Coulibaly",
                reporterPhone = "65 43 21 09",
                leakType = "Inondation de Rue",
                severity = "Grave",
                description = "Inondation majeure provoquée par une rupture de canalisation souterraine SOMAGEP. L'eau monte dans les cours des habitations environnantes.",
                latitude = 12.6458,
                longitude = -8.0123,
                address = "Hamdallaye, Boulevard du 22 Octobre",
                photoPath = "preset_street",
                status = "Signalé"
            )
        )
        for (report in seedReports) {
            repository.insertLeak(report)
        }
    }
}
