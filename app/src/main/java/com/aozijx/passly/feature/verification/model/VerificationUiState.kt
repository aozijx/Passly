package com.aozijx.passly.feature.verification.model

import com.aozijx.passly.security.crypto.SecureString

data class VerificationUiState(
    val authInProgress: Boolean = false,
    val appPassword: SecureString = SecureString.EMPTY,
    val appPasswordConfirm: SecureString = SecureString.EMPTY,
    val recoveryCode: SecureString = SecureString.EMPTY,
    val recoveryCodeAvailable: Boolean = false,
    val showPasswordInput: Boolean = false,
    val showSetPasswordDialog: Boolean = false,
    val showRecoveryCodeInput: Boolean = false
)
