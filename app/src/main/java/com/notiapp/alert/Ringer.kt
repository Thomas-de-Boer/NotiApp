package com.notiapp.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.util.Log

/**
 * Laat de telefoon bellen zoals een inkomend gesprek:
 * - normaal: beltoon + trillen
 * - op trillen of stil: alleen trillen
 */
class Ringer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrating = false

    /** @param ringtone eigen beltoon, of null voor de standaard beltoon van de telefoon. */
    fun start(ringtone: Uri?, pattern: VibrationPattern) {
        if (vibrating) return
        wakeLock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotiApp:bellen")
            .apply { acquire(11 * 60 * 1000L) }

        Vibrations.start(context, pattern)
        vibrating = true

        val ringerMode = context.getSystemService(AudioManager::class.java).ringerMode
        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            Log.i(TAG, "Telefoon staat op stil/trillen: alleen trillen")
            return
        }
        val uri = ringtone
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Beltoon afspelen mislukt", e)
            null
        }
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        if (vibrating) Vibrations.vibrator(context).cancel()
        vibrating = false
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private companion object {
        const val TAG = "NotiApp"
    }
}
