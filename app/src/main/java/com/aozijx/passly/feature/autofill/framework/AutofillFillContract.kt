package com.aozijx.passly.feature.autofill.framework

import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.aozijx.passly.core.autofill.model.ResolvedCandidate
import com.aozijx.passly.data.settings.model.AutofillPresentation

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
    val usernameId: AutofillId?,
    val passwordId: AutofillId?,
    val otpId: AutofillId?,
    val packageName: String?,
    val webDomain: String?,
    val directEntryId: String?,
    val candidateEntryIds: List<String>,
    val returnsDataset: Boolean,
)
