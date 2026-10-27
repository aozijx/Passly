package com.aozijx.passly.presentation.shared.error

import androidx.annotation.StringRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun writePreferenceWithFeedback(
    @StringRes failureRes: Int,
    onFailure: (UiErrorMessage) -> Unit,
    onSuccess: () -> Unit = {},
    write: suspend () -> Unit,
) {
    try {
        write()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        onFailure(error.toUiErrorMessage(failureRes))
        return
    }
    currentCoroutineContext().ensureActive()
    onSuccess()
}
