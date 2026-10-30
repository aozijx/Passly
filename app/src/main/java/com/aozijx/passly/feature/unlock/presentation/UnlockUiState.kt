package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

sealed interface UnlockFailureMessage {
    data class IncorrectCredential(val remainingAttempts: Int?) : UnlockFailureMessage
    data class RateLimited(val retryAfterSeconds: Long) : UnlockFailureMessage
    data object BiometricKeyInvalidated : UnlockFailureMessage
    data object PasswordTooShort : UnlockFailureMessage
    data object AppPasswordSetupFailed : UnlockFailureMessage
    data object Generic : UnlockFailureMessage
}

data class UnlockVerificationFailure(
    val method: AuthenticationMethod,
    val message: UnlockFailureMessage,
)

sealed interface UnlockInputExpansion {
    data object Collapsed : UnlockInputExpansion
    data class Expanded(val method: AuthenticationMethod) : UnlockInputExpansion
}

sealed interface UnlockVerificationPhase {
    data object Idle : UnlockVerificationPhase
    data class Authenticating(val method: AuthenticationMethod) : UnlockVerificationPhase
    data class Failed(val failure: UnlockVerificationFailure) : UnlockVerificationPhase
}

sealed interface UnlockPasswordSetup {
    data object Closed : UnlockPasswordSetup
    data class Editing(
        val newPassword: SensitiveValue = EmptySensitiveValue,
        val confirmPassword: SensitiveValue = EmptySensitiveValue,
        val phase: UnlockPasswordSetupPhase = UnlockPasswordSetupPhase.Idle,
    ) : UnlockPasswordSetup
}

sealed interface UnlockPasswordSetupPhase {
    data object Idle : UnlockPasswordSetupPhase
    data object Saving : UnlockPasswordSetupPhase
    data class Failed(val message: UnlockFailureMessage) : UnlockPasswordSetupPhase
}

data class UnlockUiState(
    val availableMethods: AuthenticationMethods = AuthenticationMethods(),
    val appPassword: SensitiveValue = EmptySensitiveValue,
    val recoveryCode: SensitiveValue = EmptySensitiveValue,
    val recoveryUnlockVisible: Boolean = false,
    val inputExpansion: UnlockInputExpansion = UnlockInputExpansion.Collapsed,
    val verificationPhase: UnlockVerificationPhase = UnlockVerificationPhase.Idle,
    val passwordSetup: UnlockPasswordSetup = UnlockPasswordSetup.Closed,
)

internal fun UnlockPasswordSetup.Editing.canSubmit(): Boolean {
    val passwordChars = newPassword.toCharArray()
    val confirmationChars = confirmPassword.toCharArray()
    return try {
        phase != UnlockPasswordSetupPhase.Saving &&
            AppPasswordPolicy.DEFAULT.acceptsLength(passwordChars.size) &&
            passwordChars.contentEquals(confirmationChars)
    } finally {
        MemoryCleaner.wipeCharArray(passwordChars)
        MemoryCleaner.wipeCharArray(confirmationChars)
    }
}
