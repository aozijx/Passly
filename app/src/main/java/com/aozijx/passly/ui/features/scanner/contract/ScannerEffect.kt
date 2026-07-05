package com.aozijx.passly.ui.features.scanner.contract

sealed interface ScannerEffect {
    data class ScanSuccess(val result: String) : ScannerEffect
    data class ShowError(val message: String) : ScannerEffect
}