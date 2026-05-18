package com.example.wellnesskeyboard.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wellnesskeyboard.data.AppDatabase
import com.example.wellnesskeyboard.data.KeystrokeEvent
import com.example.wellnesskeyboard.data.SessionMetrics
import com.example.wellnesskeyboard.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private var pollJob: Job? = null

    private lateinit var statsTextView: TextView
    private lateinit var inputEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.testInput)
        statsTextView = findViewById(R.id.sessionStats)

        inputEditText.requestFocus()
        startPolling()
    }

    override fun onDestroy() {
        pollJob?.cancel()
        super.onDestroy()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = lifecycleScope.launch {
            while (isActive) {
                refreshStats()
                delay(5_000L)
            }
        }
    }

    private suspend fun refreshStats() {
        val dao = database.keystrokeEventDao()
        val sessionId = dao.getMostRecentSessionId()

        if (sessionId == null) {
            statsTextView.text = buildStatsText(
                sessionId = "No session yet",
                averageIki = null,
                backspaceCount = 0,
                wpmEstimate = 0,
                sessionDurationSeconds = 0,
                eventCount = 0
            )
            return
        }

        val metrics = dao.getSessionMetrics(sessionId)
        val events = dao.getEventsForSession(sessionId)
        val eventCount = events.size
        val sessionDurationSeconds = computeSessionDurationSeconds(events)
        val averageIki = metrics?.averageIki
        val backspaceCount = metrics?.backspaceCount ?: dao.getBackspaceCountForSession(sessionId)
        val wpmEstimate = computeWpmEstimate(eventCount, sessionDurationSeconds)

        statsTextView.text = buildStatsText(
            sessionId = sessionId,
            averageIki = averageIki,
            backspaceCount = backspaceCount,
            wpmEstimate = wpmEstimate,
            sessionDurationSeconds = sessionDurationSeconds,
            eventCount = eventCount
        )
    }

    private fun computeSessionDurationSeconds(events: List<KeystrokeEvent>): Int {
        if (events.isEmpty()) return 0

        val first = events.first().timestamp
        val last = events.last().timestamp
        return (((last - first).coerceAtLeast(0L)) / 1000L).toInt()
    }

    private fun computeWpmEstimate(eventCount: Int, sessionDurationSeconds: Int): Int {
        if (eventCount == 0 || sessionDurationSeconds <= 0) return 0

        val wordsEquivalent = eventCount / 5.0
        val minutes = sessionDurationSeconds / 60.0
        return (wordsEquivalent / minutes).roundToInt()
    }

    private fun buildStatsText(
        sessionId: String,
        averageIki: Double?,
        backspaceCount: Int,
        wpmEstimate: Int,
        sessionDurationSeconds: Int,
        eventCount: Int
    ): String {
        return buildString {
            appendLine("Session: $sessionId")
            appendLine("Events logged: $eventCount")
            appendLine("Current session IKI average: ${averageIki?.let { "%.1f ms".format(it) } ?: "n/a"}")
            appendLine("Backspace count: $backspaceCount")
            appendLine("WPM estimate: $wpmEstimate")
            appendLine("Session duration: ${sessionDurationSeconds}s")
        }
    }
}