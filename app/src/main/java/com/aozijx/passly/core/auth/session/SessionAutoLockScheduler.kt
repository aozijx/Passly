package com.aozijx.passly.core.auth.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SessionAutoLockScheduler(
    private val scope: CoroutineScope,
    private val onTimeout: () -> Unit
) {
    private var timerJob: Job? = null

    fun schedule(timeoutMs: Long) {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeoutMs)
            onTimeout()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
    }
}