package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

data class ScannerUiState(
    val isScanning: Boolean = true,
    val scanResult: String = "",
    val scannedOtp: OtpConfig? = null,
)
