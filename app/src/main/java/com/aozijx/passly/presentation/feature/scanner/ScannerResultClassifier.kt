package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.otp.OtpAuthUriCodec
import com.aozijx.passly.feature.vault.wifi.WifiQrParseResult
import com.aozijx.passly.feature.vault.wifi.WifiQrPayloadParser
import java.net.URI

internal object ScannerResultClassifier {
    fun classify(rawValue: String): ScannerClassification =
        classify(rawValue, OtpAuthUriCodec::parse)

    fun classify(
        rawValue: String,
        otpParser: (String) -> OtpConfig?,
    ): ScannerClassification {
        if (rawValue.isBlank()) return ScannerClassification.Empty

        runCatching { otpParser(rawValue) }
            .getOrNull()
            ?.let {
                return ScannerClassification.Classified(ScannerResult.Otp(rawValue, it))
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

        val uri = runCatching { URI(rawValue) }.getOrNull()
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
