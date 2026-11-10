package com.aozijx.passly.feature.scanner.presentation.state

internal sealed interface ScannerCameraState {
    data object Active : ScannerCameraState

    data object Idle : ScannerCameraState
}
