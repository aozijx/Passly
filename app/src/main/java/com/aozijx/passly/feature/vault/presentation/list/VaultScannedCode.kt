package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.otp.OtpAuthUriCodec
import com.aozijx.passly.domain.entry.otp.OtpUriParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrParseResult
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.domain.entry.policy.WifiQrPayloadParser
import java.net.URI
import java.net.URISyntaxException

internal sealed interface VaultScannedCode {
    data class Otp(
        val config: OtpConfig,
    ) : VaultScannedCode

    data class Wifi(
        val payload: WifiQrPayload,
    ) : VaultScannedCode

    data class WebLink(
        val uri: URI,
    ) : VaultScannedCode

    data object Unsupported : VaultScannedCode
}

internal fun resolveVaultScannedCode(value: String): VaultScannedCode =
    (OtpAuthUriCodec.parse(value) as? OtpUriParseResult.Parsed)
        ?.let { VaultScannedCode.Otp(it.config) }
        ?: (WifiQrPayloadParser.parse(value) as? WifiQrParseResult.Parsed)
            ?.let { VaultScannedCode.Wifi(it.payload) }
        ?: value.toScannedWebUri()?.let(VaultScannedCode::WebLink)
        ?: VaultScannedCode.Unsupported

private fun String.toScannedWebUri(): URI? =
    try {
        URI(this)
            .takeIf { uri ->
                uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true)
            }?.takeIf { !it.host.isNullOrBlank() }
    } catch (_: URISyntaxException) {
        null
    }
