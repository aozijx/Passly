package com.aozijx.passly.feature.autofill.presentation.credential

import android.content.Intent
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

internal sealed interface CredentialResponseCompletion {
    val resultIntent: Intent

    data class Immediate(
        override val resultIntent: Intent,
    ) : CredentialResponseCompletion

    data class CommittedSelection(
        override val resultIntent: Intent,
        val receipt: AutofillSelectionReceipt,
    ) : CredentialResponseCompletion
}

/** The Android protocol payload must never be persisted in UI state or SavedState. */
internal sealed interface CredentialResponseUiState {
    data object Loading : CredentialResponseUiState

    data object Unrecoverable : CredentialResponseUiState
}
