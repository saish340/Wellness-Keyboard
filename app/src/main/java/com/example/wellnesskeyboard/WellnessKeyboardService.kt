package com.example.wellnesskeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.KeyboardView
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.example.wellnesskeyboard.data.AppDatabase
import com.example.wellnesskeyboard.data.KeystrokeEvent
import com.example.wellnesskeyboard.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar

class WellnessKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pressedAtByKeyCode = mutableMapOf<Int, PendingKeyPress>()
    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val sessionManager = SessionManager()
    private var lastPressTimeMillis: Long? = null

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        pressedAtByKeyCode.clear()
        lastPressTimeMillis = null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        recordKeyDown(keyCode)
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        recordKeyUp(keyCode)
        return super.onKeyUp(keyCode, event)
    }

    override fun onPress(primaryCode: Int) {
        recordKeyDown(primaryCode)
    }

    override fun onRelease(primaryCode: Int) {
        recordKeyUp(primaryCode)
    }

    private fun recordKeyDown(keyCode: Int) {
        val pressTimeMillis = System.currentTimeMillis()
        val interKeyIntervalMillis = lastPressTimeMillis?.let { pressTimeMillis - it }
        val sessionId = sessionManager.recordKeystroke(pressTimeMillis)

        pressedAtByKeyCode[keyCode] = PendingKeyPress(
            pressTimeMillis = pressTimeMillis,
            interKeyIntervalMillis = interKeyIntervalMillis ?: 0L,
            sessionId = sessionId
        )
        lastPressTimeMillis = pressTimeMillis
    }

    private fun recordKeyUp(keyCode: Int) {
        val releaseTimeMillis = System.currentTimeMillis()
        val pendingKeyPress = pressedAtByKeyCode.remove(keyCode)
        val pressTimeMillis = pendingKeyPress?.pressTimeMillis ?: releaseTimeMillis
        val holdDurationMillis = releaseTimeMillis - pressTimeMillis
        val sessionId = pendingKeyPress?.sessionId ?: sessionManager.currentSessionId()
        val timestampHourOfDay = hourOfDayFrom(pressTimeMillis)

        val event = KeystrokeEvent(
            sessionId = sessionId,
            timestamp = pressTimeMillis,
            holdDuration = holdDurationMillis,
            interKeyInterval = pendingKeyPress?.interKeyIntervalMillis ?: 0L,
            isBackspace = keyCode == KeyEvent.KEYCODE_DEL,
            hourOfDay = timestampHourOfDay
        )

        serviceScope.launch {
            database.keystrokeEventDao().insert(event)
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        // Intentionally left blank. This scaffold captures metadata only.
    }

    override fun onText(text: CharSequence?) {
        // Intentionally ignored so no typed content is logged.
    }

    override fun swipeLeft() = Unit

    override fun swipeRight() = Unit

    override fun swipeDown() = Unit

    override fun swipeUp() = Unit

    private data class PendingKeyPress(
        val pressTimeMillis: Long,
        val interKeyIntervalMillis: Long,
        val sessionId: String
    )

    private fun hourOfDayFrom(timestampMillis: Long): Int {
        return Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }.get(Calendar.HOUR_OF_DAY)
    }
}
