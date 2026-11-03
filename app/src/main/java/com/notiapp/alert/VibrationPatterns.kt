package com.notiapp.alert

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import com.notiapp.R

/**
 * Trilpatronen. [timings] wisselt af tussen stil en trillen (in ms), te beginnen met stil.
 * Het hele patroon wordt herhaald zolang de telefoon belt.
 */
enum class VibrationPattern(@StringRes val label: Int, val timings: LongArray) {
    STANDAARD(R.string.vib_standard, longArrayOf(0, 800, 600)),
    SNEL_DUBBEL(R.string.vib_double, longArrayOf(0, 120, 90, 120, 1100)),
    HARTSLAG(R.string.vib_heartbeat, longArrayOf(0, 100, 120, 250, 900)),
    LANG_RUSTIG(R.string.vib_long, longArrayOf(0, 1500, 1500)),
    SOS(R.string.vib_sos, longArrayOf(0, 150, 100, 150, 100, 150, 300, 450, 100, 450, 100, 450, 300, 150, 100, 150, 100, 150, 1200));

    companion object {
        fun fromName(name: String?): VibrationPattern = entries.firstOrNull { it.name == name } ?: STANDAARD
    }
}

object Vibrations {
    /** Trilt ook als de telefoon op stil staat (wekker-categorie). */
    val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()

    fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    /** Herhalend trillen tot [Vibrator.cancel]. */
    fun start(context: Context, pattern: VibrationPattern) {
        vibrator(context).vibrate(VibrationEffect.createWaveform(pattern.timings, 0), ATTRIBUTES)
    }

    /** Het patroon twee keer laten voelen, als voorbeeld in de instellingen. */
    fun preview(context: Context, pattern: VibrationPattern) {
        val twice = pattern.timings + pattern.timings.copyOfRange(1, pattern.timings.size)
        vibrator(context).vibrate(VibrationEffect.createWaveform(twice, -1), ATTRIBUTES)
    }
}
