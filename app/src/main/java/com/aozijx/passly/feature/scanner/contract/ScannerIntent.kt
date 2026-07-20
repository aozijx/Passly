package com.aozijx.passly.feature.scanner.contract

import android.net.Uri

sealed interface ScannerIntent {
    data class BarcodeDetected(val barcode: String) : ScannerIntent
    data class DecodeImage(val uri: Uri) : ScannerIntent
    data object StartScanning : ScannerIntent
    data object StopScanning : ScannerIntent
}