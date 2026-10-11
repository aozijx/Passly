package com.aozijx.passly.presentation.feature.scanner

sealed interface ScannerUiState {
    data object Scanning : ScannerUiState
    data object ProcessingImage : ScannerUiState
    data class ShowingResult(val result: ScannerResult) : ScannerUiState
    data object Stopped : ScannerUiState
}
