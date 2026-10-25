package com.aozijx.passly.feature.scanner.presentation

import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadParser
import java.net.URI
import java.net.URISyntaxException

internal object ScannerResultClassifier {
    fun classify(rawValue: String): ScannerClassification {
        if (rawValue.isBlank()) return ScannerClassification.Empty

        when (val otp = OtpAuthUriCodec.parse(rawValue)) {
            is OtpUriParseResult.Parsed ->
                return ScannerClassification.Classified(ScannerResult.Otp(rawValue, otp.config))
            OtpUriParseResult.NotOtp,
            OtpUriParseResult.Malformed,
            OtpUriParseResult.Unsupported -> Unit
        }

        when (val wifi = WifiQrPayloadParser.parse(rawValue)) {
            is WifiQrParseResult.Parsed ->
                return ScannerClassification.Classified(
                    ScannerResult.Wifi(rawValue, wifi.payload),
                )
            WifiQrParseResult.NotWifi,
            WifiQrParseResult.Malformed,
            WifiQrParseResult.Unsupported,
            -> Unit
        }

        val uri = try {
            URI(rawValue)
        } catch (_: URISyntaxException) {
            null
        }
        if (
            uri != null &&
            (uri.scheme.equals("http", ignoreCase = true) ||
                uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
        ) {
            return ScannerClassification.Classified(ScannerResult.WebLink(rawValue, uri))
        }

        return ScannerClassification.Classified(ScannerResult.PlainText(rawValue))
    }
}

internal sealed interface ScannerClassification {
    data object Empty : ScannerClassification
    data class Classified(val result: ScannerResult) : ScannerClassification
}
