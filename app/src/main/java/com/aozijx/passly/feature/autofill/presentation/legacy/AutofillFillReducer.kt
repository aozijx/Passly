package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary

sealed interface AutofillFillMutation {
    data object Loading : AutofillFillMutation
    data class CandidatesAvailable(val candidates: List<AutofillCandidateSummary>) : AutofillFillMutation
    data class Completed(val result: AutofillFillResult) : AutofillFillMutation
    data class Failed(val message: String) : AutofillFillMutation
    data object ResultDelivered : AutofillFillMutation
}

object AutofillFillReducer {
    fun reduce(state: AutofillFillUiState, mutation: AutofillFillMutation): AutofillFillUiState {
        return when (mutation) {
            is AutofillFillMutation.Loading -> AutofillFillUiState.Loading
            is AutofillFillMutation.CandidatesAvailable -> AutofillFillUiState.ShowCandidates(mutation.candidates)
            is AutofillFillMutation.Completed -> AutofillFillUiState.Result(mutation.result)
            is AutofillFillMutation.Failed -> AutofillFillUiState.Error(mutation.message)
            AutofillFillMutation.ResultDelivered -> when (state) {
                is AutofillFillUiState.Result,
                is AutofillFillUiState.Error,
                -> AutofillFillUiState.Initial
                AutofillFillUiState.Initial,
                AutofillFillUiState.Loading,
                is AutofillFillUiState.ShowCandidates,
                -> state
            }
        }
    }
}
