package com.aozijx.passly.feature.scanner.presentation

sealed interface ScannerUiState {
    data object Scanning : ScannerUiState
    data object ProcessingImage : ScannerUiState
    data class ShowingResult(val result: ScannerResult) : ScannerUiState
    data object Stopped : ScannerUiState
}
