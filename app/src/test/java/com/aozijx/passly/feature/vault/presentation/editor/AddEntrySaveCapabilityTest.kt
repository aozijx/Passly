package com.aozijx.passly.feature.vault.presentation.editor

import com.aozijx.passly.feature.vault.presentation.editor.otp.AddOtpCodeState
import com.aozijx.passly.feature.vault.presentation.editor.otp.AddOtpSavePhase
import com.aozijx.passly.feature.vault.presentation.editor.otp.OtpFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordFormState
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordSavePhase
import com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEntrySaveCapabilityTest {
    @Test
    fun validPasswordAndOtpFormsCanSaveOnlyWhileIdle() {
        val passwordForm = AddPasswordFormState(title = "Mail", password = "secret")
        val otpForm = OtpFormState(title = "Mail", secret = "JBSWY3DPEHPK3PXP")

        assertTrue(AddPasswordUiState(form = passwordForm).canSave)
        assertFalse(
            AddPasswordUiState(form = passwordForm, savePhase = AddPasswordSavePhase.Saving).canSave,
        )
        assertTrue(AddOtpCodeState(form = otpForm).canSave)
        assertFalse(AddOtpCodeState(form = otpForm, savePhase = AddOtpSavePhase.Saving).canSave)
    }
}
