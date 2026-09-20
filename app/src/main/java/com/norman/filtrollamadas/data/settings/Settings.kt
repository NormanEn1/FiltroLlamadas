package com.norman.filtrollamadas.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { SISTEMA, CLARO, OSCURO }

object ForwardingState {
    const val NINGUNO = ""
    const val ACTIVADO = "ACTIVADO"
    const val DESACTIVADO = "DESACTIVADO"
}

data class AppSettings(
    val enabled: Boolean = false,
    // Destino de la redirección
    val destinationNumber: String = "",
    val destinationLabel: String = "iPhone",
    val useCountryPrefix: Boolean = false,
    // Línea protegida (vacío = todas)
    val lineId: String = "",
    val lineComponent: String = "",
    val lineLabel: String = "",
    // Reglas
    val divertHidden: Boolean = true,
    val divertUnknown: Boolean = true,
    val recentDays: Int = 30,
    val allowRepeat: Boolean = true,
    val repeatMinutes: Int = 3,
    // Notificaciones y registro
    val notifyOnDivert: Boolean = true,
    val retentionDays: Int = 90,
    // Apariencia
    val themeMode: ThemeMode = ThemeMode.SISTEMA,
    val dynamicColor: Boolean = false,
    // Última acción de desvío marcada en el operador
    val forwardingState: String = ForwardingState.NINGUNO,
    val forwardingTarget: String = "",
    val forwardingAt: Long = 0L,
) {
    /** Número tal como se marca en el código del operador. */
    val destinationDialable: String
        get() {
            val d = destinationNumber.filter(Char::isDigit)
            return if (useCountryPrefix && d.length == 10) "57$d" else d
        }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

class SettingsRepository(private val store: DataStore<Preferences>) {

    private object K {
        val enabled = booleanPreferencesKey("enabled")
        val destinationNumber = stringPreferencesKey("destination_number")
        val destinationLabel = stringPreferencesKey("destination_label")
        val useCountryPrefix = booleanPreferencesKey("use_country_prefix")
        val lineId = stringPreferencesKey("line_id")
        val lineComponent = stringPreferencesKey("line_component")
        val lineLabel = stringPreferencesKey("line_label")
        val divertHidden = booleanPreferencesKey("divert_hidden")
        val divertUnknown = booleanPreferencesKey("divert_unknown")
        val recentDays = intPreferencesKey("recent_days")
        val allowRepeat = booleanPreferencesKey("allow_repeat")
        val repeatMinutes = intPreferencesKey("repeat_minutes")
        val notifyOnDivert = booleanPreferencesKey("notify_on_divert")
        val retentionDays = intPreferencesKey("retention_days")
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val forwardingState = stringPreferencesKey("forwarding_state")
        val forwardingTarget = stringPreferencesKey("forwarding_target")
        val forwardingAt = longPreferencesKey("forwarding_at")
    }

    val flow: Flow<AppSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { read(it) }

    suspend fun current(): AppSettings = flow.first()

    suspend fun update(block: (AppSettings) -> AppSettings) {
        store.edit { p -> write(p, block(read(p))) }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            enabled = p[K.enabled] ?: d.enabled,
            destinationNumber = p[K.destinationNumber] ?: d.destinationNumber,
            destinationLabel = p[K.destinationLabel] ?: d.destinationLabel,
            useCountryPrefix = p[K.useCountryPrefix] ?: d.useCountryPrefix,
            lineId = p[K.lineId] ?: d.lineId,
            lineComponent = p[K.lineComponent] ?: d.lineComponent,
            lineLabel = p[K.lineLabel] ?: d.lineLabel,
            divertHidden = p[K.divertHidden] ?: d.divertHidden,
            divertUnknown = p[K.divertUnknown] ?: d.divertUnknown,
            recentDays = p[K.recentDays] ?: d.recentDays,
            allowRepeat = p[K.allowRepeat] ?: d.allowRepeat,
            repeatMinutes = p[K.repeatMinutes] ?: d.repeatMinutes,
            notifyOnDivert = p[K.notifyOnDivert] ?: d.notifyOnDivert,
            retentionDays = p[K.retentionDays] ?: d.retentionDays,
            themeMode = p[K.themeMode]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: d.themeMode,
            dynamicColor = p[K.dynamicColor] ?: d.dynamicColor,
            forwardingState = p[K.forwardingState] ?: d.forwardingState,
            forwardingTarget = p[K.forwardingTarget] ?: d.forwardingTarget,
            forwardingAt = p[K.forwardingAt] ?: d.forwardingAt,
        )
    }

    private fun write(p: MutablePreferences, s: AppSettings) {
        p[K.enabled] = s.enabled
        p[K.destinationNumber] = s.destinationNumber
        p[K.destinationLabel] = s.destinationLabel
        p[K.useCountryPrefix] = s.useCountryPrefix
        p[K.lineId] = s.lineId
        p[K.lineComponent] = s.lineComponent
        p[K.lineLabel] = s.lineLabel
        p[K.divertHidden] = s.divertHidden
        p[K.divertUnknown] = s.divertUnknown
        p[K.recentDays] = s.recentDays
        p[K.allowRepeat] = s.allowRepeat
        p[K.repeatMinutes] = s.repeatMinutes
        p[K.notifyOnDivert] = s.notifyOnDivert
        p[K.retentionDays] = s.retentionDays
        p[K.themeMode] = s.themeMode.name
        p[K.dynamicColor] = s.dynamicColor
        p[K.forwardingState] = s.forwardingState
        p[K.forwardingTarget] = s.forwardingTarget
        p[K.forwardingAt] = s.forwardingAt
    }
}
