package com.aozijx.passly.feature.scanner.presentation

sealed interface ScannerEffect {
    data object ScanSucceeded : ScannerEffect
    data object CopySucceeded : ScannerEffect
    data object ImageNoBarcode : ScannerEffect
    data object ImageDecodeFailed : ScannerEffect
}
