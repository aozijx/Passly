package com.aozijx.passly.feature.scanner.presentation

sealed interface ScannerEffect {
    data object CopySucceeded : ScannerEffect
    data class ShowError(val message: String) : ScannerEffect
}
