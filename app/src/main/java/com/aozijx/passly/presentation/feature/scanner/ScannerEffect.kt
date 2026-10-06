package com.aozijx.passly.presentation.feature.scanner

sealed interface ScannerEffect {
    data object CopySucceeded : ScannerEffect
    data class ShowError(val message: String) : ScannerEffect
}
