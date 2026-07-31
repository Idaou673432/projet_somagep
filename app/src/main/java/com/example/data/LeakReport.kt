package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leak_reports")
data class LeakReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reporterName: String,
    val reporterPhone: String,
    val leakType: String, // "Tuyau Principal", "Robinet", "Compteur d'Eau", "Inondation de Rue", "Autre"
    val severity: String, // "Faible", "Moyen", "Grave"
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String, // Neighborhood / Quartier in Bamako
    val photoPath: String, // URI string, or identifier of preset leak image
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Signalé", // "Signalé", "En cours", "Réparé"
    val technicianNotes: String = "",
    val assignedTechnician: String = "",
    val assignedTechnicianPhone: String = "",
    val isDeletedByCitizen: Boolean = false,
    val citizenRating: Int = 0,
    val citizenFeedback: String = ""
)
