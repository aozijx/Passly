package com.aozijx.passly.feature.unlock.presentation

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationMethods
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationReducerTest {

    @Test
    fun `available authentication methods are part of authoritative ui state`() {
        val methods = AuthenticationMethods(
            setOf(
                AuthenticationMethod.BIOMETRIC,
                AuthenticationMethod.APP_PASSWORD,
            ),
        )

        val result = UnlockReducer.reduce(
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
            val short = UnlockReducer.reduce(
                UnlockUiState(passwordSetup = UnlockPasswordSetup.Editing()),
                UnlockMutation.NewAppPasswordChanged(shortPassword),
            )
            val withPassword = UnlockReducer.reduce(
                short,
                UnlockMutation.NewAppPasswordChanged(validPassword),
            )
            val matching = UnlockReducer.reduce(
                withPassword,
                UnlockMutation.ConfirmAppPasswordChanged(matchingConfirmation),
            )

            assertFalse((short.passwordSetup as UnlockPasswordSetup.Editing).canSubmit())
            assertFalse((withPassword.passwordSetup as UnlockPasswordSetup.Editing).canSubmit())
            assertTrue((matching.passwordSetup as UnlockPasswordSetup.Editing).canSubmit())
        } finally {
            shortPassword.wipe()
            validPassword.wipe()
            matchingConfirmation.wipe()
        }
    }

    @Test
    fun `authentication lifecycle preserves authoritative failure`() {
        val failure = AuthenticationFailure(
            code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
        )
        val started = UnlockReducer.reduce(
            UnlockUiState(),
            UnlockMutation.AuthenticationStarted(AuthenticationMethod.APP_PASSWORD),
        )
        val failed = UnlockReducer.reduce(
            started,
            UnlockMutation.AuthenticationFailed(
                method = AuthenticationMethod.APP_PASSWORD,
                failure = failure,
            ),
        )
        val finished = UnlockReducer.reduce(
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
    fun `resetting unlock inputs clears only unlock state`() {
        val initial = UnlockUiState(
            recoveryUnlockVisible = true,
            inputExpansion = UnlockInputExpansion.Expanded(AuthenticationMethod.RECOVERY_CODE),
        )

        val result = UnlockReducer.reduce(
            initial,
            UnlockMutation.UnlockInputsReset,
        )

        assertFalse(result.recoveryUnlockVisible)
        assertSame(UnlockInputExpansion.Collapsed, result.inputExpansion)
        assertSame(EmptySensitiveValue, result.appPassword)
        assertSame(EmptySensitiveValue, result.recoveryCode)
    }

    @Test
    fun `dismissing setup dialog removes both password references`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result = UnlockReducer.reduce(
                UnlockUiState(
                    passwordSetup = UnlockPasswordSetup.Editing(
                        newPassword = password,
                        confirmPassword = confirm,
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
