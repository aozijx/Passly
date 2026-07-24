package com.aozijx.passly.feature.auth.presentation

import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.security.crypto.SecureString

data class AuthenticationUiState(
    val showSetPasswordDialog: Boolean = false,
    val newAppPassword: SecureString = SecureString.EMPTY,
    val confirmAppPassword: SecureString = SecureString.EMPTY,
    val isSettingAppPassword: Boolean = false,
    val appPassword: SecureString = SecureString.EMPTY,
    val recoveryCode: SecureString = SecureString.EMPTY,
    val expandedMethod: AuthenticationMethod? = null,
    val activeMethod: AuthenticationMethod? = null
)
