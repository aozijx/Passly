package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

sealed interface CredentialResponseCompletion {
    val resultIntent: Intent

    data class Immediate(
        override val resultIntent: Intent,
    ) : CredentialResponseCompletion

    data class CommittedSelection(
        override val resultIntent: Intent,
        val receipt: AutofillSelectionReceipt,
    ) : CredentialResponseCompletion
}

sealed class CredentialResponseUiState {
    data object Loading : CredentialResponseUiState()
    data class Complete(val completion: CredentialResponseCompletion) : CredentialResponseUiState()
    data object Unrecoverable : CredentialResponseUiState()
}
