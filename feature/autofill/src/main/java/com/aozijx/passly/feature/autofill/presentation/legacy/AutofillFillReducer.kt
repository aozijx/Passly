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

    data object ResultDelivered : AutofillFillMutation
}

internal object AutofillFillReducer {
    fun reduce(
        state: AutofillFillUiState,
        mutation: AutofillFillMutation,
    ): AutofillFillUiState =
        when (mutation) {
            is AutofillFillMutation.Loading -> {
                when (state) {
                    AutofillFillUiState.Initial,
                    is AutofillFillUiState.ShowCandidates,
                    is AutofillFillUiState.ConfirmWebRecipient,
                    is AutofillFillUiState.ConfirmWebEntry,
                    -> AutofillFillUiState.Loading

                    AutofillFillUiState.Loading,
                    is AutofillFillUiState.Result,
                    -> state
                }
            }

            is AutofillFillMutation.CandidatesAvailable -> {
                if (state == AutofillFillUiState.Loading) {
                    AutofillFillUiState.ShowCandidates(mutation.candidates)
                } else {
                    state
                }
            }

            is AutofillFillMutation.WebRecipientConfirmationRequired -> {
                if (state == AutofillFillUiState.Loading) {
                    AutofillFillUiState.ConfirmWebRecipient(mutation.packageName, mutation.webDomain)
                } else {
                    state
                }
            }

            is AutofillFillMutation.WebEntryConfirmationRequired -> {
                if (state == AutofillFillUiState.Loading) {
                    AutofillFillUiState.ConfirmWebEntry(mutation.packageName, mutation.webDomain, mutation.candidate)
                } else {
                    state
                }
            }

            is AutofillFillMutation.Completed -> {
                when {
                    state is AutofillFillUiState.Result -> {
                        state
                    }

                    mutation.result is AutofillFillResult.Failed ||
                        mutation.result == AutofillFillResult.Cancelled -> {
                        AutofillFillUiState.Result(mutation.result)
                    }

                    state == AutofillFillUiState.Loading -> {
                        AutofillFillUiState.Result(mutation.result)
                    }

                    else -> {
                        state
                    }
                }
            }

            AutofillFillMutation.ResultDelivered -> {
                if (state is AutofillFillUiState.Result) AutofillFillUiState.Initial else state
            }
        }
}
