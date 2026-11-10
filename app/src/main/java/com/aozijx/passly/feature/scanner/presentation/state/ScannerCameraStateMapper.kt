package com.aozijx.passly.feature.scanner.presentation.state

internal fun mapScannerCameraState(
    overlay: ScannerOverlayState,
    state: ScannerUiState,
): ScannerCameraState =
    when (overlay) {
        ScannerOverlayState.Hidden -> ScannerCameraState.Idle
        ScannerOverlayState.Visible ->
            when (state) {
                is ScannerUiState.Scanning -> ScannerCameraState.Active
                ScannerUiState.ProcessingImage,
                is ScannerUiState.ShowingResult,
                ScannerUiState.Stopped,
                -> ScannerCameraState.Idle
            }
    }
