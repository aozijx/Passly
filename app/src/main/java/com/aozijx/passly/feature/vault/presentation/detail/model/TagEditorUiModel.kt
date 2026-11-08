package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.PluralsRes

sealed interface DetailTagEditorUiModel {
    data object Hidden : DetailTagEditorUiModel

    data class Visible(
        val draftTags: DetailTagChipsUiState,
        val availableTags: Set<String>,
        val input: String,
        val suggestions: DetailTagChipsUiState,
        val validation: TagEditorValidationUiState,
        val showDiscardConfirmation: Boolean,
        val dirty: Boolean,
    ) : DetailTagEditorUiModel
}

sealed interface TagEditorValidationUiState {
    data object None : TagEditorValidationUiState

    data class Invalid(
        @param:PluralsRes @get:PluralsRes val pluralResourceId: Int,
        val quantity: Int,
    ) : TagEditorValidationUiState
}
