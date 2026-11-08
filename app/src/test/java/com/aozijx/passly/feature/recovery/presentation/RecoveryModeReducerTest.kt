package com.aozijx.passly.feature.recovery.presentation

import com.aozijx.passly.R
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.presentation.mapping.PasswordSetupDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryModeReducerTest {
    @Test
    fun `recovery state exposes explicit dialog mode and feedback`() {
        val draft = PasswordSetupDraft()
        val editing = RecoveryModeUiState.Editing(draft, canSubmit = true)
        val saving = RecoveryModeUiState.Saving(draft)
        val failed = RecoveryModeUiState.Failed(draft, RecoveryPasswordSetupFailure.SetupFailed)

        assertSame(RecoveryPasswordDialogError.None, editing.error)
        assertSame(draft, editing.draft)
        assertTrue(editing.canSubmit)
        assertFalse(editing.isSaving)
        assertSame(RecoveryPasswordDialogError.None, saving.error)
        assertSame(draft, saving.draft)
        assertFalse(saving.canSubmit)
        assertTrue(saving.isSaving)
        assertEquals(
            RecoveryPasswordDialogError.Message(R.string.recovery_mode_password_setup_failed),
            failed.error,
        )
    }

    @Test
    fun `successful password setup closes the dialog state`() {
        val password = OwnedChars.fromString("temporary")
        val confirm = OwnedChars.fromString("temporary")
        try {
            val result =
                RecoveryModeReducer.reduce(
                    RecoveryModeUiState.Editing(
                        PasswordSetupDraft(
                            newPassword = password,
                            confirmPassword = confirm,
                        ),
                    ),
                    RecoveryModeMutation.PasswordSetupCompleted,
                )

            assertSame(RecoveryModeUiState.Closed, result)
        } finally {
            password.wipe()
            confirm.wipe()
        }
    }

    @Test
    fun `rejected recovery mode closes setup`() {
        val result =
            RecoveryModeReducer.reduce(
                RecoveryModeUiState.Saving(PasswordSetupDraft()),
                RecoveryModeMutation.RecoveryModeRejected,
            )

        assertSame(RecoveryModeUiState.Closed, result)
    }

    @Test
    fun `opening dialog starts with empty password drafts`() {
        val result =
            RecoveryModeReducer.reduce(
                RecoveryModeUiState.Closed,
                RecoveryModeMutation.PasswordDialogOpened,
            )

        val setup = result as RecoveryModeUiState.Editing
        assertSame(EmptySensitiveValue, setup.draft.newPassword)
        assertSame(EmptySensitiveValue, setup.draft.confirmPassword)
    }
}
