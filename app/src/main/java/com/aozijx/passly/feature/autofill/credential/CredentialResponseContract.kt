package com.aozijx.passly.feature.autofill.credential

import android.content.Intent

sealed class CredentialResponseUiState {
    data object Loading : CredentialResponseUiState()
    data class Complete(val resultIntent: Intent) : CredentialResponseUiState()
    data object Unrecoverable : CredentialResponseUiState()
}
