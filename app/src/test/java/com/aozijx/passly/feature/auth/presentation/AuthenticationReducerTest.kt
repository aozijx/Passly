package com.aozijx.passly.feature.auth.presentation

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.feature.auth.contract.AuthenticationUiState
import com.aozijx.passly.domain.sensitive.SecureString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationReducerTest {

    @Test
    fun `authentication lifecycle preserves authoritative failure`() {
        val failure = AuthenticationFailure(
            code = AuthenticationFailureCode.CREDENTIAL_INCORRECT,
        )
        val started = AuthenticationReducer.reduce(
            AuthenticationUiState(),
            AuthenticationMutation.AuthenticationStarted(AuthenticationMethod.APP_PASSWORD),
        )
        val failed = AuthenticationReducer.reduce(
            started,
            AuthenticationMutation.AuthenticationFailed(
                method = AuthenticationMethod.APP_PASSWORD,
                failure = failure,
            ),
        )
        val finished = AuthenticationReducer.reduce(
            failed,
            AuthenticationMutation.AuthenticationFinished,
        )

        assertEquals(AuthenticationMethod.APP_PASSWORD, started.activeMethod)
        assertNull(finished.activeMethod)
        assertSame(failure, finished.verificationFailure?.failure)
    }

    @Test
    fun `resetting unlock inputs keeps setup dialog state`() {
        val initial = AuthenticationUiState(
            showSetPasswordDialog = true,
            recoveryUnlockVisible = true,
            expandedMethod = AuthenticationMethod.RECOVERY_CODE,
        )

        val result = AuthenticationReducer.reduce(
            initial,
            AuthenticationMutation.UnlockInputsReset,
        )

        assertTrue(result.showSetPasswordDialog)
        assertFalse(result.recoveryUnlockVisible)
        assertNull(result.expandedMethod)
        assertSame(EmptySensitiveValue, result.appPassword)
        assertSame(EmptySensitiveValue, result.recoveryCode)
    }

    @Test
    fun `dismissing setup dialog removes both password references`() {
        val password = SecureString.fromString("temporary")
        val confirm = SecureString.fromString("temporary")
        try {
            val result = AuthenticationReducer.reduce(
                AuthenticationUiState(
                    showSetPasswordDialog = true,
                    newAppPassword = password,
                    confirmAppPassword = confirm,
                ),
                AuthenticationMutation.SetPasswordDialogVisibilityChanged(false),
            )

            assertFalse(result.showSetPasswordDialog)
            assertSame(EmptySensitiveValue, result.newAppPassword)
            assertSame(EmptySensitiveValue, result.confirmAppPassword)
        } finally {
            password.wipe()
            confirm.wipe()
        }
    }
}
