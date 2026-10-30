package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

data class AddOtpCodeState(
    val form: OtpFormState = OtpFormState(),
    val savePhase: AddOtpSavePhase = AddOtpSavePhase.Idle,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
    val loadFailed: Boolean = false,
) {
    val canSave: Boolean
        get() = form.isValid && savePhase == AddOtpSavePhase.Idle &&
            loadPhase != EditorLoadPhase.Loading && loadPhase != EditorLoadPhase.Failed
}

sealed interface AddOtpSavePhase {
    data object Idle : AddOtpSavePhase
    data object Saving : AddOtpSavePhase
}

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent
    data object UriParseFailed : AddOtpEvent
}
