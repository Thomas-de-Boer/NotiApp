package com.notiapp.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Geschiedenis van verstuurde en ontvangen berichten, bewaard op deze telefoon
 * (bestand `geschiedenis.json`, maximaal [MAX_ENTRIES] berichten, nieuwste eerst).
 */
object History {

    enum class Direction { VERSTUURD, ONTVANGEN }

    /** Alleen voor ontvangen berichten. */
    enum class Status { NIEUW, GEZIEN, GEMIST, GEANNULEERD }

    data class Entry(
        val alertId: String,
        val direction: Direction,
        /** Naam van de afzender. */
        val from: String,
        /** Namen van de ontvangers; leeg = iedereen. */
        val to: List<String>,
        val text: String,
        val time: Long,
        /** Bij verstuurde berichten: wie het gezien heeft. */
        val seenBy: List<String> = emptyList(),
        val status: Status = Status.NIEUW,
    )

    private const val MAX_ENTRIES = 200
    private const val FILE_NAME = "geschiedenis.json"

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private var file: File? = null

    /** Eén keer aanroepen bij het starten van de app. */
    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, FILE_NAME)
        file = f
        _entries.value = runCatching { if (f.exists()) parse(f.readText()) else emptyList() }
            .onFailure { Log.e("NotiApp", "Geschiedenis lezen mislukt", it) }
            .getOrDefault(emptyList())
    }

    fun addSent(alertId: String, from: String, to: List<String>, text: String) =
        update { listOf(Entry(alertId, Direction.VERSTUURD, from, to, text, System.currentTimeMillis())) + it }

    fun addReceived(alertId: String, from: String, to: List<String>, text: String, time: Long) =
        update { list ->
            if (list.any { it.alertId == alertId && it.direction == Direction.ONTVANGEN }) list
            else listOf(Entry(alertId, Direction.ONTVANGEN, from, to, text, time)) + list
        }

    /** Iemand heeft ons verstuurde bericht gezien. Geeft de bijgewerkte regel terug. */
    fun markSeenBy(alertId: String, name: String): Entry? {
        update { list ->
            list.map {
                if (it.alertId == alertId && it.direction == Direction.VERSTUURD && name !in it.seenBy) {
                    it.copy(seenBy = it.seenBy + name)
                } else it
            }
        }
        return lastSent(alertId)
    }

    /** Status van een ontvangen bericht bijwerken (alleen zolang het nog "nieuw" is). */
    fun setReceivedStatus(alertId: String, status: Status) = update { list ->
        list.map {
            if (it.alertId == alertId && it.direction == Direction.ONTVANGEN && it.status == Status.NIEUW) {
                it.copy(status = status)
            } else it
        }
    }

    fun lastSent(alertId: String? = null): Entry? =
        _entries.value.firstOrNull { it.direction == Direction.VERSTUURD && (alertId == null || it.alertId == alertId) }

    @Synchronized
    private fun update(transform: (List<Entry>) -> List<Entry>) {
        val new = transform(_entries.value).take(MAX_ENTRIES)
        if (new == _entries.value) return
        _entries.value = new
        runCatching { file?.writeText(serialize(new)) }.onFailure { Log.e("NotiApp", "Geschiedenis opslaan mislukt", it) }
    }

    private fun serialize(list: List<Entry>): String = JSONArray().apply {
        list.forEach { e ->
            put(
                JSONObject()
                    .put("alertId", e.alertId)
                    .put("direction", e.direction.name)
                    .put("from", e.from)
                    .put("to", JSONArray(e.to))
                    .put("text", e.text)
                    .put("time", e.time)
                    .put("seenBy", JSONArray(e.seenBy))
                    .put("status", e.status.name)
            )
        }
    }.toString()

    private fun parse(json: String): List<Entry> {
        val array = JSONArray(json)
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Entry(
                    alertId = o.getString("alertId"),
                    direction = Direction.valueOf(o.getString("direction")),
                    from = o.optString("from"),
                    to = o.optJSONArray("to").toStringList(),
                    text = o.optString("text"),
                    time = o.optLong("time"),
                    seenBy = o.optJSONArray("seenBy").toStringList(),
                    status = runCatching { Status.valueOf(o.optString("status")) }.getOrDefault(Status.NIEUW),
                )
            }.getOrNull()
        }
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}
