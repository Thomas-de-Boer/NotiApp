package com.notiapp

import android.app.Application
import com.notiapp.alert.Notifications

class NotiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
