package com.aozijx.passly.features.main.internal

import com.aozijx.passly.core.security.AutoLockScheduler

internal data class AutoLockDecision(
    val timeoutMs: Long,
    val shouldLockNow: Boolean,
    val timeoutAdjusted: Boolean
)

internal class MainAutoLockCoordinator(
    private val scheduler: AutoLockScheduler,
    private val validationSupport: MainValidationSupport,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var timeoutMs: Long = MainValidationSupport.DEFAULT_LOCK_TIMEOUT_MS
    private var lastInteractionAtMs: Long? = null

    fun applyTimeout(rawTimeoutMs: Long, isAuthorized: Boolean): AutoLockDecision {
        val normalizedTimeout = validationSupport.normalizeLockTimeout(rawTimeoutMs)
        val timeoutAdjusted = normalizedTimeout != rawTimeoutMs
        timeoutMs = normalizedTimeout

        if (!isAuthorized) {
            return AutoLockDecision(
                timeoutMs = timeoutMs,
                shouldLockNow = false,
                timeoutAdjusted = timeoutAdjusted
            )
        }

        return evaluateLockNeed(timeoutAdjusted = timeoutAdjusted)
    }

    fun onAuthorized() {
        lastInteractionAtMs = nowProvider()
        scheduler.schedule(timeoutMs)
    }

    fun onInteraction(isAuthorized: Boolean) {
        if (!isAuthorized) return
        lastInteractionAtMs = nowProvider()
        scheduler.schedule(timeoutMs)
    }

    fun checkNow(isAuthorized: Boolean): AutoLockDecision {
        if (!isAuthorized) {
            return AutoLockDecision(
                timeoutMs = timeoutMs,
                shouldLockNow = false,
                timeoutAdjusted = false
            )
        }

        return evaluateLockNeed(timeoutAdjusted = false)
    }

    fun onLocked() {
        scheduler.cancel()
        lastInteractionAtMs = null
    }

    private fun evaluateLockNeed(timeoutAdjusted: Boolean): AutoLockDecision {
        val lastInteraction = lastInteractionAtMs
        if (lastInteraction == null) {
            lastInteractionAtMs = nowProvider()
            scheduler.schedule(timeoutMs)
            return AutoLockDecision(
                timeoutMs = timeoutMs,
                shouldLockNow = false,
                timeoutAdjusted = timeoutAdjusted
            )
        }

        val elapsed = (nowProvider() - lastInteraction).coerceAtLeast(0L)
        if (elapsed >= timeoutMs) {
            scheduler.cancel()
            return AutoLockDecision(
                timeoutMs = timeoutMs,
                shouldLockNow = true,
                timeoutAdjusted = timeoutAdjusted
            )
        }

        val remaining = (timeoutMs - elapsed).coerceAtLeast(1L)
        scheduler.schedule(remaining)
        return AutoLockDecision(
            timeoutMs = timeoutMs,
            shouldLockNow = false,
            timeoutAdjusted = timeoutAdjusted
        )
    }
}
