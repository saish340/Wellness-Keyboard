package com.example.wellnesskeyboard.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "keystroke_events",
    indices = [Index(value = ["sessionId"]), Index(value = ["timestamp"])]
)
data class KeystrokeEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val holdDuration: Long,
    val interKeyInterval: Long,
    val isBackspace: Boolean,
    val hourOfDay: Int
)
