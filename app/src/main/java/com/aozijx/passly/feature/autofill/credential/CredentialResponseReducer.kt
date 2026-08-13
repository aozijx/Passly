package com.aozijx.passly.feature.autofill.credential

import android.content.Intent

internal sealed interface CredentialResponseMutation {
    data class Completed(val resultIntent: Intent) : CredentialResponseMutation
    data object Unrecoverable : CredentialResponseMutation
}

internal object CredentialResponseReducer {
    fun reduce(
        state: CredentialResponseUiState,
        mutation: CredentialResponseMutation,
    ): CredentialResponseUiState = when (mutation) {
        is CredentialResponseMutation.Completed ->
            CredentialResponseUiState.Complete(mutation.resultIntent)
        CredentialResponseMutation.Unrecoverable -> CredentialResponseUiState.Unrecoverable
    }
}
