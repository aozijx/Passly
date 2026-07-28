package com.aozijx.passly.feature.vault.editor.password

data class AddPasswordUiState(
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val website: String = "",
    val notes: String = "",
    val isPasswordVisible: Boolean = false,
    val isSaving: Boolean = false
) {
    val canSave: Boolean
        get() = title.isNotBlank() && password.isNotBlank() && !isSaving
}

sealed interface AddPasswordEffect {
    data object Saved : AddPasswordEffect
    data class SaveFailed(val message: String?) : AddPasswordEffect
}
