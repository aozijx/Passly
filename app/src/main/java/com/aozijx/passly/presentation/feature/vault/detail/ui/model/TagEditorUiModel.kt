package com.aozijx.passly.presentation.feature.vault.detail.ui.model

sealed interface DetailTagEditorUiModel {
    data object Hidden : DetailTagEditorUiModel

    data class Visible(
        val initialTags: Set<String>,
        val draftTags: Set<String>,
        val availableTags: Set<String>,
        val input: String,
        val suggestions: List<String>,
        val validationError: TagEditorValidationErrorUiModel?,
        val overlay: TagEditorOverlayUiState,
    ) : DetailTagEditorUiModel {
        val dirty: Boolean get() = draftTags != initialTags || input.isNotBlank()
    }
}

sealed interface TagEditorOverlayUiState {
    data object None : TagEditorOverlayUiState
    data object ConfirmDiscard : TagEditorOverlayUiState
}

enum class TagEditorValidationErrorUiModel {
    TOO_MANY_TAGS,
    TAG_TOO_LONG,
}
