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
