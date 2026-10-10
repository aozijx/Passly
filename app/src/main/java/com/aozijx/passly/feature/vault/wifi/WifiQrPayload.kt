package com.aozijx.passly.feature.vault.wifi

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

data class WifiQrPayload(
    val ssid: String,
    val password: String,
    val securityType: WifiSecurityType,
    val isHidden: Boolean,
)
