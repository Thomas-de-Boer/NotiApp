package com.notiapp.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.notiapp.R

/** Meldingskanalen van de app. */
object Notifications {
    /** Een nieuw gezinsbericht (maakt meteen geluid). */
    const val CHANNEL_MESSAGES = "berichten"
    /** Bellen omdat er niet gereageerd is (geluid speelt de app zelf af). */
    const val CHANNEL_RINGING = "bellen"
    /** Rustige meldingen, zoals "Mama heeft je bericht gezien". */
    const val CHANNEL_STATUS = "status"
    /** Stille berichten: geen geluid, alleen tussen de meldingen. */
    const val CHANNEL_SILENT = "stil"

    const val ID_ALERT = 1
    const val ID_SEEN = 2
    const val ID_RING = 3

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MESSAGES, context.getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_messages_desc)
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RINGING, context.getString(R.string.channel_ringing), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_ringing_desc)
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_status_desc)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, context.getString(R.string.channel_silent), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_silent_desc)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }
}
