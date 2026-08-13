package com.aozijx.passly.feature.scanner.contract

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

sealed interface ScannerEffect {
    data class ScanSuccess(
        val result: String,
        val otpConfig: OtpConfig?
    ) : ScannerEffect

    data class ShowError(val message: String) : ScannerEffect
}
