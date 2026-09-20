package com.norman.filtrollamadas.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Insert
    suspend fun insert(entry: CallEntry): Long

    @Query("SELECT * FROM llamadas ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CallEntry>>

    @Query("SELECT * FROM llamadas ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CallEntry>>

    @Query("SELECT COUNT(*) FROM llamadas WHERE decision = :decision AND timestamp >= :since")
    fun observeCount(decision: String, since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM llamadas WHERE numberKey = :key AND decision = :decision AND timestamp >= :since")
    suspend fun countSince(key: String, decision: String, since: Long): Int

    @Query("SELECT COUNT(*) FROM llamadas WHERE decision = :decision AND timestamp >= :since")
    suspend fun countDecisionSince(decision: String, since: Long): Int

    /**
     * Números desviados por revisar: sin clasificar, o marcados REVISADO pero que
     * volvieron a llamar después de la revisión. Agrupados por número.
     */
    @Query(
        """
        SELECT l.numberKey AS numberKey, MAX(l.rawNumber) AS rawNumber,
               COUNT(*) AS attempts, MAX(l.timestamp) AS lastAt
        FROM llamadas l LEFT JOIN listas s ON s.numberKey = l.numberKey
        WHERE l.decision = :decision AND l.numberKey != ''
          AND (s.type IS NULL OR s.type = 'REVISADO')
        GROUP BY l.numberKey
        HAVING MAX(s.createdAt) IS NULL OR MAX(l.timestamp) > MAX(s.createdAt)
        ORDER BY lastAt DESC
        """
    )
    fun observePending(decision: String): Flow<List<PendingNumber>>

    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT l.numberKey
            FROM llamadas l LEFT JOIN listas s ON s.numberKey = l.numberKey
            WHERE l.decision = :decision AND l.numberKey != ''
              AND (s.type IS NULL OR s.type = 'REVISADO')
            GROUP BY l.numberKey
            HAVING MAX(s.createdAt) IS NULL OR MAX(l.timestamp) > MAX(s.createdAt)
        )
        """
    )
    fun observePendingCount(decision: String): Flow<Int>

    /** Números marcados como revisados que no han vuelto a llamar desde entonces. */
    @Query(
        """
        SELECT s.numberKey AS numberKey, s.rawNumber AS rawNumber,
               COUNT(l.id) AS attempts, COALESCE(MAX(l.timestamp), s.createdAt) AS lastAt
        FROM listas s LEFT JOIN llamadas l ON l.numberKey = s.numberKey AND l.decision = :decision
        WHERE s.type = 'REVISADO'
        GROUP BY s.numberKey
        HAVING COALESCE(MAX(l.timestamp), 0) <= MAX(s.createdAt)
        ORDER BY lastAt DESC
        """
    )
    fun observeReviewed(decision: String): Flow<List<PendingNumber>>

    @Query("SELECT * FROM llamadas ORDER BY timestamp DESC")
    suspend fun all(): List<CallEntry>

    @Query("UPDATE llamadas SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("DELETE FROM llamadas WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM llamadas")
    suspend fun clear()

    @Query("DELETE FROM llamadas WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface ListDao {
    @Query("SELECT * FROM listas WHERE numberKey = :key LIMIT 1")
    suspend fun find(key: String): ListEntry?

    @Query("SELECT * FROM listas WHERE type = :type ORDER BY createdAt DESC")
    fun observe(type: String): Flow<List<ListEntry>>

    @Upsert
    suspend fun upsert(entry: ListEntry)

    @Upsert
    suspend fun upsertAll(entries: List<ListEntry>)

    @Query("DELETE FROM listas WHERE numberKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM listas WHERE numberKey IN (:keys)")
    suspend fun deleteAll(keys: List<String>)
}
