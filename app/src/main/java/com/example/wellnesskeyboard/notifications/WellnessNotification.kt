package com.example.wellnesskeyboard.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wellnesskeyboard.R
import com.example.wellnesskeyboard.data.DailyAnomalyScore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * WellnessNotification is a small, focused notification gateway for behavioral wellness updates.
 *
 * Design goals:
 * - Keep the logic anxiety-safe and gentle.
 * - Notify only when a sustained pattern suggests elevated fatigue.
 * - Rate-limit notifications so the app never feels noisy or intrusive.
 * - Open the app dashboard when the notification is tapped.
 *
 * The notification is intentionally conservative:
 * it only sends when the wellness score stays above the threshold for 3 or more
 * consecutive days and a weekly cooldown allows it.
 */
class WellnessNotification(
    context: Context,
    private val cooldownStore: NotificationCooldownStore = SharedPreferencesCooldownStore(context.applicationContext),
    private val dashboardIntentFactory: () -> Intent = {
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
    },
) {
    private val appContext: Context = context.applicationContext

    /**
     * A single channel keeps Android 8+ notifications organized and user-visible in system settings.
     * The channel name is calm and descriptive so the feature reads as behavioral wellness support,
     * not as an alerting or medical feature.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(false)
            enableVibration(false)
        }

        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Returns true only when the latest daily scores show:
     * 1) anomaly score > 0.65
     * 2) for 3 or more consecutive calendar days
     *
     * The input is expected to be daily anomaly records sorted in any order.
     */
    fun shouldTriggerNotification(dailyScores: List<DailyAnomalyScore>): Boolean {
        if (dailyScores.size < 3) return false

        val dateFormatter = utcDateFormatter()
        val sortedScores = dailyScores
            .mapNotNull { score -> parseDay(score.date, dateFormatter)?.let { parsedDate -> parsedDate to score.anomalyScore } }
            .sortedBy { (date, _) -> date.timeInMillis }

        var streakCount = 0
        var previousDay: Calendar? = null

        for ((currentDay, score) in sortedScores) {
            val isHighWellnessSignal = score > SCORE_THRESHOLD

            if (!isHighWellnessSignal) {
                streakCount = 0
                previousDay = currentDay
                continue
            }

            if (previousDay == null) {
                streakCount = 1
            } else if (isConsecutiveDay(previousDay, currentDay)) {
                streakCount += 1
            } else {
                streakCount = 1
            }

            if (streakCount >= REQUIRED_CONSECUTIVE_DAYS) {
                return true
            }

            previousDay = currentDay
        }

        return false
    }

    /**
     * Returns true when the weekly cooldown has expired.
     * The last notification timestamp is stored in private app preferences, which keeps the state
     * scoped to the app and avoids exposing it outside the app sandbox.
     */
    fun canSendNotification(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val lastSentMillis = cooldownStore.getLastSentAtMillis()
        if (lastSentMillis == null) return true

        return nowMillis - lastSentMillis >= WEEK_COOLDOWN_MILLIS
    }

    /**
     * Sends a gentle wellness notification if the high-score pattern persists and the cooldown
     * allows it. The notification opens the React Native dashboard when tapped.
     *
     * Returns true if a notification was posted.
     */
    fun sendWellnessNotification(dailyScores: List<DailyAnomalyScore>): Boolean {
        createNotificationChannel()

        val nowMillis = System.currentTimeMillis()
        if (!shouldTriggerNotification(dailyScores)) return false
        if (!canSendNotification(nowMillis)) return false

        val launchIntent = dashboardIntentFactory().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_OPEN_WELLNESS_DASHBOARD, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            REQUEST_CODE_OPEN_DASHBOARD,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(resolveSmallIcon())
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(NOTIFICATION_MESSAGE))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        cooldownStore.saveLastSentAtMillis(nowMillis)
        return true
    }

    /**
     * A conservative fallback icon keeps the code self-contained. In production, replace this with
     * a dedicated monochrome notification drawable for the app.
     */
    private fun resolveSmallIcon(): Int {
        val appIcon = appContext.applicationInfo.icon
        return if (appIcon != 0) appIcon else android.R.drawable.ic_dialog_info
    }

    private fun parseDay(rawDay: String, formatter: SimpleDateFormat): Calendar? {
        return try {
            val date = formatter.parse(rawDay) ?: return null
            Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: ParseException) {
            null
        }
    }

    private fun isConsecutiveDay(previous: Calendar, current: Calendar): Boolean {
        val expected = (previous.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return expected.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            expected.get(Calendar.DAY_OF_YEAR) == current.get(Calendar.DAY_OF_YEAR)
    }

    private fun utcDateFormatter(): SimpleDateFormat {
        return SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
    }

    interface NotificationCooldownStore {
        fun getLastSentAtMillis(): Long?
        fun saveLastSentAtMillis(timestampMillis: Long)
    }

    private class SharedPreferencesCooldownStore(context: Context) : NotificationCooldownStore {
        private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        override fun getLastSentAtMillis(): Long? {
            val storedValue = preferences.getLong(KEY_LAST_SENT_AT_MILLIS, Long.MIN_VALUE)
            return if (storedValue == Long.MIN_VALUE) null else storedValue
        }

        override fun saveLastSentAtMillis(timestampMillis: Long) {
            // SharedPreferences in private app mode keeps the timestamp scoped to the app sandbox.
            preferences.edit().putLong(KEY_LAST_SENT_AT_MILLIS, timestampMillis).apply()
        }
    }

    companion object {
        private const val CHANNEL_ID = "wellness_behavioral_channel"
        private const val CHANNEL_NAME = "Behavioral Wellness"
        private const val CHANNEL_DESCRIPTION = "Gentle behavioral wellness reminders"
        private const val NOTIFICATION_ID = 4102
        private const val REQUEST_CODE_OPEN_DASHBOARD = 4103
        private const val PREFS_NAME = "wellness_notification_prefs"
        private const val KEY_LAST_SENT_AT_MILLIS = "last_wellness_notification_at"
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private const val WEEK_COOLDOWN_MILLIS = 7L * 24L * 60L * 60L * 1000L
        private const val SCORE_THRESHOLD = 0.65f
        private const val REQUIRED_CONSECUTIVE_DAYS = 3

        private const val NOTIFICATION_TITLE = "Behavioral wellness check-in"
        private const val NOTIFICATION_MESSAGE =
            "Your typing patterns suggest you may be fatigued. Consider taking a break."

        const val EXTRA_OPEN_WELLNESS_DASHBOARD = "com.example.wellnesskeyboard.EXTRA_OPEN_WELLNESS_DASHBOARD"
    }
}
