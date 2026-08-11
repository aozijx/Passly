package com.aozijx.passly.feature.recovery.contract

data class RecoveryModeUiState(
    val showSetPasswordDialog: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSettingPassword: Boolean = false,
    val passwordSetupError: String? = null,
)
