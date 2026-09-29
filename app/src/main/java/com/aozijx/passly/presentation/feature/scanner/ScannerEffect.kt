package com.aozijx.passly.presentation.feature.scanner

sealed interface ScannerEffect {
    data object UnsupportedOtp : ScannerEffect
    data class ShowError(val message: String) : ScannerEffect
}
