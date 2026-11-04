package com.aozijx.passly.presentation.shared.util

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private const val DEFAULT_CLICK_INTERVAL_MILLIS = 300L

/** Allows the first click immediately and ignores repeated clicks during a short interval. */
class ClickThrottle internal constructor(
    private val intervalMillis: Long,
) {
    private var lastAcceptedClickAt = Long.MIN_VALUE

    fun tryAcquire(): Boolean {
        val now = SystemClock.uptimeMillis()
        if (lastAcceptedClickAt != Long.MIN_VALUE && now - lastAcceptedClickAt < intervalMillis) {
            return false
        }
        lastAcceptedClickAt = now
        return true
    }
}

@Composable
fun rememberClickThrottle(intervalMillis: Long = DEFAULT_CLICK_INTERVAL_MILLIS): ClickThrottle =
    remember(intervalMillis) { ClickThrottle(intervalMillis) }
