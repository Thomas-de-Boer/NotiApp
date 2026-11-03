package com.notiapp.alert

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notiapp.R
import com.notiapp.data.History
import com.notiapp.data.Settings
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import com.notiapp.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Berichten die niet laten bellen: een gewone melding (met geluid) of een stille melding
 * (geen geluid, staat gewoon tussen je meldingen).
 */
object MessageNotifier {
    private const val TAG = "NotiApp"

    fun show(context: Context, message: FamilyMessage) {
        History.addReceived(
            message.alertId, message.senderName, message.to, message.text, message.sentAt,
            senderId = message.senderId, mode = message.mode.name,
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val silent = message.mode == FamilyMessage.Mode.STIL
        val id = notificationId(message.alertId)
        // Tikken = app openen en "gezien" terugsturen.
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(AlertService.EXTRA_ALERT_ID, message.alertId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ok = PendingIntent.getBroadcast(
            context, id,
            Intent(context, AckReceiver::class.java).putExtra(AlertService.EXTRA_ALERT_ID, message.alertId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            context, if (silent) Notifications.CHANNEL_SILENT else Notifications.CHANNEL_MESSAGES,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(if (silent) R.string.alert_title_silent else R.string.alert_title, message.senderName))
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(message.sentAt)
            .setShowWhen(true)
            .setContentIntent(open)
            .addAction(0, context.getString(R.string.alert_ok), ok)
            .setSilent(silent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** De afzender heeft het bericht geannuleerd. */
    fun cancel(context: Context, alertId: String) {
        val entry = History.received(alertId) ?: return
        if (entry.mode == FamilyMessage.Mode.BELLEN.name) return // dat regelt AlertService
        NotificationManagerCompat.from(context).cancel(notificationId(alertId))
        History.setReceivedStatus(alertId, History.Status.GEANNULEERD)
    }

    /**
     * Bevestigen ("gezien"): melding weghalen en de afzender laten weten dat we het gezien hebben.
     * Doet niets als het bericht al bevestigd is.
     */
    suspend fun acknowledge(context: Context, alertId: String) {
        val entry = History.received(alertId) ?: return
        NotificationManagerCompat.from(context).cancel(notificationId(alertId))
        if (entry.status != History.Status.NIEUW) return
        History.setReceivedStatus(alertId, History.Status.GEZIEN)
        sendAck(context, alertId, entry.senderId)
    }

    /** Stuurt "gezien" naar de afzender van het bericht. */
    suspend fun sendAck(context: Context, alertId: String, originalSenderId: String) {
        val me = Settings(context).current()
        withTimeoutOrNull(20_000) {
            runCatching {
                FcmSender.send(
                    context,
                    FamilyMessage(
                        type = FamilyMessage.Type.ACK,
                        alertId = alertId,
                        senderId = me.senderId,
                        senderName = me.name,
                        originalSenderId = originalSenderId,
                    ),
                )
            }.onFailure { Log.w(TAG, "Bevestiging versturen mislukt", it) }
        }
    }

    private fun notificationId(alertId: String) = 1000 + (alertId.hashCode() and 0x7fffffff) % 100_000
}

/** Knop "OK, gezien" op een gewone of stille melding. */
class AckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra(AlertService.EXTRA_ALERT_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MessageNotifier.acknowledge(context.applicationContext, alertId)
            } finally {
                pending.finish()
            }
        }
    }
}
