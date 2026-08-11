package com.aozijx.passly.feature.recovery.contract

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class RecoveryModeUiState(
    val showSetPasswordDialog: Boolean = false,
    val newPassword: SensitiveValue = EmptySensitiveValue,
    val confirmPassword: SensitiveValue = EmptySensitiveValue,
    val isSettingPassword: Boolean = false,
    val passwordSetupError: String? = null,
)
