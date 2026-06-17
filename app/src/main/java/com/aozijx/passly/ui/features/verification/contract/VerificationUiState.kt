package com.aozijx.passly.ui.features.verification.contract

import com.aozijx.passly.core.crypto.memory.SecureString

data class VerificationUiState(
    val authInProgress: Boolean = false,
    val appPassword: SecureString = SecureString.EMPTY,
    val appPasswordConfirm: SecureString = SecureString.EMPTY,
    val showPasswordInput: Boolean = false,
    val showSetPasswordDialog: Boolean = false
)