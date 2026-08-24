package com.aozijx.passly.presentation.feature.onboarding

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class BootstrapUiState(
    val showSetPasswordDialog: Boolean = false,
    val newAppPassword: SensitiveValue = EmptySensitiveValue,
    val confirmAppPassword: SensitiveValue = EmptySensitiveValue,
    val isSettingAppPassword: Boolean = false,
    val setupFailure: AuthenticationFailure? = null
)
