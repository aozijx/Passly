package com.aozijx.passly.presentation.feature.autofill.legacy

import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary

sealed interface AutofillFillMutation {
    data object Loading : AutofillFillMutation
    data class CandidatesAvailable(val candidates: List<AutofillCandidateSummary>) : AutofillFillMutation
    data class Completed(val result: AutofillFillResult) : AutofillFillMutation
    data class Failed(val message: String) : AutofillFillMutation
}

object AutofillFillReducer {
    fun reduce(state: AutofillFillUiState, mutation: AutofillFillMutation): AutofillFillUiState {
        return when (mutation) {
            is AutofillFillMutation.Loading -> AutofillFillUiState.Loading
            is AutofillFillMutation.CandidatesAvailable -> AutofillFillUiState.ShowCandidates(mutation.candidates)
            is AutofillFillMutation.Completed -> AutofillFillUiState.Result(mutation.result)
            is AutofillFillMutation.Failed -> AutofillFillUiState.Error(mutation.message)
        }
    }
}
