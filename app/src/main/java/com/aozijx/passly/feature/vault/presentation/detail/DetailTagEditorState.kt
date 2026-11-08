package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.entry.policy.EntryTagValidationError

sealed interface DetailTagEditorState {
    data object Closed : DetailTagEditorState

    data class Open(
        val initialTags: Set<String>,
        val draftTags: Set<String> = initialTags,
        val availableTags: Set<String> = emptySet(),
        val input: String = "",
        val suggestions: List<String> = emptyList(),
        val validation: DetailTagValidationState = DetailTagValidationState.None,
        val showDiscardConfirmation: Boolean = false,
    ) : DetailTagEditorState {
        val dirty: Boolean get() = draftTags != initialTags || input.isNotBlank()
    }
}

sealed interface DetailTagValidationState {
    data object None : DetailTagValidationState

    data class Invalid(val error: EntryTagValidationError) : DetailTagValidationState
}
