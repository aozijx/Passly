package com.aozijx.passly.presentation.feature.vault.editor.password

data class AddPasswordFormState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val tags: String = "",
    val isPasswordVisible: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && password.isNotBlank()
}

data class AddPasswordUiState(
    val form: AddPasswordFormState = AddPasswordFormState(),
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface AddPasswordEffect {
    data object Saved : AddPasswordEffect
    data class SaveFailed(val message: String?) : AddPasswordEffect
}
