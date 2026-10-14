package com.aozijx.passly.presentation.feature.autofill.credential

import android.content.Intent
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

internal sealed interface CredentialResponseMutation {
    data class Completed(
        val resultIntent: Intent,
        val receipt: AutofillSelectionReceipt? = null,
    ) : CredentialResponseMutation
    data object Unrecoverable : CredentialResponseMutation
}

internal object CredentialResponseReducer {
    fun reduce(
        state: CredentialResponseUiState,
        mutation: CredentialResponseMutation,
    ): CredentialResponseUiState = when (mutation) {
        is CredentialResponseMutation.Completed ->
            CredentialResponseUiState.Complete(
                CredentialResponseCompletion(mutation.resultIntent, mutation.receipt),
            )
        CredentialResponseMutation.Unrecoverable -> CredentialResponseUiState.Unrecoverable
    }
}
