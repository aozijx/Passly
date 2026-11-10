package com.aozijx.passly.feature.scanner.presentation.action

import com.aozijx.passly.feature.scanner.presentation.image.ImageRef

sealed interface ScannerUiAction {
    data class BarcodeDetected(
        val barcode: String,
    ) : ScannerUiAction

    data class DecodeImage(
        val image: ImageRef,
    ) : ScannerUiAction

    data object CopyText : ScannerUiAction

    data class FeedbackConsumed(
        val id: Long,
    ) : ScannerUiAction

    data object StartScanning : ScannerUiAction

    data object HostStarted : ScannerUiAction

    data object HostStopped : ScannerUiAction

    data object StopScanning : ScannerUiAction

    data class CameraFailure(
        val error: Throwable,
    ) : ScannerUiAction
}
