package com.aozijx.passly.feature.scanner.presentation.state

internal fun mapScannerCameraState(state: ScannerUiState): ScannerCameraState =
    when (state) {
        is ScannerUiState.Scanning -> ScannerCameraState.Active

        ScannerUiState.ProcessingImage,
        is ScannerUiState.ShowingResult,
        ScannerUiState.Stopped,
        -> ScannerCameraState.Idle
    }
