package com.aozijx.passly.domain.model.entry.secret

data class PasskeySecret(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
)
