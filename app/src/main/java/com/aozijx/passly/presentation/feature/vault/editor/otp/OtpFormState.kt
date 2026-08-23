package com.aozijx.passly.presentation.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

/** OTP form values. Save progress and errors belong to the page ViewModel state. */
data class OtpFormState(
    val title: String = "",
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val period: String = "30",
    val digits: String = "6",
    val type: OtpType = OtpType.TOTP,
    val algorithm: String = "SHA1",
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val counter: String = "0",
    val uriText: String = "",
    val notes: String = "",
    val tags: String = "",
) {
    val isValid: Boolean get() = title.isNotBlank() && secret.isNotBlank()
}
