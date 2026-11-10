package com.aozijx.passly.feature.autofill.presentation.legacy

import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary

internal sealed interface AutofillFillMutation {
    data object Loading : AutofillFillMutation

    data class CandidatesAvailable(
        val candidates: List<AutofillCandidateSummary>,
    ) : AutofillFillMutation

    data class WebRecipientConfirmationRequired(
        val packageName: String,
        val webDomain: String,
    ) : AutofillFillMutation

    data class WebEntryConfirmationRequired(
        val packageName: String,
        val webDomain: String,
        val candidate: AutofillCandidateSummary,
    ) : AutofillFillMutation

    data class Completed(
        val result: AutofillFillResult,
    ) : AutofillFillMutation

    data object Failed : AutofillFillMutation

    data object ResultDelivered : AutofillFillMutation
}

internal object AutofillFillReducer {
    fun reduce(
        state: AutofillFillUiState,
        mutation: AutofillFillMutation,
    ): AutofillFillUiState =
        when (mutation) {
            is AutofillFillMutation.Loading -> {
                AutofillFillUiState.Loading
            }

            is AutofillFillMutation.CandidatesAvailable -> {
                AutofillFillUiState.ShowCandidates(mutation.candidates)
            }

            is AutofillFillMutation.WebRecipientConfirmationRequired -> {
                AutofillFillUiState.ConfirmWebRecipient(mutation.packageName, mutation.webDomain)
            }

            is AutofillFillMutation.WebEntryConfirmationRequired -> {
                AutofillFillUiState.ConfirmWebEntry(mutation.packageName, mutation.webDomain, mutation.candidate)
            }

            is AutofillFillMutation.Completed -> {
                AutofillFillUiState.Result(mutation.result)
            }

            AutofillFillMutation.Failed -> {
                AutofillFillUiState.Error
            }

            AutofillFillMutation.ResultDelivered -> {
                when (state) {
                    is AutofillFillUiState.Result,
                    AutofillFillUiState.Error,
                    -> AutofillFillUiState.Initial

                    AutofillFillUiState.Initial,
                    AutofillFillUiState.Loading,
                    is AutofillFillUiState.ShowCandidates,
                    is AutofillFillUiState.ConfirmWebRecipient,
                    is AutofillFillUiState.ConfirmWebEntry,
                    -> state
                }
            }
        }
}
