package com.norman.filtrollamadas.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.norman.filtrollamadas.data.db.CallDao
import com.norman.filtrollamadas.data.db.ListDao
import com.norman.filtrollamadas.data.settings.SettingsRepository

/**
 * Decide si una llamada entrante timbra normal o se rechaza en silencio
 * (el operador la desvía al destino con el código **67*).
 * Todo se evalúa localmente; nada sale del teléfono.
 */
class ScreeningEngine(
    private val context: Context,
    private val settings: SettingsRepository,
    private val lists: ListDao,
    private val calls: CallDao,
) {

    suspend fun evaluate(rawNumber: String?, hidden: Boolean, accountId: String?): Verdict {
        val s = settings.current()
        if (!s.enabled) return allow(Reason.FILTRO_APAGADO)
        if (s.destinationNumber.isBlank()) return allow(Reason.SIN_DESTINO)
        if (s.lineId.isNotBlank() && accountId != null && accountId != s.lineId) {
            return allow(Reason.LINEA_NO_PROTEGIDA)
        }

        val digits = PhoneNumbers.digits(rawNumber)
        if (hidden || digits.isEmpty()) {
            return if (s.divertHidden) divert(Reason.OCULTO) else allow(Reason.OCULTO)
        }
        if (isEmergency(digits)) return allow(Reason.EMERGENCIA)
        if (digits.length <= 6) return allow(Reason.CODIGO_CORTO)

        val key = PhoneNumbers.key(digits)
        val listed = lists.find(key)
        if (listed?.type == ListType.NEGRA.name) return divert(Reason.LISTA_NEGRA)
        if (isContact(rawNumber.orEmpty())) return allow(Reason.CONTACTO)
        if (listed?.type == ListType.BLANCA.name) return allow(Reason.LISTA_BLANCA)
        if (s.recentDays > 0 && calledRecently(key, s.recentDays)) return allow(Reason.LLAMADA_RECIENTE)
        if (s.allowRepeat) {
            val since = System.currentTimeMillis() - s.repeatMinutes * 60_000L
            if (calls.countSince(key, Decision.DESVIADA.name, since) > 0) return allow(Reason.RELLAMADA)
        }
        return if (s.divertUnknown) divert(Reason.DESCONOCIDO) else allow(Reason.DESCONOCIDO)
    }

    private fun allow(r: Reason) = Verdict(Decision.PERMITIDA, r)
    private fun divert(r: Reason) = Verdict(Decision.DESVIADA, r)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun isEmergency(digits: String): Boolean = try {
        context.getSystemService(TelephonyManager::class.java)?.isEmergencyNumber(digits) == true
    } catch (e: Exception) {
        digits in setOf("123", "112", "911")
    }

    private fun isContact(number: String): Boolean {
        if (!granted(Manifest.permission.READ_CONTACTS)) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return try {
            context.contentResolver
                .query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun calledRecently(key: String, days: Int): Boolean {
        if (!granted(Manifest.permission.READ_CALL_LOG)) return false
        val since = System.currentTimeMillis() - days * 86_400_000L
        return try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} > ?",
                arrayOf(CallLog.Calls.OUTGOING_TYPE.toString(), since.toString()),
                null,
            )?.use { c ->
                var found = false
                while (!found && c.moveToNext()) {
                    found = PhoneNumbers.key(c.getString(0)) == key
                }
                found
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
