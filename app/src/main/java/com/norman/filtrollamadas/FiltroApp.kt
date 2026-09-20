package com.norman.filtrollamadas

import android.app.Application
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.notify.Notifier
import kotlinx.coroutines.launch

class FiltroApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannel(this)
        container.appScope.launch { runCatching { container.purgeOldEntries() } }
    }
}
