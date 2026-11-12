package com.aozijx.passly.feature.scanner.presentation.state

import com.aozijx.passly.feature.scanner.presentation.result.ScannerResult

internal sealed interface ScannerMutation {
    data object Started : ScannerMutation

    data object ImageProcessingStarted : ScannerMutation

    data object ImageProcessingFailed : ScannerMutation

    data object Stopped : ScannerMutation

    data class ScanCompleted(
        val result: ScannerResult,
    ) : ScannerMutation

    data class FeedbackPublished(
        val feedback: ScannerFeedback,
    ) : ScannerMutation

    data class FeedbackCleared(
        val id: Long? = null,
    ) : ScannerMutation
}

internal object ScannerUiStateReducer {
    fun reduce(
        state: ScannerUiState,
        mutation: ScannerMutation,
    ): ScannerUiState =
        when (mutation) {
            ScannerMutation.Started -> {
                ScannerUiState.Scanning()
            }

            ScannerMutation.ImageProcessingStarted -> {
                state.transitionUnlessStopped(ScannerUiState.ProcessingImage)
            }

            ScannerMutation.ImageProcessingFailed -> {
                state.transitionUnlessStopped(ScannerUiState.Scanning())
            }

            ScannerMutation.Stopped -> {
                ScannerUiState.Stopped
            }

            is ScannerMutation.ScanCompleted -> {
                state.transitionUnlessStopped(ScannerUiState.ShowingResult(mutation.result))
            }

            is ScannerMutation.FeedbackPublished -> {
                state.withFeedback(mutation.feedback)
            }

            is ScannerMutation.FeedbackCleared -> {
                state.clearFeedback(mutation.id)
            }
        }
}

private fun ScannerUiState.transitionUnlessStopped(target: ScannerUiState): ScannerUiState =
    when (this) {
        ScannerUiState.Stopped -> this

        is ScannerUiState.Scanning,
        ScannerUiState.ProcessingImage,
        is ScannerUiState.ShowingResult,
        -> target
    }

private fun ScannerUiState.withFeedback(feedback: ScannerFeedback): ScannerUiState =
    when (this) {
        is ScannerUiState.Scanning -> copy(feedback = feedback)

        is ScannerUiState.ShowingResult -> copy(feedback = feedback)

        ScannerUiState.ProcessingImage,
        ScannerUiState.Stopped,
        -> this
    }

private fun ScannerUiState.clearFeedback(id: Long? = null): ScannerUiState =
    when (this) {
        is ScannerUiState.Scanning -> copy(feedback = feedback?.takeUnless { id == null || it.id == id })

        is ScannerUiState.ShowingResult -> copy(feedback = feedback?.takeUnless { id == null || it.id == id })

        ScannerUiState.ProcessingImage,
        ScannerUiState.Stopped,
        -> this
    }
