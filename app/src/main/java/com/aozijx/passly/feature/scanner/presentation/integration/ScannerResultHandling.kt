package com.aozijx.passly.feature.scanner.presentation.integration

sealed interface ScannerResultHandling {
    data object Handled : ScannerResultHandling

    data object Rejected : ScannerResultHandling

    data class Failed(
        val error: Exception,
    ) : ScannerResultHandling
}
