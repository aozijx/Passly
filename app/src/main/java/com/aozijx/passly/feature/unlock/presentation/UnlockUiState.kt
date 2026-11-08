package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft

sealed interface UnlockFailureMessage {
    data class IncorrectCredential(
        val remainingAttempts: Int?,
    ) : UnlockFailureMessage

    data class RateLimited(
        val retryAfterSeconds: Long,
    ) : UnlockFailureMessage

    data object BiometricKeyInvalidated : UnlockFailureMessage

    data object BiometricAuthenticationFailed : UnlockFailureMessage

    data object BiometricRateLimited : UnlockFailureMessage

    data object AuthenticationBusy : UnlockFailureMessage

    data object AuthenticationHostUnavailable : UnlockFailureMessage

    data object AuthenticationMethodUnavailable : UnlockFailureMessage

    data object AuthenticationRestricted : UnlockFailureMessage

    data object SessionUnavailable : UnlockFailureMessage

    data object AttemptStateUnavailable : UnlockFailureMessage

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

    data class Expanded(
        val method: AuthenticationMethod,
    ) : UnlockInputExpansion
}

sealed interface UnlockVerificationPhase {
    data object Idle : UnlockVerificationPhase

    data class Authenticating(
        val method: AuthenticationMethod,
    ) : UnlockVerificationPhase

    data class Failed(
        val failure: UnlockVerificationFailure,
    ) : UnlockVerificationPhase
}

sealed interface UnlockPasswordSetup {
    data object Closed : UnlockPasswordSetup

    data class Editing(val draft: PasswordSetupDraft) : UnlockPasswordSetup

    data class Saving(val draft: PasswordSetupDraft) : UnlockPasswordSetup

    data class Failed(
        val draft: PasswordSetupDraft,
        val message: UnlockFailureMessage,
    ) : UnlockPasswordSetup
}

sealed interface KdfInitializationUiState {
    data object Loading : KdfInitializationUiState

    data class Ready(
        val parameters: AuthenticationKdfParameters,
    ) : KdfInitializationUiState

    data object Failed : KdfInitializationUiState
}

data class UnlockUiState(
    val kdfInitialization: KdfInitializationUiState = KdfInitializationUiState.Loading,
    val availableMethods: AuthenticationMethods = AuthenticationMethods(),
    val authenticationAttemptStatuses: Map<AuthenticationMethod, AuthenticationAttemptStatus> = emptyMap(),
    val appPassword: SensitiveValue = EmptySensitiveValue,
    val recoveryCode: SensitiveValue = EmptySensitiveValue,
    val recoveryUnlockVisible: Boolean = false,
    val inputExpansion: UnlockInputExpansion = UnlockInputExpansion.Collapsed,
    val verificationPhase: UnlockVerificationPhase = UnlockVerificationPhase.Idle,
    val passwordSetup: UnlockPasswordSetup = UnlockPasswordSetup.Closed,
) {
    val shouldOfferPasswordSetup: Boolean get() = availableMethods.available.isEmpty()
    val canSubmitAppPassword: Boolean get() = !appPassword.isEmpty
    val canSubmitRecoveryCode: Boolean get() = !recoveryCode.isEmpty
}

internal fun Long.toRetryAfterSeconds(): Long = ((this + 999L) / 1000L).coerceAtLeast(1L)
