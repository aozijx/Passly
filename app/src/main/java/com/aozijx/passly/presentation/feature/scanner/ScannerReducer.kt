package com.aozijx.passly.presentation.feature.scanner

internal sealed interface ScannerMutation {
    data object Started : ScannerMutation
    data object ImageProcessingStarted : ScannerMutation
    data object ImageProcessingFailed : ScannerMutation
    data object Stopped : ScannerMutation
    data class ScanCompleted(val result: ScannerResult) : ScannerMutation
}

internal object ScannerReducer {
    fun reduce(state: ScannerUiState, mutation: ScannerMutation): ScannerUiState =
        when (mutation) {
            ScannerMutation.Started -> ScannerUiState.Scanning
            ScannerMutation.ImageProcessingStarted ->
                if (state == ScannerUiState.Stopped) state else ScannerUiState.ProcessingImage
            ScannerMutation.ImageProcessingFailed ->
                if (state == ScannerUiState.Stopped) state else ScannerUiState.Scanning
            ScannerMutation.Stopped -> ScannerUiState.Stopped
            is ScannerMutation.ScanCompleted ->
                if (state == ScannerUiState.Stopped) state
                else ScannerUiState.ShowingResult(mutation.result)
        }
}
