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

    class SendException(message: String) : Exception(message)

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

    /** Verstuurt het bericht. Gooit [SendException] met een Nederlandse foutmelding als het mislukt. */
    suspend fun send(context: Context, message: FamilyMessage) = withContext(Dispatchers.IO) {
        val creds = credentials(context)
        val token = try {
            creds.refreshIfExpired()
            creds.accessToken?.tokenValue ?: error("leeg token")
        } catch (e: Exception) {
            Log.e(TAG, "Kon geen toegangstoken krijgen", e)
            throw SendException("Geen verbinding met Google. Controleer je internet.")
        }

        val ttl = if (message.type == FamilyMessage.Type.ALERT) "900s" else "300s"
        val body = JSONObject()
            .put("message", JSONObject()
                .put("topic", Settings.TOPIC)
                .put("data", JSONObject(message.toData()))
                .put("android", JSONObject()
                    .put("priority", "HIGH")
                    .put("ttl", ttl)))

        val conn = URL("https://fcm.googleapis.com/v1/projects/${creds.projectId}/messages:send")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = conn.errorStream?.bufferedReader()?.readText()
                Log.e(TAG, "FCM fout $code: $error")
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
