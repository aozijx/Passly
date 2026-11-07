package com.aozijx.passly.security.authentication.flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owns retry of a derived authentication-method snapshot after a transient refresh failure. */
internal class AuthenticationAvailabilityRetry(
    private val scope: CoroutineScope,
    private val initialDelayMs: Long = INITIAL_DELAY_MS,
    private val maxDelayMs: Long = MAX_DELAY_MS,
    private val refresh: suspend () -> Unit,
) {
    private var retryJob: Job? = null

    @Synchronized
    fun schedule() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            var retryDelayMs = initialDelayMs
            while (true) {
                delay(retryDelayMs)
                try {
                    refresh()
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxDelayMs)
                }
            }
        }
    }

    private companion object {
        const val INITIAL_DELAY_MS = 1_000L
        const val MAX_DELAY_MS = 60_000L
    }
}
