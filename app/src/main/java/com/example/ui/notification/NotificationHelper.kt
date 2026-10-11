package com.example.ui.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.InventoryItemEntity

data class ThresholdAlert(
    val id: String,
    val itemId: Long,
    val itemName: String,
    val currentQuantity: Int,
    val minThreshold: Int,
    val unit: String,
    val storeName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationHelper {
    const val CHANNEL_ID = "inventory_stock_alerts"
    private const val CHANNEL_NAME = "Alertes de stock & Seuil minimum"
    private const val CHANNEL_DESC = "Alertes lorsque le stock d'un produit atteint ou passe sous son seuil prédéfini."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendThresholdNotification(
        context: Context,
        item: InventoryItemEntity,
        currentQuantity: Int,
        minThreshold: Int,
        storeName: String
    ) {
        createNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isOutOfStock = currentQuantity <= 0
        val title = if (isOutOfStock) {
            "🚨 Stock épuisé : ${item.name}"
        } else {
            "⚠️ Seuil critique atteint : ${item.name}"
        }

        val message = if (isOutOfStock) {
            "Rupture de stock ! Produit automatiquement transféré sur votre liste [$storeName]."
        } else {
            "Il ne reste que $currentQuantity ${item.unit} (seuil défini : $minThreshold). Pensez à réapprovisionner chez $storeName."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (item.id.toInt() + 1000).coerceAtLeast(1)
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked or not granted
        }
    }

    fun sendTestNotification(context: Context) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🔔 Test d'alerte : Seuil minimum")
            .setContentText("Le système d'alerte proactive d'inventaire fonctionne parfaitement !")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(9999, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
