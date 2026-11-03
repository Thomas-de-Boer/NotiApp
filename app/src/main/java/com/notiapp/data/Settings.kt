package com.notiapp.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notiapp.alert.VibrationPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "instellingen")

/** Alle instellingen van deze telefoon. */
data class AppSettings(
    val name: String,
    val senderId: String,
    val presets: List<String>,
    /** Eigen beltoon, of null = standaard beltoon van de telefoon. */
    val ringtoneUri: Uri?,
    val vibrationPattern: VibrationPattern,
)

class Settings(private val context: Context) {

    companion object {
        /** Alle telefoons in het gezin luisteren naar dit kanaal. */
        const val TOPIC = "gezin"

        val DEFAULT_PRESETS = listOf(
            "Het eten is klaar 🍽️",
            "We vertrekken naar …",
            "Kom even naar beneden",
            "Bel me even 📞",
            "Ik ben thuis 🏠",
        )
        /** Zo lang (seconden) wacht een telefoon na een bericht voordat hij gaat bellen. */
        const val RING_DELAY_SECONDS = 30

        /** Een bericht met dit teken vraagt om een aanvulling, bijv. "We vertrekken naar …". */
        const val FILL_IN = "…"

        private val KEY_NAME = stringPreferencesKey("naam")
        private val KEY_SENDER_ID = stringPreferencesKey("afzender_id")
        private val KEY_PRESETS = stringPreferencesKey("berichten")
        private val KEY_RINGTONE = stringPreferencesKey("beltoon")
        private val KEY_VIBRATION = stringPreferencesKey("trilpatroon")
        private const val SEPARATOR = "\u001F"
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            name = p[KEY_NAME] ?: "",
            senderId = p[KEY_SENDER_ID] ?: "",
            presets = p[KEY_PRESETS]?.split(SEPARATOR)?.filter { it.isNotBlank() } ?: DEFAULT_PRESETS,
            ringtoneUri = p[KEY_RINGTONE]?.let(Uri::parse),
            vibrationPattern = VibrationPattern.fromName(p[KEY_VIBRATION]),
        )
    }

    suspend fun current(): AppSettings = ensureSenderId().let { settings.first() }

    /** Maakt bij de eerste start een unieke id voor deze telefoon. */
    private suspend fun ensureSenderId() {
        context.dataStore.edit { p ->
            if (p[KEY_SENDER_ID].isNullOrEmpty()) p[KEY_SENDER_ID] = UUID.randomUUID().toString()
        }
    }

    suspend fun setName(name: String) {
        ensureSenderId()
        context.dataStore.edit { it[KEY_NAME] = name.trim() }
    }

    suspend fun setPresets(presets: List<String>) {
        context.dataStore.edit { it[KEY_PRESETS] = presets.map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.joinToString(SEPARATOR) }
    }

    suspend fun setRingtone(uri: Uri?) {
        context.dataStore.edit { if (uri == null) it.remove(KEY_RINGTONE) else it[KEY_RINGTONE] = uri.toString() }
    }

    suspend fun setVibrationPattern(pattern: VibrationPattern) {
        context.dataStore.edit { it[KEY_VIBRATION] = pattern.name }
    }
}
