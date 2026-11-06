package com.aozijx.passly.feature.autofill.presentation.legacy

import android.view.autofill.AutofillId
import android.view.inputmethod.InlineSuggestionsRequest
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCredentialPayload
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.legacy.LegacyAutofillResultTarget
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt

internal sealed interface AutofillFillResult {
    data class Discovery(
        val outcome: AutofillDiscoveryOutcome,
    ) : AutofillFillResult

    data class SelectedCredential(
        val payload: AutofillCredentialPayload,
        val receipt: AutofillSelectionReceipt,
        val target: LegacyAutofillResultTarget,
    ) : AutofillFillResult

    data object Cancelled : AutofillFillResult
}

internal sealed class AutofillFillUiState {
    data object Initial : AutofillFillUiState()

    data object Loading : AutofillFillUiState()

    data class ShowCandidates(
        val candidates: List<AutofillCandidateSummary>,
    ) : AutofillFillUiState()

    data class Result(
        val result: AutofillFillResult,
    ) : AutofillFillUiState()

    data class Error(
        val message: String,
    ) : AutofillFillUiState()
}

internal sealed interface AutofillFillDisplayState {
    data object Hidden : AutofillFillDisplayState

    data class Candidates(
        val candidates: List<AutofillCandidateSummary>,
    ) : AutofillFillDisplayState
}

internal fun AutofillFillUiState.toDisplayState(): AutofillFillDisplayState =
    when (this) {
        is AutofillFillUiState.ShowCandidates -> AutofillFillDisplayState.Candidates(candidates)

        AutofillFillUiState.Initial,
        AutofillFillUiState.Loading,
        is AutofillFillUiState.Result,
        is AutofillFillUiState.Error,
        -> AutofillFillDisplayState.Hidden
    }

internal data class AutofillFillRequest(
    val interactionId: AutofillInteractionId,
    val uiMode: AutofillPresentation,
    val packageName: String,
    val webDomain: String?,
    val directEntryId: EntryId?,
    val resultTarget: LegacyAutofillResultTarget,
    val inlineRequest: InlineSuggestionsRequest? = null,
    /** All editable fields on the page. */
    val editableIds: List<AutofillId> = emptyList(),
    /** Fields identified as having specific roles. */
    val usernameIds: List<AutofillId> = emptyList(),
    val passwordIds: List<AutofillId> = emptyList(),
    val otpIds: List<AutofillId> = emptyList(),
)
