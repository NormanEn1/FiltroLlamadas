package com.norman.filtrollamadas.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una llamada revisada por el filtro. */
@Entity(tableName = "llamadas", indices = [Index("numberKey"), Index("timestamp")])
data class CallEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rawNumber: String?,      // null = número oculto
    val numberKey: String,       // últimos 10 dígitos, para comparar
    val decision: String,        // Decision.name
    val reason: String,          // Reason.name
    val lineLabel: String?,
    val note: String? = null,
)

/** Resultado agregado para la bandeja "Por revisar". */
data class PendingNumber(
    val numberKey: String,
    val rawNumber: String?,
    val attempts: Int,
    val lastAt: Long,
)

/** Entrada de lista blanca, negra o marcada como revisada. */
@Entity(tableName = "listas")
data class ListEntry(
    @PrimaryKey val numberKey: String,
    val rawNumber: String,
    val type: String,            // ListType.name
    val label: String,
    val createdAt: Long,
)
