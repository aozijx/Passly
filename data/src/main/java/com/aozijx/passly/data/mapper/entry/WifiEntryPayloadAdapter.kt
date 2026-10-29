package com.aozijx.passly.data.mapper.entry

import com.aozijx.passly.data.codec.entry.payload.SecretPayload
import com.aozijx.passly.data.codec.entry.payload.WifiCredentialPayload
import com.aozijx.passly.domain.entry.model.credential.EntryCredential
import com.aozijx.passly.domain.entry.model.credential.EntryCredentialKind
import com.aozijx.passly.domain.entry.model.credential.WifiCredential
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityTypeParseResult

internal class WifiEntryPayloadAdapter : EntryPayloadAdapter {
    override val kind: EntryCredentialKind = EntryCredentialKind.WIFI

    override fun encode(credential: EntryCredential): SecretPayload {
        require(credential is WifiCredential) { "WIFI adapter requires WifiCredential" }
        return SecretPayload(wifi = WifiCredentialPayload(
            ssid = credential.ssid,
            password = credential.password,
            securityType = credential.securityType.storedValue,
            isHidden = credential.isHidden,
        ))
    }

    override fun decode(payload: SecretPayload): EntryCredential {
        val wifi = requireNotNull(payload.wifi) { "Wi-Fi payload is missing" }
        return WifiCredential(
            ssid = wifi.ssid,
            password = wifi.password,
            securityType = wifi.securityType.requireWifiSecurityType(),
            isHidden = wifi.isHidden,
        )
    }

    private fun String?.requireWifiSecurityType(): WifiSecurityType =
        when (val result = WifiSecurityType.parseStoredValue(this)) {
            is WifiSecurityTypeParseResult.Supported -> result.value
            WifiSecurityTypeParseResult.Missing ->
                throw IllegalArgumentException("Wi-Fi security type is missing")
            is WifiSecurityTypeParseResult.Unsupported ->
                throw IllegalArgumentException("Unsupported Wi-Fi security type: ${result.storedValue}")
        }
}
