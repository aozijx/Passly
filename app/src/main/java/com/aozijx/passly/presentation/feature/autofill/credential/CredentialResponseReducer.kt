package com.aozijx.passly.presentation.feature.autofill.credential

internal sealed interface CredentialResponseMutation {
    data class Completed(
        val completion: CredentialResponseCompletion,
    ) : CredentialResponseMutation
    data object Unrecoverable : CredentialResponseMutation
}

internal object CredentialResponseReducer {
    fun reduce(
        state: CredentialResponseUiState,
        mutation: CredentialResponseMutation,
    ): CredentialResponseUiState = when (mutation) {
        is CredentialResponseMutation.Completed ->
            CredentialResponseUiState.Complete(mutation.completion)
        CredentialResponseMutation.Unrecoverable -> CredentialResponseUiState.Unrecoverable
    }
}
