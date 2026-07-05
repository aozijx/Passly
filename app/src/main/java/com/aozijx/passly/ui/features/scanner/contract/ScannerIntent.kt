package com.aozijx.passly.ui.features.scanner.contract

import android.content.Context
import android.net.Uri

sealed interface ScannerIntent {
    data class BarcodeDetected(val barcode: String, val context: Context) : ScannerIntent
    data class DecodeImage(val uri: Uri, val context: Context) : ScannerIntent
    data object StartScanning : ScannerIntent
    data object StopScanning : ScannerIntent
}