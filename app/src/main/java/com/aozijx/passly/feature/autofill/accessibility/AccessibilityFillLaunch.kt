package com.aozijx.passly.feature.autofill.accessibility

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal fun launchAccessibilityFill(
    scope: CoroutineScope,
    token: String,
    discardTrace: (String) -> Unit,
    clearCaptured: (String) -> Unit,
    onFailure: suspend (Exception) -> Unit,
    launch: suspend () -> Unit,
) : Job {
    val cleaned = AtomicBoolean(false)
    fun cleanup(error: Exception) {
        if (cleaned.compareAndSet(false, true)) {
            clearCapturedRequest(token, discardTrace, clearCaptured, error)
        }
    }

    val job = scope.launch {
        try {
            launch()
        } catch (cancelled: CancellationException) {
            cleanup(cancelled)
            throw cancelled
        } catch (error: Exception) {
            cleanup(error)
            onFailure(error)
        }
    }
    job.invokeOnCompletion { cause ->
        if (cause is CancellationException) cleanup(cause)
    }
    return job
}

private fun clearCapturedRequest(
    token: String,
    discardTrace: (String) -> Unit,
    clearCaptured: (String) -> Unit,
    originalError: Exception,
) {
    try {
        discardTrace(token)
    } catch (cleanupError: Exception) {
        originalError.addSuppressed(cleanupError)
    }
    try {
        clearCaptured(token)
    } catch (cleanupError: Exception) {
        originalError.addSuppressed(cleanupError)
    }
}
