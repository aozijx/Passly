package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class SshPayload(
    val privateKey: String? = null,
    val seedPhrase: String? = null
)
