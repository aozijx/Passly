package com.aozijx.passly.data.codec.entry.payload

import kotlinx.serialization.Serializable

@Serializable
data class IdentityCredentialPayload(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList(),
)
