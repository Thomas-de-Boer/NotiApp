package com.notiapp.alert

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.notiapp.R
import com.notiapp.data.Settings
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import com.notiapp.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Toont een binnengekomen gezinsbericht. Als er niet op gereageerd wordt, gaat de telefoon
 * na de ingestelde tijd bellen totdat iemand op "OK" drukt.
 *
 * Er is steeds maar één actief bericht: een nieuw bericht vervangt het vorige.
 */
class AlertService : Service() {

    /** Het bericht dat nu op deze telefoon openstaat. */
    data class ActiveAlert(val message: FamilyMessage, val ringing: Boolean, val isTest: Boolean)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private val ringer by lazy { Ringer(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ALERT -> {
                val message = FamilyMessage.fromData(intent.extrasToMap()) ?: return stop()
                val delaySec = intent.getIntExtra(EXTRA_DELAY, Settings.DEFAULT_RING_DELAY)
                showAlert(message, delaySec, intent.getBooleanExtra(EXTRA_TEST, false))
            }
            ACTION_ACK -> acknowledge(intent.getStringExtra(EXTRA_ALERT_ID))
            ACTION_CANCEL -> {
                if (_active.value?.message?.alertId == intent.getStringExtra(EXTRA_ALERT_ID)) finish()
                else if (_active.value == null) stop()
            }
            else -> if (_active.value == null) stop()
        }
        return START_NOT_STICKY
    }

    private fun showAlert(message: FamilyMessage, delaySec: Int, isTest: Boolean) {
        timerJob?.cancel()
        ringer.stop()
        _active.value = ActiveAlert(message, ringing = false, isTest = isTest)
        startInForeground(buildNotification(message, ringing = false))
        notificationManager().cancel(Notifications.ID_RING) // eventueel vorig belbericht

        timerJob = scope.launch {
            delay(delaySec * 1000L)
            escalate()
            delay(SAFETY_TIMEOUT_MS)
            Log.i(TAG, "Veiligheidslimiet bereikt, stoppen met bellen")
            finish()
        }
    }

    /** Niemand heeft gereageerd: gaan bellen. */
    private fun escalate() {
        val alert = _active.value ?: return
        _active.value = alert.copy(ringing = true)
        // Een nieuwe melding (ander id) zodat Android het volledige scherm zeker opent.
        startInForeground(buildNotification(alert.message, ringing = true), Notifications.ID_RING)
        notificationManager().cancel(Notifications.ID_ALERT)
        ringer.start()
    }

    private fun acknowledge(alertId: String?) {
        val alert = _active.value
        if (alert == null || (alertId != null && alertId != alert.message.alertId)) {
            if (alert == null) stop()
            return
        }
        finish(keepAlive = true)
        if (alert.isTest) {
            stop()
            return
        }
        // Laat de afzender weten dat we het gezien hebben.
        scope.launch {
            val me = Settings(applicationContext).current()
            withTimeoutOrNull(20_000) {
                runCatching {
                    FcmSender.send(
                        applicationContext,
                        FamilyMessage(
                            type = FamilyMessage.Type.ACK,
                            alertId = alert.message.alertId,
                            senderId = me.senderId,
                            senderName = me.name,
                            originalSenderId = alert.message.senderId,
                        ),
                    )
                }.onFailure { Log.w(TAG, "Bevestiging versturen mislukt", it) }
            }
            stop()
        }
    }

    /** Stopt met bellen en haalt de melding weg. */
    private fun finish(keepAlive: Boolean = false) {
        timerJob?.cancel()
        ringer.stop()
        _active.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager().cancel(Notifications.ID_ALERT)
        notificationManager().cancel(Notifications.ID_RING)
        if (!keepAlive) stopSelf()
    }

    private fun stop(): Int {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringer.stop()
        _active.value = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification, id: Int = Notifications.ID_ALERT) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(id, notification)
        }
    }

    private fun buildNotification(message: FamilyMessage, ringing: Boolean): Notification {
        // Op de melding tikken = app openen én bevestigen.
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_ALERT_ID, message.alertId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ackIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AlertService::class.java).setAction(ACTION_ACK).putExtra(EXTRA_ALERT_ID, message.alertId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, if (ringing) Notifications.CHANNEL_RINGING else Notifications.CHANNEL_MESSAGES)
        if (ringing) {
            val fullScreen = PendingIntent.getActivity(
                this, 3,
                Intent(this, RingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(fullScreen, true)
        }
        return builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.alert_title, message.senderName))
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(if (ringing) NotificationCompat.CATEGORY_CALL else NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .setDeleteIntent(ackIntent)
            .addAction(0, getString(R.string.alert_ok), ackIntent)
            .setOngoing(ringing)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .build()
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    private fun Intent.extrasToMap(): Map<String, String> =
        extras?.keySet()?.mapNotNull { key -> getStringExtra(key)?.let { key to it } }?.toMap().orEmpty()

    companion object {
        private const val TAG = "Gezinsbel"
        private const val ACTION_ALERT = "com.notiapp.ALERT"
        private const val ACTION_ACK = "com.notiapp.ACK"
        private const val ACTION_CANCEL = "com.notiapp.CANCEL"
        const val EXTRA_ALERT_ID = "alertId"
        private const val EXTRA_DELAY = "ringDelay"
        private const val EXTRA_TEST = "isTest"
        private const val SAFETY_TIMEOUT_MS = 10 * 60 * 1000L

        private val _active = MutableStateFlow<ActiveAlert?>(null)
        val active: StateFlow<ActiveAlert?> = _active

        fun startAlert(context: Context, message: FamilyMessage, ringDelaySeconds: Int, isTest: Boolean = false) {
            val intent = Intent(context, AlertService::class.java).setAction(ACTION_ALERT)
            message.toData().forEach { (k, v) -> intent.putExtra(k, v) }
            intent.putExtra(EXTRA_DELAY, ringDelaySeconds).putExtra(EXTRA_TEST, isTest)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Bevestigen: stopt het bellen en laat de afzender weten dat het gezien is. */
        fun acknowledge(context: Context, alertId: String?) {
            if (_active.value == null) return
            context.startService(Intent(context, AlertService::class.java).setAction(ACTION_ACK).putExtra(EXTRA_ALERT_ID, alertId))
        }

        fun cancel(context: Context, alertId: String) {
            if (_active.value?.message?.alertId != alertId) return
            context.startService(Intent(context, AlertService::class.java).setAction(ACTION_CANCEL).putExtra(EXTRA_ALERT_ID, alertId))
        }
    }
}
