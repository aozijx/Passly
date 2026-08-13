package com.aozijx.passly.feature.auth.contract

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class AuthenticationVerificationFailure(
    val method: AuthenticationMethod,
    val failure: AuthenticationFailure
)

data class AuthenticationUiState(
    val showSetPasswordDialog: Boolean = false,
    val newAppPassword: SensitiveValue = EmptySensitiveValue,
    val confirmAppPassword: SensitiveValue = EmptySensitiveValue,
    val isSettingAppPassword: Boolean = false,
    val appPassword: SensitiveValue = EmptySensitiveValue,
    val recoveryCode: SensitiveValue = EmptySensitiveValue,
    val recoveryUnlockVisible: Boolean = false,
    val expandedMethod: AuthenticationMethod? = null,
    val activeMethod: AuthenticationMethod? = null,
    val verificationFailure: AuthenticationVerificationFailure? = null,
    val setupFailure: AuthenticationFailure? = null
)
