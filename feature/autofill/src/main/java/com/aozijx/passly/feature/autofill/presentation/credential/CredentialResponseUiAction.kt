package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent

internal sealed interface CredentialResponseUiAction {
    data class PasswordGet(
        val sourceIntent: Intent,
    ) : CredentialResponseUiAction

    data class Unlock(
        val sourceIntent: Intent,
    ) : CredentialResponseUiAction

    data class PasswordCreate(
        val sourceIntent: Intent,
    ) : CredentialResponseUiAction

    data object UnknownAction : CredentialResponseUiAction
}
