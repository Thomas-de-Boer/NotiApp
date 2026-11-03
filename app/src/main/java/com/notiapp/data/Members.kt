package com.notiapp.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.membersStore by preferencesDataStore(name = "leden")

/** Een ander gezinslid dat de app heeft. */
data class Member(val id: String, val name: String, val token: String, val lastSeen: Long)

/**
 * Wie in het gezin de app heeft. Zonder server: elke telefoon houdt zelf de lijst bij.
 * - Aanmelden: een telefoon stuurt HELLO (met zijn FCM-adres) naar het gezinskanaal;
 *   telefoons die hem nog niet kenden antwoorden rechtstreeks met HELLO_REPLY.
 * - Afmelden: na het verwijderen van de app meldt FCM het adres als ongeldig;
 *   [checkMembers] stuurt stille PING-berichten en haalt zulke leden van de lijst.
 */
class Members(private val context: Context) {

    companion object {
        private const val TAG = "NotiApp"
        private val KEY_MEMBERS = stringPreferencesKey("leden")
        private val KEY_LAST_ANNOUNCE = longPreferencesKey("laatst_aangemeld")
        private val KEY_LAST_CHECK = longPreferencesKey("laatst_gecontroleerd")
        private const val INTERVAL_MS = 6 * 60 * 60 * 1000L
    }

    val members: Flow<List<Member>> = context.membersStore.data.map { p -> parse(p[KEY_MEMBERS]) }

    suspend fun current(): List<Member> = members.first()

    /** Lid toevoegen of bijwerken. Geeft true terug als het een nieuw lid is. */
    suspend fun upsert(id: String, name: String, token: String?): Boolean {
        var isNew = false
        context.membersStore.edit { p ->
            val list = parse(p[KEY_MEMBERS])
            val old = list.firstOrNull { it.id == id }
            isNew = old == null
            if (old == null && token.isNullOrEmpty()) return@edit // zonder adres kunnen we niets
            val updated = Member(
                id = id,
                name = name.ifBlank { old?.name.orEmpty() },
                token = token?.takeIf { it.isNotEmpty() } ?: old!!.token,
                lastSeen = System.currentTimeMillis(),
            )
            // Zelfde adres onder een oude id (bijv. na opnieuw instellen): de oude weghalen.
            p[KEY_MEMBERS] = serialize(list.filter { it.id != id && it.token != updated.token } + updated)
        }
        return isNew && !token.isNullOrEmpty()
    }

    /** Werkt "laatst gezien" en de naam bij voor een bekend lid. */
    suspend fun touch(id: String, name: String) {
        if (current().any { it.id == id }) upsert(id, name, null)
    }

    suspend fun remove(id: String) {
        context.membersStore.edit { p -> p[KEY_MEMBERS] = serialize(parse(p[KEY_MEMBERS]).filter { it.id != id }) }
    }

    /** Meldt deze telefoon aan bij het gezin (HELLO). [force] = ook als het kort geleden al gebeurd is. */
    suspend fun announce(force: Boolean = false) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val me = Settings(context).current()
        if (me.name.isBlank()) return
        val last = context.membersStore.data.first()[KEY_LAST_ANNOUNCE] ?: 0
        if (!force && System.currentTimeMillis() - last < INTERVAL_MS) return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            FcmSender.send(context, hello(FamilyMessage.Type.HELLO, me, token))
            context.membersStore.edit { it[KEY_LAST_ANNOUNCE] = System.currentTimeMillis() }
            Log.i(TAG, "Aangemeld bij het gezin als ${me.name}")
        }.onFailure { Log.w(TAG, "Aanmelden bij het gezin mislukt", it) }
    }

    /** Antwoordt een nieuwe telefoon rechtstreeks met onze eigen gegevens. */
    suspend fun replyTo(newMemberToken: String) {
        val me = Settings(context).current()
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            FcmSender.sendToToken(context, hello(FamilyMessage.Type.HELLO_REPLY, me, token), newMemberToken)
        }.onFailure { Log.w(TAG, "Antwoord op aanmelding mislukt", it) }
    }

    /**
     * Controleert of alle leden de app nog hebben (stille PING). Leden waarvan FCM zegt
     * dat de app verwijderd is, worden van de lijst gehaald. [force] = ook als het kort geleden al gebeurd is.
     */
    suspend fun checkMembers(force: Boolean = false) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val last = context.membersStore.data.first()[KEY_LAST_CHECK] ?: 0
        if (!force && System.currentTimeMillis() - last < INTERVAL_MS) return
        val me = Settings(context).current()
        current().forEach { member ->
            try {
                FcmSender.sendToToken(
                    context,
                    FamilyMessage(FamilyMessage.Type.PING, UUID.randomUUID().toString(), me.senderId, me.name),
                    member.token,
                )
            } catch (e: FcmSender.UnregisteredException) {
                Log.i(TAG, "${member.name} heeft de app niet meer: van de lijst gehaald")
                remove(member.id)
            } catch (e: FcmSender.SendException) {
                Log.w(TAG, "Controle van ${member.name} mislukt: ${e.message}")
            }
        }
        context.membersStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
    }

    private fun hello(type: FamilyMessage.Type, me: AppSettings, token: String) =
        FamilyMessage(type, UUID.randomUUID().toString(), me.senderId, me.name, token = token)

    private fun parse(json: String?): List<Member> = runCatching {
        val array = JSONArray(json ?: return emptyList())
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Member(o.getString("id"), o.optString("name"), o.getString("token"), o.optLong("lastSeen"))
        }
    }.getOrDefault(emptyList())

    private fun serialize(list: List<Member>): String = JSONArray().apply {
        list.forEach {
            put(JSONObject().put("id", it.id).put("name", it.name).put("token", it.token).put("lastSeen", it.lastSeen))
        }
    }.toString()
}
