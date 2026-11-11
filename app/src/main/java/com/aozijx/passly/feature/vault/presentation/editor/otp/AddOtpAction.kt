package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType

sealed interface AddOtpAction {
    sealed interface FormMutation : AddOtpAction

    data class TitleChanged(val value: String) : FormMutation

    data class IssuerChanged(val value: String) : FormMutation

    data class AccountNameChanged(val value: String) : FormMutation

    data class SecretChanged(val value: String) : FormMutation

    data class PeriodChanged(val value: String) : FormMutation

    data class DigitsChanged(val value: String) : FormMutation

    data class AlgorithmChanged(val value: OtpHashAlgorithm) : FormMutation

    data class EncodingChanged(val value: OtpSecretEncoding) : FormMutation

    data class CounterChanged(val value: String) : FormMutation

    data class TypeChanged(
        val type: OtpType,
    ) : FormMutation

    data class UriChanged(val value: String) : FormMutation

    data class ConfigApplied(val config: OtpConfig) : FormMutation

    data object Save : AddOtpAction
}
