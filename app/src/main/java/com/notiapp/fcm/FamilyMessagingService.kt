package com.notiapp.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.notiapp.alert.AlertService
import com.notiapp.alert.SeenNotifier
import com.notiapp.data.History
import com.notiapp.data.Members
import com.notiapp.data.Settings
import kotlinx.coroutines.runBlocking

/** Ontvangt berichten van het gezinskanaal en berichten die rechtstreeks naar deze telefoon gestuurd zijn. */
class FamilyMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remote: RemoteMessage) {
        val message = FamilyMessage.fromData(remote.data) ?: return
        Log.i(TAG, "Bericht ontvangen: ${message.type} van ${message.senderName}")
        val me = runBlocking { Settings(applicationContext).current() }
        if (me.name.isBlank()) return // app nog niet ingesteld
        if (message.senderId == me.senderId) return // eigen bericht
        val members = Members(applicationContext)

        when (message.type) {
            FamilyMessage.Type.ALERT -> {
                runBlocking { members.touch(message.senderId, message.senderName) }
                val ageMs = System.currentTimeMillis() - message.sentAt
                if (ageMs > MAX_AGE_MS) {
                    Log.i(TAG, "Oud bericht genegeerd (${ageMs / 1000}s)")
                    return
                }
                AlertService.startAlert(applicationContext, message, Settings.RING_DELAY_SECONDS)
            }
            FamilyMessage.Type.ACK -> {
                runBlocking { members.touch(message.senderId, message.senderName) }
                if (message.originalSenderId == me.senderId) {
                    History.markSeenBy(message.alertId, message.senderName)?.let { SeenNotifier.show(applicationContext, it) }
                }
            }
            FamilyMessage.Type.CANCEL -> AlertService.cancel(applicationContext, message.alertId)
            FamilyMessage.Type.HELLO -> runBlocking {
                val isNew = members.upsert(message.senderId, message.senderName, message.token)
                // Een nieuwe telefoon: laat weten dat wij er ook zijn.
                if (isNew) members.replyTo(message.token)
            }
            FamilyMessage.Type.HELLO_REPLY -> runBlocking {
                members.upsert(message.senderId, message.senderName, message.token)
            }
            FamilyMessage.Type.PING -> Unit // alleen een controle of de app er nog is
        }
    }

    /** Ons FCM-adres is veranderd: opnieuw aanmelden zodat de anderen het nieuwe adres krijgen. */
    override fun onNewToken(token: String) {
        runBlocking { Members(applicationContext).announce(force = true) }
    }

    companion object {
        private const val TAG = "NotiApp"
        private const val MAX_AGE_MS = 15 * 60 * 1000L
    }
}
