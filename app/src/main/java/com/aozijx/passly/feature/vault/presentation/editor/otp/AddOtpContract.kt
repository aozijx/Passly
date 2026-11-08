package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveState

data class AddOtpCodeState(
    val form: OtpFormState = OtpFormState(),
    val saveState: EditorSaveState = EditorSaveState.Idle,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val canSave: Boolean
        get() =
            form.isValid && saveState.canStartSave && loadPhase.allowsSaving
}

sealed interface AddOtpEvent {
    data object UriParsed : AddOtpEvent

    data object UriParseFailed : AddOtpEvent
}
