package com.aozijx.passly.feature.auth.contract

import com.aozijx.passly.domain.authentication.AuthenticationFailure
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.security.crypto.SecureString

data class AuthenticationVerificationFailure(
    val method: AuthenticationMethod,
    val failure: AuthenticationFailure
)

data class AuthenticationUiState(
    val showSetPasswordDialog: Boolean = false,
    val newAppPassword: SecureString = SecureString.EMPTY,
    val confirmAppPassword: SecureString = SecureString.EMPTY,
    val isSettingAppPassword: Boolean = false,
    val appPassword: SecureString = SecureString.EMPTY,
    val recoveryCode: SecureString = SecureString.EMPTY,
    val recoveryUnlockVisible: Boolean = false,
    val expandedMethod: AuthenticationMethod? = null,
    val activeMethod: AuthenticationMethod? = null,
    val verificationFailure: AuthenticationVerificationFailure? = null,
    val setupFailure: AuthenticationFailure? = null
)
