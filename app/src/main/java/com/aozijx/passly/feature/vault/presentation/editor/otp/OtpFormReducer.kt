package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.otp.suggestedEntryTitle
import com.aozijx.passly.feature.vault.otp.presentation.OtpEditorPolicy
import com.aozijx.passly.feature.vault.otp.presentation.plugin

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

    private fun OtpFormState.withTypeDefaults(type: OtpType): OtpFormState =
        when (val policy = type.plugin().editorPolicy) {
            is OtpEditorPolicy.Timed ->
                copy(
                    type = type,
                    digits = digits.takeUnless(policy.digitsToReset::contains) ?: policy.defaultDigits,
                    period = period.ifBlank { policy.defaultPeriod },
                )
            is OtpEditorPolicy.Counter ->
                copy(
                    type = type,
                    digits = digits.takeUnless(policy.digitsToReset::contains) ?: policy.defaultDigits,
                    counter = counter.ifBlank { policy.defaultCounter },
                )
            is OtpEditorPolicy.FixedTimed ->
                copy(
                    type = type,
                    digits = policy.digits,
                    period = policy.period,
                    algorithm = policy.algorithm,
                )
        }

    private fun OtpFormState.withConfig(config: OtpConfig): OtpFormState =
        copy(
            title = config.suggestedEntryTitle(fallback = title),
            accountName = config.accountName.orEmpty(),
            secret = config.secret.orEmpty(),
            issuer = config.issuer.orEmpty(),
            digits = config.digits.toString(),
            period = (config.periodSeconds ?: 30).toString(),
            counter = (config.counter ?: 0L).toString(),
            algorithm = config.algorithm,
            encoding = config.encoding,
        ).withTypeDefaults(config.type)

}
