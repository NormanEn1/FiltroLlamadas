package com.norman.filtrollamadas.data

import com.norman.filtrollamadas.data.db.CallEntry
import com.norman.filtrollamadas.domain.Reason
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun write(out: OutputStream, entries: List<CallEntry>) {
        out.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write(0xFEFF) // BOM para que Excel abra bien los acentos
            w.write("fecha,numero,decision,motivo,linea,nota\n")
            entries.forEach { e ->
                val date = Instant.ofEpochMilli(e.timestamp).atZone(ZoneId.systemDefault()).format(fmt)
                val row = listOf(
                    date,
                    e.rawNumber ?: "Oculto",
                    e.decision,
                    Reason.labelOf(e.reason),
                    e.lineLabel.orEmpty(),
                    e.note.orEmpty(),
                )
                w.write(row.joinToString(",") { escape(it) })
                w.write("\n")
            }
        }
    }

    private fun escape(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
}
