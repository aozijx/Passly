package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationKdfParameters
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.access.model.AttemptStatus
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationReducerTest {
    @Test
    fun `available authentication methods are part of authoritative ui state`() {
        val methods =
            AuthenticationMethods(
                setOf(
                    AuthenticationMethod.BIOMETRIC,
                    AuthenticationMethod.APP_PASSWORD,
                ),
            )

        val result =
            UnlockReducer.reduce(
                UnlockUiState(),
                UnlockMutation.AvailableMethodsChanged(methods),
            )

        assertEquals(methods, result.availableMethods)
    }

    @Test
    fun `password confirmation follows one derived policy`() {
        val shortPassword = OwnedChars.fromString("short")
        val validPassword = OwnedChars.fromString("long-enough-password")
        val matchingConfirmation = OwnedChars.fromString("long-enough-password")
        try {
            val short =
                UnlockReducer.reduce(
                    UnlockUiState(passwordSetup = UnlockPasswordSetup.Editing(PasswordSetupDraft())),
                    UnlockMutation.NewAppPasswordChanged(
                        value = shortPassword,
                        passwordStrength = PasswordStrengthEvaluator.evaluate("short"),
                    ),
                )
            val withPassword =
                UnlockReducer.reduce(
                    short,
                    UnlockMutation.NewAppPasswordChanged(
                        value = validPassword,
                        passwordStrength = PasswordStrengthEvaluator.evaluate("long-enough-password"),
                    ),
                )
            val matching =
                UnlockReducer.reduce(
                    withPassword,
                    UnlockMutation.ConfirmAppPasswordChanged(matchingConfirmation),
                )

            assertFalse((short.passwordSetup as UnlockPasswordSetup.Editing).draft.canSubmit())
            assertFalse((withPassword.passwordSetup as UnlockPasswordSetup.Editing).draft.canSubmit())
            assertTrue((matching.passwordSetup as UnlockPasswordSetup.Editing).draft.canSubmit())
        } finally {
            shortPassword.wipe()
            validPassword.wipe()
            matchingConfirmation.wipe()
        }
    }

    @Test
    fun `authentication lifecycle preserves authoritative failure`() {
        val failure =
            AuthenticationFailure(
                code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
            )
        val started =
            UnlockReducer.reduce(
                UnlockUiState(),
                UnlockMutation.AuthenticationStarted(AuthenticationMethod.APP_PASSWORD),
            )
        val failed =
            UnlockReducer.reduce(
                started,
                UnlockMutation.AuthenticationFailed(
                    method = AuthenticationMethod.APP_PASSWORD,
                    failure = failure,
                ),
            )
        val finished =
            UnlockReducer.reduce(
                failed,
                UnlockMutation.AuthenticationFinished,
            )

        assertEquals(
            UnlockVerificationPhase.Authenticating(AuthenticationMethod.APP_PASSWORD),
            started.verificationPhase,
        )
        val finalFailure = finished.verificationPhase as UnlockVerificationPhase.Failed
        assertEquals(
            UnlockFailureMessage.IncorrectCredential(remainingAttempts = null),
            finalFailure.failure.message,
        )
    }

    @Test
    fun `invalidated biometric key gets a recovery-specific failure instead of generic verification text`() {
        val started =
            UnlockReducer.reduce(
                UnlockUiState(),
                UnlockMutation.AuthenticationStarted(AuthenticationMethod.BIOMETRIC),
            )

        val failed =
            UnlockReducer.reduce(
                started,
                UnlockMutation.AuthenticationFailed(
                    method = AuthenticationMethod.BIOMETRIC,
                    failure = AuthenticationFailure(AuthenticationFailureCode.KEY_INVALIDATED),
                ),
            )

        val failure = (failed.verificationPhase as UnlockVerificationPhase.Failed).failure
        assertEquals(UnlockFailureMessage.BiometricKeyInvalidated, failure.message)
    }

    @Test
    fun `biometric failures map to biometric messages without counting attempts`() {
        val incorrect =
            UnlockReducer.reduce(
                UnlockReducer.reduce(
                    UnlockUiState(),
                    UnlockMutation.AuthenticationStarted(AuthenticationMethod.BIOMETRIC),
                ),
                UnlockMutation.AuthenticationFailed(
                    method = AuthenticationMethod.BIOMETRIC,
                    failure = AuthenticationFailure(AuthenticationFailureCode.CREDENTIAL_INCORRECT),
                ),
            )
        assertEquals(
            UnlockFailureMessage.BiometricAuthenticationFailed,
            (incorrect.verificationPhase as UnlockVerificationPhase.Failed).failure.message,
        )

        val rateLimited =
            UnlockReducer.reduce(
                UnlockReducer.reduce(
                    UnlockUiState(),
                    UnlockMutation.AuthenticationStarted(AuthenticationMethod.BIOMETRIC),
                ),
                UnlockMutation.AuthenticationFailed(
                    method = AuthenticationMethod.BIOMETRIC,
                    failure = AuthenticationFailure(AuthenticationFailureCode.RATE_LIMITED, retryAfterMs = 30_000L),
                ),
            )
        assertEquals(
            UnlockFailureMessage.BiometricRateLimited,
            (rateLimited.verificationPhase as UnlockVerificationPhase.Failed).failure.message,
        )
    }

    @Test
    fun `unlock reports remaining password attempts`() {
        val started =
            UnlockReducer.reduce(
                UnlockUiState(),
                UnlockMutation.AuthenticationStarted(AuthenticationMethod.APP_PASSWORD),
            )

        val failed =
            UnlockReducer.reduce(
                started,
                UnlockMutation.AuthenticationFailed(
                    method = AuthenticationMethod.APP_PASSWORD,
                    failure =
                        AuthenticationFailure(
                            code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
                            method = AuthenticationMethod.APP_PASSWORD,
                            attempts = AttemptStatus(used = 2, limit = 5),
                        ),
                ),
            )

        val failure = (failed.verificationPhase as UnlockVerificationPhase.Failed).failure
        assertEquals(UnlockFailureMessage.IncorrectCredential(remainingAttempts = 3), failure.message)
    }

    @Test
    fun `unlock maps current authentication failure codes to supported messages`() {
        val expectedMessages =
            mapOf(
                AuthenticationFailureCode.BUSY to UnlockFailureMessage.AuthenticationBusy,
                AuthenticationFailureCode.ATTEMPT_STATE_UNAVAILABLE to UnlockFailureMessage.AttemptStateUnavailable,
                AuthenticationFailureCode.HOST_UNAVAILABLE to UnlockFailureMessage.AuthenticationHostUnavailable,
                AuthenticationFailureCode.METHOD_UNAVAILABLE to UnlockFailureMessage.AuthenticationMethodUnavailable,
                AuthenticationFailureCode.KEY_MISSING to UnlockFailureMessage.AuthenticationMethodUnavailable,
                AuthenticationFailureCode.KEY_INVALIDATED to UnlockFailureMessage.BiometricKeyInvalidated,
                AuthenticationFailureCode.CRYPTO_OBJECT_INVALID to UnlockFailureMessage.AuthenticationHostUnavailable,
                AuthenticationFailureCode.CREDENTIAL_INCORRECT to UnlockFailureMessage.IncorrectCredential(null),
                AuthenticationFailureCode.PASSWORD_POLICY_VIOLATION to UnlockFailureMessage.Generic,
                AuthenticationFailureCode.ENVELOPE_CORRUPTED to UnlockFailureMessage.SessionUnavailable,
                AuthenticationFailureCode.LAST_METHOD_REQUIRED to UnlockFailureMessage.Generic,
                AuthenticationFailureCode.RATE_LIMITED to UnlockFailureMessage.RateLimited(1L),
                AuthenticationFailureCode.SESSION_MODE_RESTRICTED to UnlockFailureMessage.AuthenticationRestricted,
                AuthenticationFailureCode.SESSION_TRANSITION_FAILED to UnlockFailureMessage.SessionUnavailable,
            )

        expectedMessages.forEach { (code, expectedMessage) ->
            val started =
                UnlockReducer.reduce(
                    UnlockUiState(),
                    UnlockMutation.AuthenticationStarted(AuthenticationMethod.APP_PASSWORD),
                )
            val failed =
                UnlockReducer.reduce(
                    started,
                    UnlockMutation.AuthenticationFailed(
                        method = AuthenticationMethod.APP_PASSWORD,
                        failure = AuthenticationFailure(code),
                    ),
                )

            val actualMessage = (failed.verificationPhase as UnlockVerificationPhase.Failed).failure.message
            assertEquals("Unexpected message for $code", expectedMessage, actualMessage)
        }
    }

    @Test
    fun `resetting unlock inputs clears entered values and preserves availability state`() {
        val appPassword = OwnedChars.fromString("password-input")
        val recoveryCode = OwnedChars.fromString("recovery-input")
        val kdfState =
            KdfInitializationUiState.Ready(
                AuthenticationKdfParameters(0x13, 65_536, 3, 4, 256),
            )
        val availableMethods = AuthenticationMethods(setOf(AuthenticationMethod.APP_PASSWORD))
        val attemptStatus = AuthenticationAttemptStatus(AttemptStatus(used = 2, limit = 5), retryAfterMs = 1_000L)
        val initial =
            UnlockUiState(
                kdfInitialization = kdfState,
                availableMethods = availableMethods,
                authenticationAttemptStatuses = mapOf(AuthenticationMethod.APP_PASSWORD to attemptStatus),
                appPassword = appPassword,
                recoveryCode = recoveryCode,
                recoveryUnlockVisible = true,
                inputExpansion = AuthenticationMethod.RECOVERY_CODE,
                verificationPhase =
                    UnlockVerificationPhase.Failed(
                        UnlockVerificationFailure(
                            AuthenticationMethod.APP_PASSWORD,
                            UnlockFailureMessage.AuthenticationBusy,
                        ),
                    ),
            )

        try {
            val result = UnlockReducer.reduce(initial, UnlockMutation.UnlockInputsReset)

            assertFalse(result.recoveryUnlockVisible)
            assertNull(result.inputExpansion)
            assertSame(UnlockVerificationPhase.Idle, result.verificationPhase)
            assertSame(EmptySensitiveValue, result.appPassword)
            assertSame(EmptySensitiveValue, result.recoveryCode)
            assertSame(kdfState, result.kdfInitialization)
            assertSame(availableMethods, result.availableMethods)
            assertEquals(
                mapOf(AuthenticationMethod.APP_PASSWORD to attemptStatus),
                result.authenticationAttemptStatuses,
            )
        } finally {
            appPassword.wipe()
            recoveryCode.wipe()
        }
    }

    @Test
    fun `dismissing setup dialog removes both password references`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result =
                UnlockReducer.reduce(
                    UnlockUiState(
                        passwordSetup =
                            UnlockPasswordSetup.Editing(
                                PasswordSetupDraft(
                                    newPassword = password,
                                    confirmPassword = confirm,
                                ),
                            ),
                    ),
                    UnlockMutation.PasswordSetupClosed,
                )

            assertSame(UnlockPasswordSetup.Closed, result.passwordSetup)
        } finally {
            password.wipe()
            confirm.wipe()
        }
    }
}
