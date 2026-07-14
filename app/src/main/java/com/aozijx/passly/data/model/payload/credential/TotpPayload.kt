package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class TotpPayload(
    val secret: String? = null,
    val issuer: String? = null,
    val period: Int = 30,
    val digits: Int = 6,
    val algorithm: String = "SHA1"
)
