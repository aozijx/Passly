package com.aozijx.passly.presentation.feature.scanner

sealed interface ScannerScreenAction {
    data object Dismiss : ScannerScreenAction
    data object PickPhoto : ScannerScreenAction
    data object ScanAgain : ScannerScreenAction
    data object ConfirmResult : ScannerScreenAction
    data class BarcodeDetected(val rawValue: String) : ScannerScreenAction
    data object CameraPermissionDenied : ScannerScreenAction
}
