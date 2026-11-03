package com.notiapp

import android.app.Application
import com.notiapp.alert.Notifications
import com.notiapp.data.History

class NotiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        History.init(this)
    }
}
