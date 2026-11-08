package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.recovery.presentation.RecoveryModeUiState
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import org.junit.Assert.assertSame
import org.junit.Test

class RecoveryModeReducerTest {
    @Test
    fun `successful password setup clears sensitive references`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result =
                RecoveryModeReducer.reduce(
                    RecoveryModeUiState(
                        passwordSetup =
                            RecoveryPasswordSetup.Editing(
                                PasswordSetupDraft(
                                    newPassword = password,
                                    confirmPassword = confirm,
                                ),
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
        val result =
            RecoveryModeReducer.reduce(
                RecoveryModeUiState(
                    passwordSetup =
                        RecoveryPasswordSetup.Saving(PasswordSetupDraft()),
                ),
                RecoveryModeMutation.RecoveryModeRejected,
            )

        assertSame(RecoveryPasswordSetup.Closed, result.passwordSetup)
    }

    @Test
    fun `opening dialog preserves no full access state`() {
        val result =
            RecoveryModeReducer.reduce(
                RecoveryModeUiState(),
                RecoveryModeMutation.PasswordDialogOpened,
            )

        val setup = result.passwordSetup as RecoveryPasswordSetup.Editing
        assertSame(EmptySensitiveValue, setup.draft.newPassword)
        assertSame(EmptySensitiveValue, setup.draft.confirmPassword)
    }
}
