package com.aozijx.passly.domain.access.model

data class AuthenticationCredentialSnapshot(
    val biometricConfigured: Boolean = false,
    val appPasswordConfigured: Boolean = false,
    val recoveryCodeConfigured: Boolean = false,
)
