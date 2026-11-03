package com.notiapp

import android.app.Application
import com.notiapp.alert.Notifications

class GezinsbelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
