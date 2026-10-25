package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

sealed interface WifiQrFormatResult {
    data class Formatted(val content: String) : WifiQrFormatResult
    data object InvalidCredential : WifiQrFormatResult
}

object WifiQrPayloadFormatter {
    fun format(payload: WifiQrPayload): WifiQrFormatResult {
        val password = if (payload.securityType == WifiSecurityType.Open) "" else payload.password
        if (WifiCredentialValidation.validate(payload.ssid, password, payload.securityType).isNotEmpty()) {
            return WifiQrFormatResult.InvalidCredential
        }
        val security = when (payload.securityType) {
            WifiSecurityType.Open -> "nopass"
            WifiSecurityType.Wep -> "WEP"
            WifiSecurityType.WpaPersonal -> "WPA"
        }
        return WifiQrFormatResult.Formatted(buildString {
            append("WIFI:T:").append(security).append(';')
            append("S:").appendEscaped(payload.ssid).append(';')
            if (payload.securityType != WifiSecurityType.Open) {
                append("P:").appendEscaped(password).append(';')
            }
            append("H:").append(payload.isHidden).append(";;")
        })
    }

    private fun StringBuilder.appendEscaped(value: String): StringBuilder = apply {
        value.forEach { character ->
            if (character in "\\;,:\"") append('\\')
            append(character)
        }
    }
}
