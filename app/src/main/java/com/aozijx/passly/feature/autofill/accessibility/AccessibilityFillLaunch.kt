package com.aozijx.passly.feature.autofill.accessibility

import kotlinx.coroutines.CancellationException

internal suspend fun launchAccessibilityFill(
    token: String,
    discardTrace: (String) -> Unit,
    clearCaptured: (String) -> Unit,
    onFailure: (Exception) -> Unit,
    launch: suspend () -> Unit,
) {
    try {
        launch()
    } catch (cancelled: CancellationException) {
        clearCapturedRequest(token, discardTrace, clearCaptured, cancelled)
        throw cancelled
    } catch (error: Exception) {
        clearCapturedRequest(token, discardTrace, clearCaptured, error)
        onFailure(error)
    }
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
