package com.aozijx.passly.domain.entry.policy

import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType

data class WifiQrPayload(
    val ssid: String,
    val password: String,
    val securityType: WifiSecurityType,
    val isHidden: Boolean,
)
