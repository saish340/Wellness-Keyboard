package com.example.wellnesskeyboard.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wellnesskeyboard.data.AppDatabase
import com.example.wellnesskeyboard.data.DailyAnomalyScore
import com.example.wellnesskeyboard.data.KeystrokeEvent
import com.example.wellnesskeyboard.ml.WellnessInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NightlyInferenceWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getInstance(applicationContext)
        val targetDay = previousCalendarDay()
        val (startMillis, endMillis) = dayBoundsMillis(targetDay)
        val events = database.keystrokeEventDao().getEventsByDateRange(startMillis, endMillis)

        val featureVector = buildDailyFeatureVector(events)
        val anomalyScore = WellnessInference(applicationContext).use { inference ->
            inference.infer(featureVector)
        }

        val dayLabel = dayLabel(targetDay)
        database.keystrokeEventDao().upsertDailyAnomalyScore(
            DailyAnomalyScore(
                date = dayLabel,
                anomalyScore = anomalyScore,
            )
        )

        NightlyInferenceScheduler.scheduleNext(applicationContext)
        Result.success()
    }

    private fun buildDailyFeatureVector(events: List<KeystrokeEvent>): FloatArray {
        if (events.isEmpty()) {
            return FloatArray(8)
        }

        val total = events.size.toFloat()
        val ikiValues = events.map { it.interKeyInterval.toFloat() }
        val holdValues = events.map { it.holdDuration.toFloat() }
        val backspaces = events.count { it.isBackspace }.toFloat()
        val lateNight = events.count { it.hourOfDay == 23 || it.hourOfDay in 0..3 }.toFloat()
        val sessions = events.groupBy { it.sessionId }
        val sessionDurationsSeconds = sessions.values.map { sessionEvents ->
            val first = sessionEvents.minOf { it.timestamp }
            val last = sessionEvents.maxOf { it.timestamp }
            ((last - first).coerceAtLeast(0L) / 1000f)
        }

        val avgIki = ikiValues.average().toFloat()
        val stdIki = stdDev(ikiValues)
        val backspaceRate = backspaces / total
        val avgHold = holdValues.average().toFloat()
        val durationMinutes = (((events.maxOf { it.timestamp } - events.minOf { it.timestamp }).coerceAtLeast(1L)) / 60000f).coerceAtLeast(1f / 60f)
        val wpmEstimate = (total / 5f) / durationMinutes
        val lateNightRatio = lateNight / total
        val sessionCount = sessions.size.toFloat()
        val avgSessionDuration = if (sessionDurationsSeconds.isNotEmpty()) sessionDurationsSeconds.average().toFloat() else 0f

        return floatArrayOf(
            avgIki,
            stdIki,
            backspaceRate,
            avgHold,
            wpmEstimate,
            lateNightRatio,
            sessionCount,
            avgSessionDuration,
        )
    }

    private fun stdDev(values: List<Float>): Float {
        if (values.size <= 1) return 0f
        val mean = values.average()
        val variance = values.sumOf { value -> (value - mean) * (value - mean).toDouble() } / values.size
        return kotlin.math.sqrt(variance).toFloat()
    }

    private fun previousCalendarDay(): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }

    private fun dayBoundsMillis(day: Calendar): Pair<Long, Long> {
        val start = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    private fun dayLabel(day: Calendar): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(day.time)
    }
}