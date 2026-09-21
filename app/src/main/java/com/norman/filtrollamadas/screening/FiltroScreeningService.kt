package com.norman.filtrollamadas.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.norman.filtrollamadas.FiltroApp
import com.norman.filtrollamadas.data.AppContainer
import com.norman.filtrollamadas.data.db.CallEntry
import com.norman.filtrollamadas.domain.Decision
import com.norman.filtrollamadas.domain.PhoneNumbers
import com.norman.filtrollamadas.domain.Reason
import com.norman.filtrollamadas.domain.Verdict
import com.norman.filtrollamadas.notify.Notifier
import com.norman.filtrollamadas.telephony.Telefonia
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.ZoneId

/**
 * Android invoca este servicio antes de que el teléfono timbre.
 * Si la llamada es "no identificada" se rechaza en silencio: el operador la ve
 * como "ocupado" y la desvía al destino configurado (desvío condicional **67*).
 */
class FiltroScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }
        val container = (application as FiltroApp).container
        val raw = callDetails.handle?.schemeSpecificPart
        val hidden = raw.isNullOrBlank() ||
            callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val account = callDetails.accountHandle

        container.appScope.launch {
            // Android da ~5 s para responder; ante cualquier fallo, la llamada timbra normal.
            val verdict = try {
                withTimeout(3_500) { container.engine.evaluate(raw, hidden, account?.id) }
            } catch (t: Throwable) {
                Verdict(Decision.PERMITIDA, Reason.ERROR)
            }

            val response = CallResponse.Builder().apply {
                if (verdict.decision == Decision.DESVIADA) {
                    setDisallowCall(true)
                    setRejectCall(true)       // "ocupado" → desvío del operador
                    setSkipCallLog(false)
                    setSkipNotification(true) // sin notificación de llamada perdida
                }
            }.build()
            respondToCall(callDetails, response)

            if (verdict.reason != Reason.FILTRO_APAGADO) {
                record(container, if (hidden) null else raw, account, verdict)
            }
        }
    }

    private suspend fun record(container: AppContainer, number: String?, account: PhoneAccountHandle?, verdict: Verdict) {
        runCatching {
            val s = container.settings.current()
            container.database.callDao().insert(
                CallEntry(
                    timestamp = System.currentTimeMillis(),
                    rawNumber = number,
                    numberKey = PhoneNumbers.key(number),
                    decision = verdict.decision.name,
                    reason = verdict.reason.name,
                    lineLabel = Telefonia.lineLabel(this, account),
                    contactName = verdict.contactName,
                )
            )
            if (verdict.decision == Decision.DESVIADA && s.notifyOnDivert) {
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val today = container.database.callDao().countDecisionSince(Decision.DESVIADA.name, startOfDay)
                Notifier.diverted(
                    this,
                    today,
                    verdict.contactName ?: PhoneNumbers.pretty(number),
                    verdict.reason.label,
                    s.destinationLabel,
                )
            }
        }
    }
}
