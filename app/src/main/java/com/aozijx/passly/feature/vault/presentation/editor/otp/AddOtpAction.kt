package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.domain.entry.model.otp.OtpType

sealed interface AddOtpAction {
    data class FormChanged(val form: OtpFormState) : AddOtpAction
    data class TypeChanged(val type: OtpType) : AddOtpAction
    data class UriChanged(
        val value: String,
        val reportFailure: Boolean = false,
    ) : AddOtpAction
    data object Save : AddOtpAction
}
