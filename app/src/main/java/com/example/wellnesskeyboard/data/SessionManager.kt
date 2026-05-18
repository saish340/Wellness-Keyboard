package com.example.wellnesskeyboard.data

import java.util.UUID

class SessionManager(
    private val sessionTimeoutMillis: Long = 60_000L
) {
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var lastKeystrokeTimeMillis: Long? = null

    @Synchronized
    fun recordKeystroke(timestampMillis: Long): String {
        if (lastKeystrokeTimeMillis == null || timestampMillis - lastKeystrokeTimeMillis!! >= sessionTimeoutMillis) {
            currentSessionId = UUID.randomUUID().toString()
        }

        lastKeystrokeTimeMillis = timestampMillis
        return currentSessionId
    }

    @Synchronized
    fun currentSessionId(): String = currentSessionId

    @Synchronized
    fun reset() {
        currentSessionId = UUID.randomUUID().toString()
        lastKeystrokeTimeMillis = null
    }
}