package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import com.aozijx.passly.presentation.mapping.PasswordStrengthState

internal sealed interface UnlockMutation {
    data class KdfParametersReady(
        val parameters: AuthenticationKdfParameters,
    ) : UnlockMutation

    data object KdfInitializationFailed : UnlockMutation

    data class AvailableMethodsChanged(
        val methods: AuthenticationMethods,
    ) : UnlockMutation

    data class RecoveryUnlockVisibilityChanged(
        val visible: Boolean,
    ) : UnlockMutation

    data class AppPasswordChanged(
        val value: SensitiveValue,
    ) : UnlockMutation

    data class RecoveryCodeChanged(
        val value: SensitiveValue,
    ) : UnlockMutation

    data class InputExpansionChanged(
        val expansion: UnlockInputExpansion,
    ) : UnlockMutation

    data class AuthenticationStarted(
        val method: AuthenticationMethod,
    ) : UnlockMutation

    data class AuthenticationFailed(
        val method: AuthenticationMethod,
        val failure: AuthenticationFailure,
    ) : UnlockMutation

    data class AuthenticationAttemptStatusChanged(
        val method: AuthenticationMethod,
        val status: AuthenticationAttemptStatus,
    ) : UnlockMutation

    data object AuthenticationFinished : UnlockMutation

    data object VerificationFailureCleared : UnlockMutation

    data object UnlockInputsReset : UnlockMutation

    data object PasswordSetupOpened : UnlockMutation

    data object PasswordSetupClosed : UnlockMutation

    data class NewAppPasswordChanged(
        val value: SensitiveValue,
        val passwordStrength: PasswordStrengthState,
    ) : UnlockMutation

    data class ConfirmAppPasswordChanged(
        val value: SensitiveValue,
    ) : UnlockMutation

    data object PasswordSetupStarted : UnlockMutation

    data class PasswordSetupFailed(
        val failure: AuthenticationFailure,
    ) : UnlockMutation

    data object PasswordSetupFinished : UnlockMutation

    data object PasswordSetupCompleted : UnlockMutation
}

