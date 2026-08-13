package com.aozijx.passly.feature.scanner.presentation

import com.aozijx.passly.feature.scanner.contract.ScannerUiState

internal sealed interface ScannerMutation {
    data object Started : ScannerMutation
    data object Stopped : ScannerMutation
    data object ScanCompleted : ScannerMutation
    data class DecodeFailed(val message: String) : ScannerMutation
}

internal object ScannerReducer {
    fun reduce(state: ScannerUiState, mutation: ScannerMutation): ScannerUiState =
        when (mutation) {
            ScannerMutation.Started -> ScannerUiState()
            ScannerMutation.Stopped -> state.copy(isScanning = false)
            ScannerMutation.ScanCompleted -> state.copy(isScanning = false, error = null)
            is ScannerMutation.DecodeFailed -> state.copy(error = mutation.message)
        }
}
