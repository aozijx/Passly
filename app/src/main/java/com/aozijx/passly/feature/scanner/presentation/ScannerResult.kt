package com.aozijx.passly.feature.scanner.presentation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import java.net.URI

sealed interface ScannerResult {
    val rawValue: String

    data class Otp(
        override val rawValue: String,
        val config: OtpConfig,
    ) : ScannerResult

    data class Wifi(
        override val rawValue: String,
        val payload: WifiQrPayload,
    ) : ScannerResult

    data class WebLink(
        override val rawValue: String,
        val uri: URI,
    ) : ScannerResult

    data class PlainText(
        override val rawValue: String,
    ) : ScannerResult
}
