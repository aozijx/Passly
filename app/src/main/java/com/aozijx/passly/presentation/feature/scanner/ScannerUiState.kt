package com.aozijx.passly.presentation.feature.scanner

data class ScannerUiState(
    val isScanning: Boolean = true,
    val error: String? = null
)