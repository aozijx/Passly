package com.aozijx.passly.feature.vault.presentation.detail.model

sealed interface DetailTagEditorUiModel {
    data object Hidden : DetailTagEditorUiModel

    data class Visible(
        val draftTags: DetailTagChipsUiState,
        val availableTags: Set<String>,
        val input: String,
        val suggestions: DetailTagChipsUiState,
        val validationError: TagEditorValidationErrorUiModel?,
        val overlay: TagEditorOverlayUiState,
        val dirty: Boolean,
    ) : DetailTagEditorUiModel
}

sealed interface TagEditorOverlayUiState {
    data object None : TagEditorOverlayUiState

    data object ConfirmDiscard : TagEditorOverlayUiState
}

enum class TagEditorValidationErrorUiModel {
    TOO_MANY_TAGS,
    TAG_TOO_LONG,
}
