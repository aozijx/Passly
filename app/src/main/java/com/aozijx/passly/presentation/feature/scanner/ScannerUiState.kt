package com.aozijx.passly.presentation.feature.scanner

data class ScannerUiState(
    val isScanning: Boolean = true,
    val result: ScannerResult? = null,
)
