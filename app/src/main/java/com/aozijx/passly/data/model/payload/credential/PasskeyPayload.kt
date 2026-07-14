package com.aozijx.passly.data.model.payload.credential

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyPayload(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val recoveryCodes: String? = null,
    val hardwareKeyInfo: String? = null
)