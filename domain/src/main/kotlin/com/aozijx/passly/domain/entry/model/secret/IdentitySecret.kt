package com.aozijx.passly.domain.entry.model.secret

data class IdentitySecret(
    val idNumber: String? = null,
    val securityQuestion: String? = null,
    val securityAnswer: String? = null,
    val seedPhrase: String? = null,
    val recoveryCodes: List<String> = emptyList()
)
