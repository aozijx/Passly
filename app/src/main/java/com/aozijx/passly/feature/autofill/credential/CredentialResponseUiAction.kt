package com.aozijx.passly.feature.autofill.credential

import android.content.Intent

sealed interface CredentialResponseUiAction {
    data class PasswordGet(val sourceIntent: Intent) : CredentialResponseUiAction
    data class Unlock(val sourceIntent: Intent) : CredentialResponseUiAction
    data class PasswordCreate(val sourceIntent: Intent) : CredentialResponseUiAction
    data object UnknownAction : CredentialResponseUiAction
}
