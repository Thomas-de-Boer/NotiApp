package com.notiapp.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.util.Log

/** Laat de telefoon luid bellen en trillen, ook als hij op stil staat (via het wekker-volume). */
class Ringer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    fun start() {
        if (player != null) return
        wakeLock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Gezinsbel:bellen")
            .apply { acquire(11 * 60 * 1000L) }

        val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("Gezinsbel", "Beltoon afspelen mislukt", e)
            null
        }

        val pattern = longArrayOf(0, 800, 600)
        vibrator.vibrate(
            VibrationEffect.createWaveform(pattern, 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
    }

    fun stop() {
        player?.runCatching { stop(); release() }
        player = null
        vibrator.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }
}
