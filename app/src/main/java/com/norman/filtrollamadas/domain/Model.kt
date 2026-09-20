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

data class Verdict(val decision: Decision, val reason: Reason)

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
        val k = key(raw)
        if (k.isEmpty()) return "Número oculto"
        return if (k.length == 10 && k.startsWith("3")) {
            "${k.substring(0, 3)} ${k.substring(3, 6)} ${k.substring(6)}"
        } else {
            raw!!.trim()
        }
    }
}
