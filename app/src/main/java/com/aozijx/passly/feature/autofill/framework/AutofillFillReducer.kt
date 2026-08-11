package com.aozijx.passly.feature.autofill.framework

import com.aozijx.passly.core.autofill.model.ResolvedCandidate

internal sealed interface AutofillFillMutation {
    data object Loading : AutofillFillMutation
    data class CandidatesAvailable(
        val candidates: List<ResolvedCandidate>,
    ) : AutofillFillMutation
    data class Completed(
        val payload: AutofillAuthenticationPayload?,
    ) : AutofillFillMutation
    data class Failed(val message: String) : AutofillFillMutation
}

internal object AutofillFillReducer {
    fun reduce(
        state: AutofillFillUiState,
        mutation: AutofillFillMutation,
    ): AutofillFillUiState = when (mutation) {
        AutofillFillMutation.Loading -> AutofillFillUiState.Loading
        is AutofillFillMutation.CandidatesAvailable ->
            AutofillFillUiState.ShowCandidates(mutation.candidates)
        is AutofillFillMutation.Completed -> AutofillFillUiState.Result(mutation.payload)
        is AutofillFillMutation.Failed -> AutofillFillUiState.Error(mutation.message)
    }
}
