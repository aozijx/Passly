package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.OtpConfigInput
import com.aozijx.passly.domain.entry.otp.OtpConfigInputResult
import com.aozijx.passly.domain.entry.model.otp.OtpMovingFactor
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.feature.vault.otp.presentation.plugin

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

    val usesCounterField: Boolean get() = OtpTypeDefinitions[type].movingFactor == OtpMovingFactor.COUNTER

    val allowsParameterEditing: Boolean get() = type.plugin().parameterEditingAllowed

    val feedback: OtpFormFeedbackUiState
        get() = configInput.parse().toFeedbackUiState(secretHasInput = secret.isNotEmpty())
}

enum class OtpFieldFeedbackUiState { Hidden, Invalid }

data class OtpFormFeedbackUiState(
    val secret: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    val counter: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    val period: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    val digits: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
)

private fun OtpConfigInputResult.toFeedbackUiState(secretHasInput: Boolean): OtpFormFeedbackUiState =
    when (this) {
        is OtpConfigInputResult.Invalid -> {
            OtpFormFeedbackUiState(
                secret =
                    if (secretHasInput && FieldKey.OTP_SECRET in fields) {
                        OtpFieldFeedbackUiState.Invalid
                    } else {
                        OtpFieldFeedbackUiState.Hidden
                    },
                counter = fields.feedback(FieldKey.OTP_COUNTER),
                period = fields.feedback(FieldKey.OTP_PERIOD),
                digits = fields.feedback(FieldKey.OTP_DIGITS),
            )
        }

        is OtpConfigInputResult.Valid -> {
            OtpFormFeedbackUiState()
        }
    }

private fun Set<FieldKey>.feedback(field: FieldKey): OtpFieldFeedbackUiState =
    if (field in this) OtpFieldFeedbackUiState.Invalid else OtpFieldFeedbackUiState.Hidden
