package com.aozijx.passly.feature.scanner.contract

data class ScannerUiState(
    val isScanning: Boolean = true,
    val error: String? = null
)