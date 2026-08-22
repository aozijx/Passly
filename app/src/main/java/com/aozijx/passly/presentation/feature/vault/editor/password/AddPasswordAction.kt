package com.aozijx.passly.presentation.feature.vault.editor.password

sealed interface AddPasswordAction {
    data class TitleChanged(val value: String) : AddPasswordAction
    data class UsernameChanged(val value: String) : AddPasswordAction
    data class PasswordChanged(val value: String) : AddPasswordAction
    data class PasswordVisibilityChanged(val visible: Boolean) : AddPasswordAction
    data class WebsiteChanged(val value: String) : AddPasswordAction
    data class NotesChanged(val value: String) : AddPasswordAction
    data class TagsChanged(val value: String) : AddPasswordAction
    data object Save : AddPasswordAction
}
