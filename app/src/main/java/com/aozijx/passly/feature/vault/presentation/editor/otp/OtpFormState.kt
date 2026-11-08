package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpConfigInput
import com.aozijx.passly.domain.entry.otp.OtpConfigInputResult

/** OTP form values. Save progress and errors belong to the page ViewModel state. */
data class OtpFormState(
    val title: String = "",
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val period: String = "30",
    val digits: String = "6",
    val type: OtpType = OtpType.TOTP,
    val algorithm: OtpHashAlgorithm = OtpHashAlgorithm.SHA1,
    val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    val counter: String = "0",
    val uriText: String = "",
    val notes: String = "",
    val tags: String = "",
) {
    val configInput: OtpConfigInput
        get() =
            OtpConfigInput(
                secret = secret,
                type = type,
                algorithm = algorithm,
                digits = digits,
                period = period,
                counter = counter,
                encoding = encoding,
                issuer = issuer,
                accountName = accountName,
            )

    val isValid: Boolean get() = title.isNotBlank() && configInput.parse() is OtpConfigInputResult.Valid

    val invalidFields: Set<FieldKey>
        get() = configInput.parse().invalidFields()

    val secretInputState: OtpSecretInputState
        get() =
            when {
                secret.isEmpty() -> OtpSecretInputState.Empty
                configInput.parse().invalidFields().contains(FieldKey.OTP_SECRET) -> OtpSecretInputState.Invalid
                else -> OtpSecretInputState.Valid
            }
}

enum class OtpSecretInputState { Empty, Valid, Invalid }

private fun OtpConfigInputResult.invalidFields(): Set<FieldKey> =
    when (this) {
        is OtpConfigInputResult.Invalid -> fields
        is OtpConfigInputResult.Valid -> emptySet()
    }
