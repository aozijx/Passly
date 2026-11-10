package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.policy.EntryTagValidationError
import com.aozijx.passly.feature.vault.presentation.detail.model.DetailTagChipsUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.TagEditorValidationUiState
import com.aozijx.passly.feature.vault.presentation.detail.model.toDetailTagChipsUiState

sealed interface DetailTagEditorState {
    data object Closed : DetailTagEditorState

    data class Open(
        val initialTags: Set<String>,
        val draftTags: Set<String> = initialTags,
        val availableTags: Set<String> = emptySet(),
        val input: String = "",
        val suggestions: List<String> = emptyList(),
        val validation: TagEditorValidationUiState = TagEditorValidationUiState.None,
        val showDiscardConfirmation: Boolean = false,
    ) : DetailTagEditorState {
        val dirty: Boolean get() = draftTags != initialTags || input.isNotBlank()

        val draftTagsUiState: DetailTagChipsUiState
            get() = draftTags.toDetailTagChipsUiState()

        val suggestionsUiState: DetailTagChipsUiState
            get() = suggestions.toDetailTagChipsUiState()
    }
}

internal fun EntryTagValidationError.toTagEditorValidationUiState(): TagEditorValidationUiState.Invalid =
    when (this) {
        is EntryTagValidationError.TooMany ->
            TagEditorValidationUiState.Invalid(
                pluralResourceId = R.plurals.vault_detail_tag_too_many,
                quantity = maximum,
            )

        is EntryTagValidationError.TooLong ->
            TagEditorValidationUiState.Invalid(
                pluralResourceId = R.plurals.vault_detail_tag_too_long,
                quantity = maximum,
            )
}
