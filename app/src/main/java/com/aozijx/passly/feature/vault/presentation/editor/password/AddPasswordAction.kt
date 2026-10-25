package com.aozijx.passly.feature.vault.presentation.editor.password

sealed interface AddPasswordAction {
    data class TitleChanged(val value: String) : AddPasswordAction
    data class UsernameChanged(val value: String) : AddPasswordAction
    data class PasswordChanged(val value: String) : AddPasswordAction
    data class PasswordVisibilityChanged(val visible: Boolean) : AddPasswordAction
    data class WebsiteUrlChanged(val value: String) : AddPasswordAction
    data class NotesChanged(val value: String) : AddPasswordAction
    data class TagsChanged(val value: String) : AddPasswordAction
    data object LoadAssociatedApps : AddPasswordAction
    data class AssociatedAppSelected(val packageName: String) : AddPasswordAction
    data object Save : AddPasswordAction
}
