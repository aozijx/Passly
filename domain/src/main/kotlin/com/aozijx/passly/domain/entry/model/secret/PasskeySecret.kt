package com.aozijx.passly.domain.entry.model.secret

data class PasskeySecret(
    val credentialId: String? = null,
    val rpId: String? = null,
    val userHandle: String? = null,
    val privateKeyReference: String? = null,
    val hardwareKeyInfo: String? = null
)
