package com.aozijx.passly.feature.scanner.presentation.route

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.aozijx.passly.feature.scanner.presentation.state.ScannerFeedback
import com.aozijx.passly.feature.scanner.presentation.state.ScannerUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

internal suspend fun collectScannerRouteFeedback(
    lifecycle: Lifecycle,
    uiState: StateFlow<ScannerUiState>,
    onHostStarted: () -> Unit,
    onHostStopped: () -> Unit,
    onFeedback: (ScannerFeedback) -> Unit,
    onFeedbackConsumed: (Long) -> Unit,
) {
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        onHostStarted()
        try {
            uiState.collect { state ->
                state.feedback?.let { feedback ->
                    onFeedback(feedback)
                    onFeedbackConsumed(feedback.id)
                }
            }
        } finally {
            onHostStopped()
        }
    }
}
