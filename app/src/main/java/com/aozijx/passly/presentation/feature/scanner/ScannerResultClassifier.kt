package com.aozijx.passly.presentation.feature.scanner

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.feature.vault.otp.OtpAuthUriCodec
import java.net.URI

internal object ScannerResultClassifier {
    fun classify(rawValue: String): ScannerResult? =
        classify(rawValue, OtpAuthUriCodec::parse)

    fun classify(
        rawValue: String,
        otpParser: (String) -> OtpConfig?,
    ): ScannerResult? {
        if (rawValue.isBlank()) return null

        runCatching { otpParser(rawValue) }
            .getOrNull()
            ?.let { return ScannerResult.Otp(rawValue, it) }

        val uri = runCatching { URI(rawValue) }.getOrNull()
        if (
            uri != null &&
            (uri.scheme.equals("http", ignoreCase = true) ||
                uri.scheme.equals("https", ignoreCase = true)) &&
            !uri.host.isNullOrBlank()
        ) {
            return ScannerResult.WebLink(rawValue, uri)
        }

        return ScannerResult.PlainText(rawValue)
    }
}
