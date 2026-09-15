package com.aozijx.passly.presentation.feature.vault.editor.otp

data class AddOtpCodeState(
    val form: OtpFormState = OtpFormState(),
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent
    data object UriParseFailed : AddOtpEvent
}
