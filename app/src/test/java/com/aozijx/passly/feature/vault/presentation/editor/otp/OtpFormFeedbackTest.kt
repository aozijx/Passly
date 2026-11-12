package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OtpFormFeedbackTest {
    @Test
    fun `empty secret remains invalid for saving but does not show an input error`() {
        val form = OtpFormState(title = "OTP")

        assertFalse(form.isValid)
        assertEquals(OtpFieldFeedbackUiState.Hidden, form.feedback.secret)
    }

    @Test
    fun `invalid HOTP values project only visible field feedback`() {
        val feedback =
            OtpFormState(
                secret = "!",
                configuration = OtpFormConfiguration.Hotp(counter = "not a number", digits = "3"),
            ).feedback

        assertEquals(OtpFieldFeedbackUiState.Invalid, feedback.secret)
        assertEquals(
            OtpConfigurationFeedbackUiState.Hotp(
                digits = OtpFieldFeedbackUiState.Invalid,
                counter = OtpFieldFeedbackUiState.Invalid,
            ),
            feedback.configuration,
        )
    }

    @Test
    fun `invalid TOTP values project period and digit feedback`() {
        val feedback =
            OtpFormState(
                secret = "JBSWY3DPEHPK3PXP",
                configuration = OtpFormConfiguration.Totp(periodSeconds = "0", digits = "9"),
            ).feedback

        assertEquals(OtpFieldFeedbackUiState.Hidden, feedback.secret)
        assertEquals(
            OtpConfigurationFeedbackUiState.Totp(
                digits = OtpFieldFeedbackUiState.Invalid,
                period = OtpFieldFeedbackUiState.Invalid,
            ),
            feedback.configuration,
        )
    }

    @Test
    fun `valid configuration has no field feedback`() {
        val feedback = OtpFormState(secret = "JBSWY3DPEHPK3PXP").feedback

        assertEquals(OtpFieldFeedbackUiState.Hidden, feedback.secret)
        assertEquals(OtpConfigurationFeedbackUiState.Totp(), feedback.configuration)
    }
}
