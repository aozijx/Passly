package com.aozijx.passly.feature.scanner.presentation.ui

import com.aozijx.passly.feature.scanner.presentation.ScannerUiState

internal data class ScannerScreenModel(
    val content: ScannerUiState,
    val isHandlingResult: Boolean,
) {
    val isScanning: Boolean
        get() = content == ScannerUiState.Scanning && !isHandlingResult
}
