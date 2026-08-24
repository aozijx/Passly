package com.aozijx.passly.presentation.feature.unlock

import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.feature.onboarding.BootstrapMutation
import com.aozijx.passly.presentation.feature.onboarding.BootstrapReducer
import com.aozijx.passly.presentation.feature.onboarding.BootstrapUiState
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

        assertEquals(AuthenticationMethod.APP_PASSWORD, started.activeMethod)
        assertNull(finished.activeMethod)
        assertSame(failure, finished.verificationFailure?.failure)
    }

    @Test
    fun `resetting unlock inputs clears only unlock state`() {
        val initial = UnlockUiState(
            recoveryUnlockVisible = true,
            expandedMethod = AuthenticationMethod.RECOVERY_CODE,
        )

        val result = UnlockReducer.reduce(
            initial,
            UnlockMutation.UnlockInputsReset,
        )

        assertFalse(result.recoveryUnlockVisible)
        assertNull(result.expandedMethod)
        assertSame(EmptySensitiveValue, result.appPassword)
        assertSame(EmptySensitiveValue, result.recoveryCode)
    }

    @Test
    fun `dismissing setup dialog removes both password references`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result = BootstrapReducer.reduce(
                BootstrapUiState(
                    showSetPasswordDialog = true,
                    newAppPassword = password,
                    confirmAppPassword = confirm,
                ),
                BootstrapMutation.SetPasswordDialogVisibilityChanged(false),
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
