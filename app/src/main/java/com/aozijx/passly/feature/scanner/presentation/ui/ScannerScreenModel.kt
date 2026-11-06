package com.aozijx.passly.feature.scanner.presentation.ui

import com.aozijx.passly.feature.scanner.presentation.ScannerUiState
import com.aozijx.passly.feature.scanner.presentation.ScannerResultPresentation

internal data class ScannerScreenModel(
    val content: ScannerUiState,
    val isResultActionLocked: Boolean,
    val resultPresentation: ScannerResultPresentation,
) {
    val isScanning: Boolean
        get() = content == ScannerUiState.Scanning && !isResultActionLocked
}
