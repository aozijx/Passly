package com.aozijx.passly.presentation.feature.vault.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpType

sealed interface AddOtpAction {
    data class FormChanged(val form: OtpFormState) : AddOtpAction
    data class TypeChanged(val type: OtpType) : AddOtpAction
    data class UriChanged(
        val value: String,
        val reportFailure: Boolean = false,
    ) : AddOtpAction
    data class ScannedConfigApplied(val config: OtpConfig) : AddOtpAction
    data object Save : AddOtpAction
}
