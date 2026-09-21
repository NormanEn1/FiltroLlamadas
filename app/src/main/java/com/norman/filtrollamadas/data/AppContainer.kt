package com.norman.filtrollamadas.data

import android.content.Context
import androidx.room.Room
import com.norman.filtrollamadas.data.db.AppDatabase
import com.norman.filtrollamadas.data.db.MIGRATION_1_2
import com.norman.filtrollamadas.data.settings.SettingsRepository
import com.norman.filtrollamadas.data.settings.dataStore
import com.norman.filtrollamadas.domain.ScreeningEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Dependencias compartidas de la app (inyección manual). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "filtro.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    val settings = SettingsRepository(appContext.dataStore)

    val engine = ScreeningEngine(appContext, settings, database.listDao(), database.callDao())

    suspend fun purgeOldEntries() {
        val days = settings.current().retentionDays
        database.callDao().deleteOlderThan(System.currentTimeMillis() - days * 86_400_000L)
    }
}
