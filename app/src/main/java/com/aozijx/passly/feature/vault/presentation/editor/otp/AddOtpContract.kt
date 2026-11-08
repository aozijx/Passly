package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

data class AddOtpCodeState(
    val form: OtpFormState = OtpFormState(),
    val isSaving: Boolean = false,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val canSave: Boolean
        get() =
            form.isValid && !isSaving && loadPhase.allowsSaving
}

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent

    data object UriParseFailed : AddOtpEvent
}
