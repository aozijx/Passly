package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class WifiCredentialPayload(
    val ssid: String,
    val password: String? = null,
    val securityType: String? = null,
    val isHidden: Boolean = false,
)
