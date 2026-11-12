package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.suggestedEntryTitle

internal object OtpFormReducer {
    fun reduce(
        state: OtpFormState,
        mutation: AddOtpAction.FormMutation,
    ): OtpFormState =
        when (mutation) {
            is AddOtpAction.TitleChanged -> {
                state.copy(title = mutation.value)
            }

            is AddOtpAction.IssuerChanged -> {
                state.copy(issuer = mutation.value)
            }

            is AddOtpAction.AccountNameChanged -> {
                state.copy(accountName = mutation.value)
            }

            is AddOtpAction.SecretChanged -> {
                state.copy(secret = mutation.value)
            }

            is AddOtpAction.PeriodChanged -> {
                state.updateConfiguration { config ->
                    (config as? OtpFormConfiguration.Totp)?.copy(periodSeconds = mutation.value) ?: config
                }
            }

            is AddOtpAction.DigitsChanged -> {
                state.updateConfiguration { config ->
                    when (config) {
                        is OtpFormConfiguration.Totp -> config.copy(digits = mutation.value)
                        is OtpFormConfiguration.Hotp -> config.copy(digits = mutation.value)
                        is OtpFormConfiguration.Steam -> config
                    }
                }
            }

            is AddOtpAction.AlgorithmChanged -> {
                state.updateConfiguration { config ->
                    when (config) {
                        is OtpFormConfiguration.Totp -> config.copy(algorithm = mutation.value)
                        is OtpFormConfiguration.Hotp -> config.copy(algorithm = mutation.value)
                        is OtpFormConfiguration.Steam -> config
                    }
                }
            }

            is AddOtpAction.EncodingChanged -> {
                state.updateConfiguration { it.withEncoding(mutation.value) }
            }

            is AddOtpAction.CounterChanged -> {
                state.updateConfiguration { config ->
                    (config as? OtpFormConfiguration.Hotp)?.copy(counter = mutation.value) ?: config
                }
            }

            is AddOtpAction.TypeChanged -> {
                state.withType(mutation.type)
            }

            is AddOtpAction.UriChanged -> {
                state.copy(uriText = mutation.value)
            }

            is AddOtpAction.ConfigApplied -> {
                state.withConfig(mutation.config)
            }
        }

    private inline fun OtpFormState.updateConfiguration(transform: (OtpFormConfiguration) -> OtpFormConfiguration): OtpFormState =
        copy(configuration = transform(configuration))

    private fun OtpFormConfiguration.withEncoding(encoding: OtpSecretEncoding) =
        when (this) {
            is OtpFormConfiguration.Totp -> copy(encoding = encoding)
            is OtpFormConfiguration.Hotp -> copy(encoding = encoding)
            is OtpFormConfiguration.Steam -> copy(encoding = encoding)
        }

    private fun OtpFormState.withType(type: OtpType): OtpFormState =
        updateConfiguration {
            when (type) {
                OtpType.TOTP -> {
                    when (it) {
                        is OtpFormConfiguration.Totp -> {
                            it
                        }

                        is OtpFormConfiguration.Hotp -> {
                            OtpFormConfiguration.Totp(
                                algorithm =
                                    it.algorithm.takeIf(TotpDefinition.supportedAlgorithms::contains)
                                        ?: TotpDefinition.defaultAlgorithm,
                                digits =
                                    it.digits
                                        .toIntOrNull()
                                        ?.takeIf(TotpDefinition.allowedDigits::contains)
                                        ?.toString()
                                        ?: TotpDefinition.DEFAULT_DIGITS.toString(),
                                encoding = it.encoding,
                            )
                        }

                        is OtpFormConfiguration.Steam -> {
                            OtpFormConfiguration.Totp(encoding = it.encoding)
                        }
                    }
                }

                OtpType.HOTP -> {
                    when (it) {
                        is OtpFormConfiguration.Hotp -> {
                            it
                        }

                        is OtpFormConfiguration.Totp -> {
                            OtpFormConfiguration.Hotp(
                                algorithm =
                                    it.algorithm.takeIf(HotpDefinition.supportedAlgorithms::contains)
                                        ?: HotpDefinition.defaultAlgorithm,
                                digits =
                                    it.digits
                                        .toIntOrNull()
                                        ?.takeIf(HotpDefinition.allowedDigits::contains)
                                        ?.toString()
                                        ?: HotpDefinition.DEFAULT_DIGITS.toString(),
                                encoding = it.encoding,
                            )
                        }

                        is OtpFormConfiguration.Steam -> {
                            OtpFormConfiguration.Hotp(encoding = it.encoding)
                        }
                    }
                }

                OtpType.STEAM -> {
                    OtpFormConfiguration.Steam(encoding = it.encoding)
                }
            }
        }

    private fun OtpFormState.withConfig(config: OtpConfig): OtpFormState =
        copy(
            title = config.suggestedEntryTitle(fallback = title),
            accountName = config.accountName.orEmpty(),
            secret = config.secret.orEmpty(),
            issuer = config.issuer.orEmpty(),
            configuration =
                when (config) {
                    is OtpConfig.Totp -> {
                        OtpFormConfiguration.Totp(
                            algorithm = config.algorithm,
                            digits = config.digits.toString(),
                            periodSeconds = config.periodSeconds.toString(),
                            encoding = config.encoding,
                        )
                    }

                    is OtpConfig.Hotp -> {
                        OtpFormConfiguration.Hotp(
                            algorithm = config.algorithm,
                            digits = config.digits.toString(),
                            counter = config.counter.toString(),
                            encoding = config.encoding,
                        )
                    }

                    is OtpConfig.Steam -> {
                        OtpFormConfiguration.Steam(config.encoding)
                    }
                },
        )
}
