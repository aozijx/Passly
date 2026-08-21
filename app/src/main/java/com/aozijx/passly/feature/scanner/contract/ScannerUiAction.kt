package com.aozijx.passly.feature.scanner.contract

sealed interface ScannerUiAction {
    data class BarcodeDetected(val barcode: String) : ScannerUiAction
    data class DecodeImage(val image: ImageRef) : ScannerUiAction
    data object StartScanning : ScannerUiAction
    data object StopScanning : ScannerUiAction
}
