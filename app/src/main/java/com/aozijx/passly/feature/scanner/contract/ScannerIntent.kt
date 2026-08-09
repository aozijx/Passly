package com.aozijx.passly.feature.scanner.contract

sealed interface ScannerIntent {
    data class BarcodeDetected(val barcode: String) : ScannerIntent
    data class DecodeImage(val image: ImageRef) : ScannerIntent
    data object StartScanning : ScannerIntent
    data object StopScanning : ScannerIntent
}