package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.OtpConfigInput
import com.aozijx.passly.domain.entry.otp.OtpConfigInputResult

/** Shared identity and text fields; algorithm parameters exist only on their owning OTP form. */
data class OtpFormState(
    val title: String = "",
    val issuer: String = "",
    val accountName: String = "",
    val secret: String = "",
    val configuration: OtpFormConfiguration = OtpFormConfiguration.Totp(),
    val uriText: String = "",
    val notes: String = "",
    val tags: String = "",
) {
    val type: OtpType
        get() =
            when (configuration) {
                is OtpFormConfiguration.Totp -> OtpType.TOTP
                is OtpFormConfiguration.Hotp -> OtpType.HOTP
                is OtpFormConfiguration.Steam -> OtpType.STEAM
            }

    val configInput: OtpConfigInput
        get() =
            when (val config = configuration) {
                is OtpFormConfiguration.Totp -> {
                    OtpConfigInput.Totp(secret, config.algorithm, config.digits, config.periodSeconds, config.encoding, issuer, accountName)
                }

                is OtpFormConfiguration.Hotp -> {
                    OtpConfigInput.Hotp(secret, config.algorithm, config.digits, config.counter, config.encoding, issuer, accountName)
                }

                is OtpFormConfiguration.Steam -> {
                    OtpConfigInput.Steam(secret, config.encoding, issuer, accountName)
                }
            }

    val isValid: Boolean get() = title.isNotBlank() && configInput.parse() is OtpConfigInputResult.Valid

    val feedback: OtpFormFeedbackUiState
        get() = configInput.parse().toFeedbackUiState(secretHasInput = secret.isNotEmpty(), configuration)
}

sealed interface OtpFormConfiguration {
    val encoding: OtpSecretEncoding

    data class Totp(
        val algorithm: OtpHashAlgorithm = TotpDefinition.defaultAlgorithm,
        val digits: String = TotpDefinition.DEFAULT_DIGITS.toString(),
        val periodSeconds: String = TotpDefinition.DEFAULT_PERIOD_SECONDS.toString(),
        override val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    ) : OtpFormConfiguration

    data class Hotp(
        val algorithm: OtpHashAlgorithm = HotpDefinition.defaultAlgorithm,
        val digits: String = HotpDefinition.DEFAULT_DIGITS.toString(),
        val counter: String = HotpDefinition.INITIAL_COUNTER.toString(),
        override val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    ) : OtpFormConfiguration

    data class Steam(
        override val encoding: OtpSecretEncoding = OtpSecretEncoding.BASE32,
    ) : OtpFormConfiguration
}

enum class OtpFieldFeedbackUiState { Hidden, Invalid }

data class OtpFormFeedbackUiState(
    val secret: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    val configuration: OtpConfigurationFeedbackUiState = OtpConfigurationFeedbackUiState.Totp(),
)

sealed interface OtpConfigurationFeedbackUiState {
    data class Totp(
        val digits: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
        val period: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    ) : OtpConfigurationFeedbackUiState

    data class Hotp(
        val digits: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
        val counter: OtpFieldFeedbackUiState = OtpFieldFeedbackUiState.Hidden,
    ) : OtpConfigurationFeedbackUiState

    data object Steam : OtpConfigurationFeedbackUiState
}

private fun OtpConfigInputResult.toFeedbackUiState(
    secretHasInput: Boolean,
    configuration: OtpFormConfiguration,
): OtpFormFeedbackUiState =
    when (this) {
        is OtpConfigInputResult.Invalid -> {
            OtpFormFeedbackUiState(
                secret =
                    if (secretHasInput && FieldKey.OTP_SECRET in fields) {
                        OtpFieldFeedbackUiState.Invalid
                    } else {
                        OtpFieldFeedbackUiState.Hidden
                    },
                configuration =
                    when (configuration) {
                        is OtpFormConfiguration.Totp -> {
                            OtpConfigurationFeedbackUiState.Totp(
                                digits = fields.feedback(FieldKey.OTP_DIGITS),
                                period = fields.feedback(FieldKey.OTP_PERIOD),
                            )
                        }

                        is OtpFormConfiguration.Hotp -> {
                            OtpConfigurationFeedbackUiState.Hotp(
                                digits = fields.feedback(FieldKey.OTP_DIGITS),
                                counter = fields.feedback(FieldKey.OTP_COUNTER),
                            )
                        }

                        is OtpFormConfiguration.Steam -> {
                            OtpConfigurationFeedbackUiState.Steam
                        }
                    },
            )
        }

        is OtpConfigInputResult.Valid -> {
            OtpFormFeedbackUiState(
                configuration =
                    when (configuration) {
                        is OtpFormConfiguration.Totp -> OtpConfigurationFeedbackUiState.Totp()
                        is OtpFormConfiguration.Hotp -> OtpConfigurationFeedbackUiState.Hotp()
                        is OtpFormConfiguration.Steam -> OtpConfigurationFeedbackUiState.Steam
                    },
            )
        }
    }

private fun Set<FieldKey>.feedback(field: FieldKey): OtpFieldFeedbackUiState =
    if (field in this) OtpFieldFeedbackUiState.Invalid else OtpFieldFeedbackUiState.Hidden
