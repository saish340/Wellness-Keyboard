package com.example.wellnesskeyboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class DailyAverageIki(
    val day: String,
    val averageIki: Double?
)

data class SessionMetrics(
    val sessionId: String,
    val averageIki: Double?,
    val backspaceCount: Int,
    val firstTimestamp: Long?,
    val lastTimestamp: Long?,
    val eventCount: Int
)

@Dao
interface KeystrokeEventDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: KeystrokeEvent): Long

    @Query(
        """
        SELECT *
        FROM keystroke_events
        WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp
        ORDER BY timestamp ASC
        """
    )
    suspend fun getEventsByDateRange(startTimestamp: Long, endTimestamp: Long): List<KeystrokeEvent>

    @Query("SELECT sessionId FROM keystroke_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentSessionId(): String?

    @Query("SELECT * FROM keystroke_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: String): List<KeystrokeEvent>

    @Query("SELECT COUNT(*) FROM keystroke_events WHERE sessionId = :sessionId AND isBackspace = 1")
    suspend fun getBackspaceCountForSession(sessionId: String): Int

    @Query(
        """
        SELECT
            :sessionId AS sessionId,
            AVG(interKeyInterval) AS averageIki,
            SUM(CASE WHEN isBackspace = 1 THEN 1 ELSE 0 END) AS backspaceCount,
            MIN(timestamp) AS firstTimestamp,
            MAX(timestamp) AS lastTimestamp,
            COUNT(*) AS eventCount
        FROM keystroke_events
        WHERE sessionId = :sessionId
        """
    )
    suspend fun getSessionMetrics(sessionId: String): SessionMetrics?

    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch') AS day,
               AVG(interKeyInterval) AS averageIki
        FROM keystroke_events
        GROUP BY date(timestamp / 1000, 'unixepoch')
        ORDER BY day ASC
        """
    )
    suspend fun getAverageIkiPerDay(): List<DailyAverageIki>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyAnomalyScore(score: DailyAnomalyScore)

    @Query("SELECT * FROM daily_anomaly_scores WHERE date = :date LIMIT 1")
    suspend fun getDailyAnomalyScore(date: String): DailyAnomalyScore?
}
