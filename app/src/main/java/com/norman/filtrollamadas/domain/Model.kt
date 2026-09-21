package com.norman.filtrollamadas.domain

enum class Decision(val label: String) {
    DESVIADA("Desviada"),
    PERMITIDA("Permitida"),
}

enum class Reason(val label: String) {
    FILTRO_APAGADO("Filtro desactivado"),
    SIN_DESTINO("Sin destino configurado"),
    LINEA_NO_PROTEGIDA("Línea no protegida"),
    EMERGENCIA("Número de emergencia"),
    CODIGO_CORTO("Código corto"),
    OCULTO("Número oculto"),
    LISTA_NEGRA("Número bloqueado"),
    CONTACTO("En contactos"),
    LISTA_BLANCA("Número permitido"),
    LLAMADA_RECIENTE("Lo llamaste hace poco"),
    RELLAMADA("Insistió (rellamada)"),
    DESCONOCIDO("No está en contactos"),
    ERROR("Error o tiempo agotado");

    companion object {
        fun labelOf(name: String): String = entries.firstOrNull { it.name == name }?.label ?: name
    }
}

data class Verdict(val decision: Decision, val reason: Reason, val contactName: String? = null)

/**
 * BLANCA: siempre timbra · NEGRA: siempre se desvía (ni "insiste" lo deja pasar)
 * REVISADO: ya visto en la bandeja; el motor lo trata como desconocido.
 */
enum class ListType { BLANCA, NEGRA, REVISADO }

object PhoneNumbers {
    fun digits(raw: String?): String = raw?.filter { it.isDigit() }.orEmpty()

    /** Clave de comparación: últimos 10 dígitos (ignora +57, espacios, guiones). */
    fun key(raw: String?): String {
        val d = digits(raw)
        return if (d.length > 10) d.takeLast(10) else d
    }

    fun pretty(raw: String?): String {
        val d = digits(raw)
        if (d.isEmpty()) return "Número oculto"
        // "+57 300…" y "300…" son el mismo número local
        val local = if (d.length == 12 && d.startsWith("57")) d.drop(2) else d
        return when {
            local.length == 10 -> "${local.substring(0, 3)} ${local.substring(3, 6)} ${local.substring(6)}"
            local.length > 10 -> "+$local"          // internacional
            else -> local
        }
    }
}
