package com.aozijx.passly.feature.scanner.presentation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.policy.WifiQrPayload

sealed interface ScannerImport {
    data class Otp(val config: OtpConfig) : ScannerImport
    data class Wifi(val payload: WifiQrPayload) : ScannerImport
}

sealed interface ScannerImportResolution {
    data class Available(val value: ScannerImport) : ScannerImportResolution
    data object Unavailable : ScannerImportResolution
}

sealed interface ScannerImportHandlingResult {
    data object Applied : ScannerImportHandlingResult
    data object Rejected : ScannerImportHandlingResult
}
