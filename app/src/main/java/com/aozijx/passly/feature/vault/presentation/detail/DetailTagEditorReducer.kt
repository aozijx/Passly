package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.policy.EntryTagNormalizationResult
import com.aozijx.passly.domain.entry.policy.EntryTagPolicy

internal object DetailTagEditorReducer {
    fun reduce(
        state: DetailTagEditorState,
        mutation: DetailTagEditorMutation,
    ): DetailTagEditorState =
        when (mutation) {
            is DetailMutation.TagEditorOpened -> {
                mutation.toEditorState()
            }

            is DetailTagEditorEditMutation -> {
                when (state) {
                    DetailTagEditorState.Closed -> state
                    is DetailTagEditorState.Open -> reduceOpen(state, mutation)
                }
            }
        }

    private fun DetailMutation.TagEditorOpened.toEditorState(): DetailTagEditorState.Open {
        val tags =
            when (val normalized = EntryTagPolicy.normalize(currentTags)) {
                is EntryTagNormalizationResult.Valid -> normalized.tags
                is EntryTagNormalizationResult.Invalid -> currentTags
            }
        return DetailTagEditorState.Open(
            initialTags = tags,
            availableTags = availableTags,
        )
    }

    private fun reduceOpen(
        state: DetailTagEditorState.Open,
        mutation: DetailTagEditorEditMutation,
    ): DetailTagEditorState =
        when (mutation) {
            is DetailMutation.TagInputChanged -> {
                state.copy(
                    input = mutation.value,
                    suggestions =
                        DetailTagSuggestions.forInput(
                            existingTags = state.availableTags,
                            prefix = mutation.value.substringAfterLast(',').substringAfterLast('\n'),
                            selectedTags = state.draftTags,
                        ),
                    validation = TagEditorValidationUiState.None,
                    showDiscardConfirmation = false,
                )
            }

            is DetailMutation.TagSubmitted -> {
                when (
                    val normalized = EntryTagPolicy.normalize(state.draftTags + mutation.value)
                ) {
                    is EntryTagNormalizationResult.Valid -> {
                        state.copy(
                            draftTags = normalized.tags,
                            input = "",
                            suggestions = emptyList(),
                            validation = TagEditorValidationUiState.None,
                        )
                    }

                    is EntryTagNormalizationResult.Invalid -> {
                        state.copy(
                            validation = normalized.error.toTagEditorValidationUiState(),
                        )
                    }
                }
            }

            is DetailMutation.TagRemoved -> {
                state.copy(
                    draftTags =
                        state.draftTags
                            .filterNot { it.equals(mutation.value, ignoreCase = true) }
                            .toCollection(linkedSetOf()),
                    validation = TagEditorValidationUiState.None,
                )
            }

            DetailMutation.TagEditorDismissRequested -> {
                if (state.dirty) {
                    state.copy(showDiscardConfirmation = true)
                } else {
                    DetailTagEditorState.Closed
                }
            }

            DetailMutation.TagEditorDiscardConfirmed -> {
                DetailTagEditorState.Closed
            }

            DetailMutation.TagEditorDiscardCancelled -> {
                state.copy(
                    showDiscardConfirmation = false,
                )
            }
        }
}
