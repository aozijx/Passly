package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyCredentialPayload(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null,
)
