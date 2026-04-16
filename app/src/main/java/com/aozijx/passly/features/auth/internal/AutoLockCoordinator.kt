package com.aozijx.passly.features.auth.internal

import com.aozijx.passly.core.security.AutoLockScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 自动锁定决策结果。
 */
data class AutoLockDecision(
    val timeoutMs: Long,
    val shouldLockNow: Boolean,
    val timeoutAdjusted: Boolean
)

/**
 * 自动锁定协调器：负责计时器调度和锁定决策。
 */
class AutoLockCoordinator(
    scope: CoroutineScope,
    private val validationSupport: AuthValidationSupport,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val scheduler = AutoLockScheduler(scope) { onTimeoutTriggered() }
    
    private var timeoutMs: Long = AuthValidationSupport.DEFAULT_LOCK_TIMEOUT_MS
    private var lastInteractionAtMs: Long? = null

    private val _shouldLock = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shouldLock: SharedFlow<Unit> = _shouldLock.asSharedFlow()

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

        val decision = evaluateLockNeed(timeoutAdjusted = timeoutAdjusted)
        if (decision.shouldLockNow) {
            _shouldLock.tryEmit(Unit)
        }
        return decision
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

    fun checkNow(isAuthorized: Boolean) {
        if (!isAuthorized) return
        val decision = evaluateLockNeed(timeoutAdjusted = false)
        if (decision.shouldLockNow) {
            _shouldLock.tryEmit(Unit)
        }
    }

    fun onLocked() {
        scheduler.cancel()
        lastInteractionAtMs = null
    }

    private fun onTimeoutTriggered() {
        _shouldLock.tryEmit(Unit)
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