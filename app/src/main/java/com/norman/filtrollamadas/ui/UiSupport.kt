package com.norman.filtrollamadas.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.norman.filtrollamadas.FiltroApp
import com.norman.filtrollamadas.data.AppContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- ViewModels con acceso al contenedor ----------

@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as FiltroApp
    return viewModel(factory = viewModelFactory { initializer { create(app.container) } })
}

// ---------- Estado de permisos y rol ----------

data class SetupStatus(
    val role: Boolean,
    val contacts: Boolean,
    val callLog: Boolean,
    val callPhone: Boolean,
    val phoneState: Boolean,
    val notifications: Boolean,
) {
    /** Mínimo para poder filtrar. */
    val required get() = role && contacts
    val complete get() = required && callLog && callPhone && phoneState && notifications
}

fun readSetupStatus(ctx: Context): SetupStatus {
    fun g(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    val rm = ctx.getSystemService(RoleManager::class.java)
    return SetupStatus(
        role = rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true,
        contacts = g(Manifest.permission.READ_CONTACTS),
        callLog = g(Manifest.permission.READ_CALL_LOG),
        callPhone = g(Manifest.permission.CALL_PHONE),
        phoneState = g(Manifest.permission.READ_PHONE_STATE),
        notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || g(Manifest.permission.POST_NOTIFICATIONS),
    )
}

/** Se recalcula cada vez que la pantalla vuelve al frente (p. ej. tras un diálogo de permisos). */
@Composable
fun rememberSetupStatus(): SetupStatus {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    return remember(tick) { readSetupStatus(ctx) }
}

fun roleRequestIntent(ctx: Context): Intent? =
    ctx.getSystemService(RoleManager::class.java)?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)

// ---------- Formatos de fecha ----------

private val es: Locale = Locale.forLanguageTag("es-CO")

fun localDate(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

fun dayLabel(d: LocalDate): String {
    val today = LocalDate.now()
    return when (d) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> d.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)).replaceFirstChar { it.titlecase(es) }
    }
}

fun timeLabel(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", es))

fun dateTimeLabel(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", es))
