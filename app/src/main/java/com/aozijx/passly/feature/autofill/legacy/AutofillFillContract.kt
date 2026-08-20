package com.aozijx.passly.feature.autofill.legacy

import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.aozijx.passly.domain.autofill.model.ResolvedCandidate
import com.aozijx.passly.domain.settings.model.AutofillPresentation

sealed interface AutofillAuthenticationPayload {
    data class Response(val value: FillResponse) : AutofillAuthenticationPayload
    data class DatasetResult(val value: Dataset) : AutofillAuthenticationPayload
}

sealed class AutofillFillUiState {
    data object Initial : AutofillFillUiState()
    data object Loading : AutofillFillUiState()
    data class ShowCandidates(
        val candidates: List<ResolvedCandidate>
    ) : AutofillFillUiState()

    data class Result(
        val payload: AutofillAuthenticationPayload?
    ) : AutofillFillUiState()

    data class Error(val message: String) : AutofillFillUiState()
}

data class AutofillFillRequest(
    val uiMode: AutofillPresentation,
    val isUnlockOnly: Boolean,
    val packageName: String?,
    val webDomain: String?,
    val directEntryId: String?,
    val candidateEntryIds: List<String>,
    val returnsDataset: Boolean,
    /** All editable fields on the page. */
    val editableIds: List<AutofillId> = emptyList(),
    /** Fields identified as having specific roles. */
    val usernameIds: List<AutofillId> = emptyList(),
    val passwordIds: List<AutofillId> = emptyList(),
    val otpIds: List<AutofillId> = emptyList(),
)