internal object UnlockReducer {
    fun reduce(
        state: UnlockUiState,
        mutation: UnlockMutation,
    ): UnlockUiState =
        when (mutation) {
            is UnlockMutation.KdfParametersReady -> {
                state.copy(kdfInitialization = KdfInitializationUiState.Ready(mutation.parameters))
            }

            UnlockMutation.KdfInitializationFailed -> {
                state.copy(kdfInitialization = KdfInitializationUiState.Failed)
            }

            is UnlockMutation.AvailableMethodsChanged -> {
                state.copy(availableMethods = mutation.methods)
            }

            is UnlockMutation.RecoveryUnlockVisibilityChanged -> {
                state.copy(recoveryUnlockVisible = mutation.visible)
            }

            is UnlockMutation.AppPasswordChanged -> {
                state.copy(
                    appPassword = mutation.value,
                    verificationPhase = state.verificationPhase.withFailureCleared(),
                )
            }

            is UnlockMutation.RecoveryCodeChanged -> {
                state.copy(
                    recoveryCode = mutation.value,
                    verificationPhase = state.verificationPhase.withFailureCleared(),
                )
            }

            is UnlockMutation.InputExpansionChanged -> {
                state.copy(
                    inputExpansion = mutation.expansion,
                    verificationPhase = state.verificationPhase.withFailureCleared(),
                )
            }

            is UnlockMutation.AuthenticationStarted -> {
                state.copy(
                    verificationPhase = UnlockVerificationPhase.Authenticating(mutation.method),
                )
            }

            is UnlockMutation.AuthenticationFailed -> {
                state.copy(
                    verificationPhase =
                        UnlockVerificationPhase.Failed(
                            UnlockVerificationFailure(
                                method = mutation.method,
                                message = mutation.failure.toVerificationMessage(),
                            ),
                        ),
                )
            }

            is UnlockMutation.AuthenticationAttemptStatusChanged -> {
                state.copy(
                    authenticationAttemptStatuses = state.authenticationAttemptStatuses + (mutation.method to mutation.status),
                    verificationPhase = state.verificationPhase.withAttemptStatus(mutation.method, mutation.status),
                )
            }

            UnlockMutation.AuthenticationFinished -> {
                state.copy(
                    verificationPhase =
                        when (state.verificationPhase) {
                            is UnlockVerificationPhase.Authenticating -> UnlockVerificationPhase.Idle

                            is UnlockVerificationPhase.Failed,
                            UnlockVerificationPhase.Idle,
                            -> state.verificationPhase
                        },
                )
            }

            UnlockMutation.VerificationFailureCleared -> {
                state.copy(
                    verificationPhase = state.verificationPhase.withFailureCleared(),
                )
            }

            UnlockMutation.UnlockInputsReset -> {
                state.copy(
                    appPassword = EmptySensitiveValue,
                    recoveryCode = EmptySensitiveValue,
                    recoveryUnlockVisible = false,
                    inputExpansion = UnlockInputExpansion.Collapsed,
                    verificationPhase = UnlockVerificationPhase.Idle,
                )
            }

            UnlockMutation.PasswordSetupOpened -> {
                if (state.passwordSetup is UnlockPasswordSetup.Saving) return state
                state.copy(passwordSetup = UnlockPasswordSetup.Editing(PasswordSetupDraft()))
            }

            UnlockMutation.PasswordSetupClosed -> {
                if (state.passwordSetup is UnlockPasswordSetup.Saving) return state
                state.copy(passwordSetup = UnlockPasswordSetup.Closed)
            }

            UnlockMutation.PasswordSetupCompleted -> {
                state.copy(
                    passwordSetup = UnlockPasswordSetup.Closed,
                )
            }

            is UnlockMutation.NewAppPasswordChanged -> {
                state.updateEditableDraft(mutation.value) {
                    copy(newPassword = mutation.value, passwordStrength = mutation.passwordStrength)
                }
            }

            is UnlockMutation.ConfirmAppPasswordChanged -> {
                state.updateEditableDraft(mutation.value) { copy(confirmPassword = mutation.value) }
            }

            UnlockMutation.PasswordSetupStarted -> {
                state.updateEditableSetup {
                    UnlockPasswordSetup.Saving(PasswordSetupDraft())
                }
            }

            is UnlockMutation.PasswordSetupFailed -> {
                when (val setup = state.passwordSetup) {
                    is UnlockPasswordSetup.Saving ->
                        state.copy(
                            passwordSetup =
                                UnlockPasswordSetup.Failed(
                                    setup.draft,
                                    mutation.failure.toSetupMessage(),
                                ),
                        )
                    else -> state
                }
            }

            UnlockMutation.PasswordSetupFinished -> {
                when (val setup = state.passwordSetup) {
                    is UnlockPasswordSetup.Saving ->
                        state.copy(passwordSetup = UnlockPasswordSetup.Editing(setup.draft))
                    else -> state
                }
            }
        }

