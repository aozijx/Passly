package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class WifiPayload(
    val securityType: String? = "WPA",
    val isHidden: Boolean = false
)