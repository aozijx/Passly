package com.aozijx.passly.presentation.feature.autofill.credential

import android.content.Intent
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

data class CredentialResponseCompletion(
    val resultIntent: Intent,
    val receipt: AutofillSelectionReceipt? = null,
)

sealed class CredentialResponseUiState {
    data object Loading : CredentialResponseUiState()
    data class Complete(val completion: CredentialResponseCompletion) : CredentialResponseUiState()
    data object Unrecoverable : CredentialResponseUiState()
}
