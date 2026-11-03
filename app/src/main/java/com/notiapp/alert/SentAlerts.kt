package com.notiapp.alert

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notiapp.R
import com.notiapp.fcm.FamilyMessage
import com.notiapp.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Houdt bij wie het laatst verstuurde bericht van deze telefoon heeft gezien. */
object SentAlerts {

    data class SentAlert(val alertId: String, val text: String, val seenBy: List<String> = emptyList())

    private val _last = MutableStateFlow<SentAlert?>(null)
    val last: StateFlow<SentAlert?> = _last

    fun onSent(message: FamilyMessage) {
        _last.value = SentAlert(message.alertId, message.text)
    }

    fun clear() {
        _last.value = null
    }

    /** Iemand heeft ons bericht gezien. */
    fun onSeen(context: Context, ack: FamilyMessage) {
        _last.update { sent ->
            if (sent?.alertId == ack.alertId && ack.senderName !in sent.seenBy) sent.copy(seenBy = sent.seenBy + ack.senderName)
            else sent
        }
        showSeenNotification(context, ack.senderName)
    }

    private fun showSeenNotification(context: Context, name: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val names = _last.value?.seenBy?.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: name
        val open = PendingIntent.getActivity(
            context, 2, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.seen_title))
            .setContentText(context.getString(R.string.seen_by, names))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        NotificationManagerCompat.from(context).notify(Notifications.ID_SEEN, notification)
    }
}
