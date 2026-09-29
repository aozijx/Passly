package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig

internal sealed interface ScannerMutation {
    data object Started : ScannerMutation
    data object Stopped : ScannerMutation
    data class ScanCompleted(
        val result: String,
        val otpConfig: OtpConfig?,
    ) : ScannerMutation
}

internal object ScannerReducer {
    fun reduce(state: ScannerUiState, mutation: ScannerMutation): ScannerUiState =
        when (mutation) {
            ScannerMutation.Started -> ScannerUiState()
            ScannerMutation.Stopped -> ScannerUiState(isScanning = false)
            is ScannerMutation.ScanCompleted -> state.copy(
                isScanning = false,
                scanResult = mutation.result,
                scannedOtp = mutation.otpConfig,
            )
        }
}
