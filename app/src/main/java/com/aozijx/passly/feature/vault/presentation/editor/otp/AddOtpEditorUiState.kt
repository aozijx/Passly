package com.aozijx.passly.feature.vault.presentation.editor.otp

import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

data class AddOtpEditorUiState(
    val form: OtpFormState = OtpFormState(),
    val isSaving: Boolean = false,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val isLoading: Boolean
        get() = loadPhase == EditorLoadPhase.Loading

    val canSave: Boolean
        get() =
            form.isValid && !isSaving && loadPhase.allowsSaving
}
