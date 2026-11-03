package com.notiapp.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.notiapp.alert.AlertService
import com.notiapp.alert.SeenNotifier
import com.notiapp.data.History
import com.notiapp.data.Settings
import kotlinx.coroutines.runBlocking

/** Ontvangt berichten van het gezinskanaal. */
class FamilyMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remote: RemoteMessage) {
        val message = FamilyMessage.fromData(remote.data) ?: return
        Log.i(TAG, "Bericht ontvangen: ${message.type} van ${message.senderName}")
        val me = runBlocking { Settings(applicationContext).current() }
        if (me.name.isBlank()) return // app nog niet ingesteld
        if (message.senderId == me.senderId) return // eigen bericht

        when (message.type) {
            FamilyMessage.Type.ALERT -> {
                val ageMs = System.currentTimeMillis() - message.sentAt
                if (ageMs > MAX_AGE_MS) {
                    Log.i(TAG, "Oud bericht genegeerd (${ageMs / 1000}s)")
                    return
                }
                AlertService.startAlert(applicationContext, message, Settings.RING_DELAY_SECONDS)
            }
            FamilyMessage.Type.ACK ->
                if (message.originalSenderId == me.senderId) {
                    History.markSeenBy(message.alertId, message.senderName)?.let { SeenNotifier.show(applicationContext, it) }
                }
            FamilyMessage.Type.CANCEL -> AlertService.cancel(applicationContext, message.alertId)
            else -> Unit
        }
    }

    override fun onNewToken(token: String) {
        // We gebruiken een gezinskanaal (topic), dus het token zelf hoeven we nergens op te slaan.
    }

    companion object {
        private const val TAG = "NotiApp"
        private const val MAX_AGE_MS = 15 * 60 * 1000L
    }
}
