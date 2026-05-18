package com.example.wellnesskeyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_anomaly_scores")
data class DailyAnomalyScore(
    @PrimaryKey
    val date: String,
    val anomalyScore: Float,
    val createdAtMillis: Long = System.currentTimeMillis()
)