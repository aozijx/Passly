package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpConfigParameter
import com.aozijx.passly.domain.entry.model.otp.OtpFactorDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.otp.suggestedEntryTitle
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions

internal object OtpFormReducer {
    fun reduce(
        state: OtpFormState,
        mutation: AddOtpAction.FormMutation,
    ): OtpFormState =
        when (mutation) {
            is AddOtpAction.TitleChanged -> state.copy(title = mutation.value)
            is AddOtpAction.IssuerChanged -> state.copy(issuer = mutation.value)
            is AddOtpAction.AccountNameChanged -> state.copy(accountName = mutation.value)
            is AddOtpAction.SecretChanged -> state.copy(secret = mutation.value)
            is AddOtpAction.PeriodChanged -> state.copy(period = mutation.value)
            is AddOtpAction.DigitsChanged -> state.copy(digits = mutation.value)
            is AddOtpAction.AlgorithmChanged -> state.copy(algorithm = mutation.value)
            is AddOtpAction.EncodingChanged -> state.copy(encoding = mutation.value)
            is AddOtpAction.CounterChanged -> state.copy(counter = mutation.value)
            is AddOtpAction.TypeChanged -> state.withTypeDefaults(mutation.type)
            is AddOtpAction.UriChanged -> state.copy(uriText = mutation.value)
            is AddOtpAction.ConfigApplied -> state.withConfig(mutation.config)
        }

    private fun OtpFormState.withTypeDefaults(type: OtpType): OtpFormState {
        val definition = OtpTypeDefinitions[type]
        val validDigits = digits.toIntOrNull()?.takeIf(definition.allowedDigits::contains)
        return when (val factor = definition.factor) {
            is OtpFactorDefinition.Time ->
                copy(
                    type = type,
                    digits = (validDigits ?: definition.defaultDigits).toString(),
                    period =
                        (period.toIntOrNull()?.takeIf { it > 0 }
                            ?.takeIf { OtpConfigParameter.PERIOD in definition.editableParameters }
                            ?: factor.defaultPeriodSeconds).toString(),
                    algorithm =
                        if (OtpConfigParameter.ALGORITHM in definition.editableParameters) algorithm else definition.defaultAlgorithm,
                )

            is OtpFactorDefinition.Counter ->
                copy(
                    type = type,
                    digits = (validDigits ?: definition.defaultDigits).toString(),
                    counter = counter.toLongOrNull()?.takeIf { it >= 0L }?.toString()
                        ?: factor.initialValue.toString(),
                )
        }
    }

    private fun OtpFormState.withConfig(config: OtpConfig): OtpFormState =
        copy(
            title = config.suggestedEntryTitle(fallback = title),
            accountName = config.accountName.orEmpty(),
            secret = config.secret.orEmpty(),
            issuer = config.issuer.orEmpty(),
            digits = config.digits.toString(),
            period = when (config) {
                is OtpConfig.Totp -> config.periodSeconds.toString()
                is OtpConfig.Steam -> SteamDefinition.PERIOD_SECONDS.toString()
                is OtpConfig.Hotp -> period
            },
            counter = (config as? OtpConfig.Hotp)?.counter?.toString() ?: HotpDefinition.INITIAL_COUNTER.toString(),
            algorithm = config.algorithm,
            encoding = config.encoding,
        ).withTypeDefaults(config.type)

}
