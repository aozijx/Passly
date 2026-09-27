package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

sealed interface UnlockFailureMessage {
    data class IncorrectCredential(val remainingAttempts: Int?) : UnlockFailureMessage
    data class RateLimited(val retryAfterSeconds: Long) : UnlockFailureMessage
    data object PasswordTooShort : UnlockFailureMessage
    data object AppPasswordSetupFailed : UnlockFailureMessage
    data object Generic : UnlockFailureMessage
}

data class UnlockVerificationFailure(
    val method: AuthenticationMethod,
    val message: UnlockFailureMessage,
)

data class UnlockUiState(
    val availableMethods: AuthenticationMethods = AuthenticationMethods(),
    val appPassword: SensitiveValue = EmptySensitiveValue,
    val recoveryCode: SensitiveValue = EmptySensitiveValue,
    val recoveryUnlockVisible: Boolean = false,
    val expandedMethod: AuthenticationMethod? = null,
    val activeMethod: AuthenticationMethod? = null,
    val verificationFailure: UnlockVerificationFailure? = null,
    val showSetPasswordDialog: Boolean = false,
    val newAppPassword: SensitiveValue = EmptySensitiveValue,
    val confirmAppPassword: SensitiveValue = EmptySensitiveValue,
    val canConfirmAppPassword: Boolean = false,
    val isSettingAppPassword: Boolean = false,
    val setupFailure: UnlockFailureMessage? = null,
)
