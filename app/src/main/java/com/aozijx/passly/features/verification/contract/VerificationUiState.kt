package com.aozijx.passly.features.verification.contract

data class VerificationUiState(
    val authInProgress: Boolean = false,
    val appPassword: String = "",
    val appPasswordConfirm: String = "",
    val showPasswordInput: Boolean = false,
    val showSetPasswordDialog: Boolean = false
)