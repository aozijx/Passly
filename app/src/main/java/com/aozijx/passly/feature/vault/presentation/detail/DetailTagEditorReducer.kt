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
                val tags =
                    when (val normalized = EntryTagPolicy.normalize(mutation.currentTags)) {
                        is EntryTagNormalizationResult.Valid -> normalized.tags
                        is EntryTagNormalizationResult.Invalid -> mutation.currentTags
                    }
                DetailTagEditorState.Open(
                    initialTags = tags,
                    availableTags = mutation.availableTags,
                )
            }

            else -> {
                when (state) {
                    DetailTagEditorState.Closed -> state
                    is DetailTagEditorState.Open -> reduceOpen(state, mutation)
                }
            }
        }

    private fun reduceOpen(
        state: DetailTagEditorState.Open,
        mutation: DetailTagEditorMutation,
    ): DetailTagEditorState =
        when (mutation) {
            is DetailMutation.TagEditorOpened -> {
                state
            }

            is DetailMutation.TagInputChanged -> {
                state.copy(
                    input = mutation.value,
                    suggestions =
                        DetailTagSuggestions.forInput(
                            existingTags = state.availableTags,
                            prefix = mutation.value.substringAfterLast(',').substringAfterLast('\n'),
                            selectedTags = state.draftTags,
                        ),
                    validationError = null,
                    overlay = DetailTagEditorOverlay.None,
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
                            validationError = null,
                        )
                    }

                    is EntryTagNormalizationResult.Invalid -> {
                        state.copy(
                            validationError = normalized.error,
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
                    validationError = null,
                )
            }

            DetailMutation.TagEditorDismissRequested -> {
                if (state.dirty) {
                    state.copy(overlay = DetailTagEditorOverlay.ConfirmDiscard)
                } else {
                    DetailTagEditorState.Closed
                }
            }

            DetailMutation.TagEditorDiscardConfirmed -> {
                DetailTagEditorState.Closed
            }

            DetailMutation.TagEditorDiscardCancelled -> {
                state.copy(
                    overlay = DetailTagEditorOverlay.None,
                )
            }
        }
}
