package com.aozijx.passly.presentation.feature.autofill.legacy

import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.view.autofill.AutofillId
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

sealed interface AutofillFillResult {
    data class DiscoveryResponse(
        val value: FillResponse,
    ) : AutofillFillResult

    data class SelectedResponse(
        val value: FillResponse,
        val receipt: AutofillSelectionReceipt,
    ) : AutofillFillResult

    data class SelectedDataset(
        val value: Dataset,
        val receipt: AutofillSelectionReceipt,
    ) : AutofillFillResult

    data object Cancelled : AutofillFillResult
}

sealed class AutofillFillUiState {
    data object Initial : AutofillFillUiState()
    data object Loading : AutofillFillUiState()
    data class ShowCandidates(
        val candidates: List<AutofillCandidateSummary>
    ) : AutofillFillUiState()

    data class Result(
        val result: AutofillFillResult
    ) : AutofillFillUiState()

    data class Error(val message: String) : AutofillFillUiState()
}

sealed interface AutofillFillDisplayState {
    data object Hidden : AutofillFillDisplayState

    data class Candidates(
        val candidates: List<AutofillCandidateSummary>,
    ) : AutofillFillDisplayState
}

internal fun AutofillFillUiState.toDisplayState(): AutofillFillDisplayState = when (this) {
    is AutofillFillUiState.ShowCandidates -> AutofillFillDisplayState.Candidates(candidates)
    AutofillFillUiState.Initial,
    AutofillFillUiState.Loading,
    is AutofillFillUiState.Result,
    is AutofillFillUiState.Error,
    -> AutofillFillDisplayState.Hidden
}

data class AutofillFillRequest(
    val interactionId: AutofillInteractionId,
    val uiMode: AutofillPresentation,
    val packageName: String,
    val webDomain: String?,
    val directEntryId: EntryId?,
    val returnsDataset: Boolean,
    /** All editable fields on the page. */
    val editableIds: List<AutofillId> = emptyList(),
    /** Fields identified as having specific roles. */
    val usernameIds: List<AutofillId> = emptyList(),
    val passwordIds: List<AutofillId> = emptyList(),
    val otpIds: List<AutofillId> = emptyList(),
)
