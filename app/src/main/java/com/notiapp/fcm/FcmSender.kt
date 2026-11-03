package com.notiapp.fcm

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.ServiceAccountCredentials
import com.notiapp.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verstuurt berichten naar het gezinskanaal via de FCM HTTP v1 API.
 * Gebruikt de serviceaccount-sleutel in assets/fcm_service_account.json (zie README).
 */
object FcmSender {
    private const val TAG = "NotiApp"
    private const val KEY_FILE = "fcm_service_account.json"
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

    open class SendException(message: String) : Exception(message)

    /** De telefoon heeft de app niet meer (verwijderd). */
    class UnregisteredException : SendException("Deze telefoon heeft de app niet meer.")

    private var credentials: ServiceAccountCredentials? = null
    private val lock = Mutex()

    private suspend fun credentials(context: Context): ServiceAccountCredentials = lock.withLock {
        credentials ?: try {
            context.assets.open(KEY_FILE).use { stream ->
                (ServiceAccountCredentials.fromStream(stream).createScoped(SCOPE) as ServiceAccountCredentials)
                    .also { credentials = it }
            }
        } catch (e: FileNotFoundException) {
            throw SendException("De verzendsleutel ontbreekt. Zie de README (stap 2).")
        }
    }

    /** Verstuurt het bericht naar het hele gezin. Gooit [SendException] met een Nederlandse foutmelding als het mislukt. */
    suspend fun send(context: Context, message: FamilyMessage) = post(context, message, topic = Settings.TOPIC, token = null)

    /**
     * Verstuurt het bericht rechtstreeks naar één telefoon.
     * Gooit [UnregisteredException] als die telefoon de app niet meer heeft.
     */
    suspend fun sendToToken(context: Context, message: FamilyMessage, token: String) =
        post(context, message, topic = null, token = token)

    private suspend fun post(context: Context, message: FamilyMessage, topic: String?, token: String?) = withContext(Dispatchers.IO) {
        val creds = credentials(context)
        val accessToken = try {
            creds.refreshIfExpired()
            creds.accessToken?.tokenValue ?: error("leeg token")
        } catch (e: Exception) {
            Log.e(TAG, "Kon geen toegangstoken krijgen", e)
            throw SendException("Geen verbinding met Google. Controleer je internet.")
        }

        val ttl = when (message.type) {
            FamilyMessage.Type.ALERT -> "900s"
            FamilyMessage.Type.PING -> "60s"
            else -> "300s"
        }
        // Stille controleberichten hoeven de telefoon niet meteen wakker te maken.
        val priority = if (message.type == FamilyMessage.Type.PING) "NORMAL" else "HIGH"
        val target = JSONObject()
        if (token != null) target.put("token", token) else target.put("topic", topic)
        val body = JSONObject()
            .put("message", target
                .put("data", JSONObject(message.toData()))
                .put("android", JSONObject()
                    .put("priority", priority)
                    .put("ttl", ttl)))

        val conn = URL("https://fcm.googleapis.com/v1/projects/${creds.projectId}/messages:send")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.readText().orEmpty()
                Log.e(TAG, "FCM fout $code: $error")
                // Een telefoon die de app verwijderd heeft: FCM meldt UNREGISTERED (404),
                // of een ongeldig adres (400 INVALID_ARGUMENT).
                if (token != null && (code == 404 || error.contains("UNREGISTERED") ||
                        (code == 400 && error.contains("registration token")))
                ) {
                    throw UnregisteredException()
                }
                throw SendException("Versturen mislukt (code $code).")
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Netwerkfout", e)
            throw SendException("Geen internetverbinding.")
        } finally {
            conn.disconnect()
        }
    }
}