    private fun AuthenticationFailure.toVerificationMessage(): UnlockFailureMessage =
        when (code) {
            AuthenticationFailureCode.CREDENTIAL_INCORRECT -> {
                UnlockFailureMessage.IncorrectCredential(attempts.remaining)
            }

            AuthenticationFailureCode.RATE_LIMITED -> {
                UnlockFailureMessage.RateLimited(
                    retryAfterSeconds = (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L),
                )
            }

            AuthenticationFailureCode.KEY_INVALIDATED -> {
                UnlockFailureMessage.BiometricKeyInvalidated
            }

            AuthenticationFailureCode.BUSY -> {
                UnlockFailureMessage.AuthenticationBusy
            }

            AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE -> {
                UnlockFailureMessage.AttemptStateUnavailable
            }

            AuthenticationFailureCode.HOST_UNAVAILABLE -> {
                UnlockFailureMessage.AuthenticationHostUnavailable
            }

            AuthenticationFailureCode.METHOD_UNAVAILABLE,
            AuthenticationFailureCode.KEY_MISSING,
            -> {
                UnlockFailureMessage.AuthenticationMethodUnavailable
            }

            AuthenticationFailureCode.CRYPTO_OBJECT_INVALID -> {
                UnlockFailureMessage.AuthenticationHostUnavailable
            }

            AuthenticationFailureCode.ENVELOPE_CORRUPTED -> {
                UnlockFailureMessage.SessionUnavailable
            }

            AuthenticationFailureCode.SESSION_MODE_RESTRICTED -> {
                UnlockFailureMessage.AuthenticationRestricted
            }

            AuthenticationFailureCode.SESSION_TRANSITION_FAILED -> {
                UnlockFailureMessage.SessionUnavailable
            }

            else -> {
                UnlockFailureMessage.Generic
            }
        }

    private fun AuthenticationFailure.toSetupMessage(): UnlockFailureMessage =
        when (code) {
            AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION -> {
                UnlockFailureMessage.PasswordTooShort
            }

            AuthenticationFailureCode.RATE_LIMITED -> {
                UnlockFailureMessage.RateLimited(
                    retryAfterSeconds = (((retryAfterMs ?: 0L) + 999L) / 1000L).coerceAtLeast(1L),
                )
            }

            else -> {
                UnlockFailureMessage.AppPasswordSetupFailed
            }
        }
}

private fun UnlockVerificationPhase.withAttemptStatus(
    method: AuthenticationMethod,
    status: AuthenticationAttemptStatus,
): UnlockVerificationPhase {
    val failed = this as? UnlockVerificationPhase.Failed ?: return this
    if (failed.failure.method != method || failed.failure.message !is UnlockFailureMessage.RateLimited) return this
    val retryAfterMs = status.retryAfterMs ?: return UnlockVerificationPhase.Idle
    return failed.copy(
        failure =
            failed.failure.copy(
                message = UnlockFailureMessage.RateLimited(retryAfterMs.toRetryAfterSeconds()),
            ),
    )
}

private fun Long.toRetryAfterSeconds(): Long = ((this + 999L) / 1000L).coerceAtLeast(1L)

private fun UnlockVerificationPhase.withFailureCleared(): UnlockVerificationPhase =
    when (this) {
        is UnlockVerificationPhase.Failed -> UnlockVerificationPhase.Idle

        is UnlockVerificationPhase.Authenticating,
        UnlockVerificationPhase.Idle,
        -> this
    }

private inline fun UnlockUiState.updateEditableDraft(
    value: SensitiveValue,
    transform: PasswordSetupDraft.() -> PasswordSetupDraft,
): UnlockUiState =
    when (val setup = passwordSetup) {
        UnlockPasswordSetup.Closed -> {
            value.wipe()
            this
        }
        is UnlockPasswordSetup.Saving -> {
            value.wipe()
            this
        }
        is UnlockPasswordSetup.Editing -> copy(passwordSetup = UnlockPasswordSetup.Editing(setup.draft.transform()))
        is UnlockPasswordSetup.Failed -> copy(passwordSetup = UnlockPasswordSetup.Editing(setup.draft.transform()))
    }

private inline fun UnlockUiState.updateEditableSetup(
    transform: (PasswordSetupDraft) -> UnlockPasswordSetup,
): UnlockUiState =
    when (val setup = passwordSetup) {
        UnlockPasswordSetup.Closed -> this
        is UnlockPasswordSetup.Editing -> copy(passwordSetup = transform(setup.draft))
        is UnlockPasswordSetup.Failed -> copy(passwordSetup = transform(setup.draft))
        is UnlockPasswordSetup.Saving -> this
    }
