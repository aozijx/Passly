package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.otp.AddOtpCodeState
import com.aozijx.passly.feature.vault.presentation.editor.otp.OtpFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEntrySaveCapabilityTest {
    @Test
    fun validPasswordAndOtpFormsCanSaveOnlyWhileIdleAndAvailable() {
        val passwordForm = AddPasswordFormState(title = "Mail", password = "secret")
        val otpForm = OtpFormState(title = "Mail", secret = "JBSWY3DPEHPK3PXP")

        assertTrue(AddPasswordUiState(form = passwordForm).canSave)
        assertFalse(
            AddPasswordUiState(form = passwordForm, saveState = EditorSaveState.Saving).canSave,
        )
        assertTrue(AddOtpCodeState(form = otpForm).canSave)
        assertFalse(AddOtpCodeState(form = otpForm, saveState = EditorSaveState.Saving).canSave)
        assertFalse(EditorSaveState.Idle.isSaving)
        assertTrue(EditorSaveState.Saving.isSaving)
        assertTrue(EditorSaveState.Idle.canStartSave)
        assertFalse(EditorSaveState.Saving.canStartSave)
        assertTrue(EditorLoadPhase.NotRequested.allowsSaving)
        assertTrue(EditorLoadPhase.Ready.allowsSaving)
        assertFalse(EditorLoadPhase.Loading.allowsSaving)
        assertFalse(EditorLoadPhase.Failed.allowsSaving)
        assertFalse(AddPasswordUiState(form = passwordForm, loadPhase = EditorLoadPhase.Loading).canSave)
        assertFalse(AddPasswordUiState(form = passwordForm, loadPhase = EditorLoadPhase.Failed).canSave)
        assertFalse(AddOtpCodeState(form = otpForm, loadPhase = EditorLoadPhase.Loading).canSave)
        assertFalse(AddOtpCodeState(form = otpForm, loadPhase = EditorLoadPhase.Failed).canSave)
    }
}
