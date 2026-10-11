package com.aozijx.passly.presentation.feature.vault.editor.otp

data class AddOtpCodeState(
    val form: OtpFormState = OtpFormState(),
    val savePhase: AddOtpSavePhase = AddOtpSavePhase.Idle,
) {
    val canSave: Boolean
        get() = form.isValid && savePhase == AddOtpSavePhase.Idle
}

sealed interface AddOtpSavePhase {
    data object Idle : AddOtpSavePhase
    data object Saving : AddOtpSavePhase
}

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent
    data object UriParseFailed : AddOtpEvent
}
