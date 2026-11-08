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
        val validationError: EntryTagValidationError? = null,
        val overlay: DetailTagEditorOverlay = DetailTagEditorOverlay.None,
    ) : DetailTagEditorState {
        val dirty: Boolean get() = draftTags != initialTags || input.isNotBlank()
    }
}

sealed interface DetailTagEditorOverlay {
    data object None : DetailTagEditorOverlay

    data object ConfirmDiscard : DetailTagEditorOverlay
}
