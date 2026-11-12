package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
internal data class OtpCredentialPayload(
    val config: OtpConfigPayload? = null,
)
