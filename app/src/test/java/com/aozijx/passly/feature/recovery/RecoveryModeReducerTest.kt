package com.aozijx.passly.feature.recovery

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.feature.recovery.RecoveryModeUiState
import com.aozijx.passly.domain.sensitive.OwnedChars
import org.junit.Assert.assertSame
import org.junit.Test

class RecoveryModeReducerTest {

    @Test
    fun `successful password setup clears sensitive references`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result = RecoveryModeReducer.reduce(
                RecoveryModeUiState(
                    passwordSetup = RecoveryPasswordSetup.Editing(
                        newPassword = password,
                        confirmPassword = confirm,
                        phase = RecoveryPasswordSetupPhase.Saving,
                    ),
                ),
                RecoveryModeMutation.PasswordSetupCompleted,
            )

            assertSame(RecoveryPasswordSetup.Closed, result.passwordSetup)
        } finally {
            password.wipe()
            confirm.wipe()
        }
    }

    @Test
    fun `rejected recovery mode closes setup`() {
        val result = RecoveryModeReducer.reduce(
            RecoveryModeUiState(
                passwordSetup = RecoveryPasswordSetup.Editing(
                    phase = RecoveryPasswordSetupPhase.Saving,
                ),
            ),
            RecoveryModeMutation.RecoveryModeRejected,
        )

        assertSame(RecoveryPasswordSetup.Closed, result.passwordSetup)
    }

    @Test
    fun `opening dialog preserves no full access state`() {
        val result = RecoveryModeReducer.reduce(
            RecoveryModeUiState(),
            RecoveryModeMutation.PasswordDialogOpened,
        )

        val setup = result.passwordSetup as RecoveryPasswordSetup.Editing
        assertSame(EmptySensitiveValue, setup.newPassword)
        assertSame(EmptySensitiveValue, setup.confirmPassword)
        assertSame(RecoveryPasswordSetupPhase.Idle, setup.phase)
    }
}
