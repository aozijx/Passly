package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpMovingFactor
import com.aozijx.passly.domain.entry.model.otp.OtpType
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
        return when (definition.movingFactor) {
            OtpMovingFactor.TIME ->
                copy(
                    type = type,
                    digits = (validDigits ?: definition.defaultDigits).toString(),
                    period =
                        (definition.fixedPeriodSeconds
                            ?: period.toIntOrNull()?.takeIf { it > 0 }
                            ?: checkNotNull(definition.defaultPeriodSeconds)).toString(),
                    algorithm = definition.fixedAlgorithm ?: algorithm,
                )

            OtpMovingFactor.COUNTER ->
                copy(
                    type = type,
                    digits = (validDigits ?: definition.defaultDigits).toString(),
                    counter = counter.toLongOrNull()?.takeIf { it >= 0L }?.toString()
                        ?: checkNotNull(definition.defaultCounter).toString(),
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
            period = (config.periodSeconds ?: OtpTypeDefinitions[config.type].defaultPeriodSeconds ?: 30).toString(),
            counter = (config.counter ?: OtpTypeDefinitions[config.type].defaultCounter ?: 0L).toString(),
            algorithm = config.algorithm,
            encoding = config.encoding,
        ).withTypeDefaults(config.type)

}
