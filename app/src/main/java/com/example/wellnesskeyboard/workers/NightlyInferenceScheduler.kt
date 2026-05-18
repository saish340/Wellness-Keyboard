package com.example.wellnesskeyboard.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NightlyInferenceScheduler {
    private const val UNIQUE_WORK_NAME = "nightly_wellness_inference"

    fun schedule(context: Context) {
        enqueue(context, nextDelayMillis())
    }

    fun scheduleNext(context: Context) {
        enqueue(context, nextDelayMillis())
    }

    private fun enqueue(context: Context, delayMillis: Long) {
        val request = OneTimeWorkRequestBuilder<NightlyInferenceWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun nextDelayMillis(): Long {
        val now = Calendar.getInstance()
        val nextRun = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (nextRun.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }
}