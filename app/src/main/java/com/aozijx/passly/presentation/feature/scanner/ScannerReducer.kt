package com.aozijx.passly.presentation.feature.scanner

internal sealed interface ScannerMutation {
    data object Started : ScannerMutation
    data object Stopped : ScannerMutation
    data class ScanCompleted(val result: ScannerResult) : ScannerMutation
}

internal object ScannerReducer {
    fun reduce(state: ScannerUiState, mutation: ScannerMutation): ScannerUiState =
        when (mutation) {
            ScannerMutation.Started -> ScannerUiState()
            ScannerMutation.Stopped -> ScannerUiState(isScanning = false)
            is ScannerMutation.ScanCompleted -> state.copy(
                isScanning = false,
                result = mutation.result,
            )
        }
}
