package com.norman.filtrollamadas.telephony

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.norman.filtrollamadas.data.settings.AppSettings

data class PhoneLine(val id: String, val component: String, val label: String)

/** Líneas (SIM) del teléfono y códigos MMI de desvío condicional por "ocupado". */
object Telefonia {

    const val CODE_QUERY = "*#67#"
    const val CODE_DEACTIVATE = "##67#"
    fun codeActivate(number: String) = "**67*$number#"

    private fun granted(ctx: Context, p: String) =
        ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun lines(ctx: Context): List<PhoneLine> {
        if (!granted(ctx, Manifest.permission.READ_PHONE_STATE)) return emptyList()
        val tm = ctx.getSystemService(TelecomManager::class.java) ?: return emptyList()
        return try {
            tm.callCapablePhoneAccounts.mapIndexed { i, h ->
                val label = tm.getPhoneAccount(h)?.label?.toString()?.takeIf { it.isNotBlank() }
                PhoneLine(h.id, h.componentName.flattenToString(), label ?: "Línea ${i + 1}")
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun lineLabel(ctx: Context, handle: PhoneAccountHandle?): String? = try {
        handle?.let { ctx.getSystemService(TelecomManager::class.java)?.getPhoneAccount(it)?.label?.toString() }
    } catch (e: Exception) {
        null
    }

    /**
     * Marca un código MMI (lo procesa el operador y muestra el resultado en pantalla).
     * Si hay una línea protegida elegida, se marca por esa SIM.
     */
    @SuppressLint("MissingPermission")
    fun dial(ctx: Context, code: String, s: AppSettings): Boolean {
        if (!granted(ctx, Manifest.permission.CALL_PHONE)) return false
        val tm = ctx.getSystemService(TelecomManager::class.java) ?: return false
        val extras = Bundle()
        if (s.lineId.isNotBlank()) {
            ComponentName.unflattenFromString(s.lineComponent)?.let { cn ->
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, PhoneAccountHandle(cn, s.lineId))
            }
        }
        return try {
            tm.placeCall(Uri.fromParts("tel", code, null), extras)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
