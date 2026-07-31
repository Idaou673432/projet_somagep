package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_ID = "somagep_leak_notifications"
    const val CHANNEL_NAME = "Suivi des Signalements SOMAGEP"
    const val CHANNEL_DESC = "Notifications locales pour l'avancement de la réparation des fuites d'eau."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendLeakStatusNotification(
        context: Context,
        reportId: Long,
        leakType: String,
        address: String,
        newStatus: String,
        notes: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_LEAK_ID", reportId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            reportId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, contentText, iconRes) = when (newStatus) {
            "Signalé" -> Triple(
                "💧 Signalement Reçu (#$reportId)",
                "Votre signalement pour '$leakType' à $address a été bien reçu et transmis au service technique SOMAGEP.",
                R.mipmap.ic_launcher
            )
            "En cours" -> Triple(
                "🛠️ Intervention en Cours (#$reportId)",
                "Les techniciens SOMAGEP sont en cours d'intervention sur '$leakType' à $address." +
                        if (!notes.isNull_or_empty()) " Note: $notes" else "",
                R.mipmap.ic_launcher
            )
            "Réparé" -> Triple(
                "✅ Fuite Réparée (#$reportId)",
                "Bonne nouvelle ! La fuite d'eau ($leakType) à $address a été réparée avec succès par SOMAGEP. Merci pour votre citoyenneté !",
                R.mipmap.ic_launcher
            )
            else -> Triple(
                "💧 Mise à jour Signalement (#$reportId)",
                "Statut : $newStatus pour la fuite '$leakType' à $address.",
                R.mipmap.ic_launcher
            )
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(reportId.toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun CharSequence?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}
