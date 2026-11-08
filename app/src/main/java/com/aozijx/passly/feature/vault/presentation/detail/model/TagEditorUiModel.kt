package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.PluralsRes

sealed interface DetailTagEditorUiModel {
    data object Hidden : DetailTagEditorUiModel

    data class Visible(
        val draftTags: DetailTagChipsUiState,
        val availableTags: Set<String>,
        val input: String,
        val suggestions: DetailTagChipsUiState,
        val validationMessage: TagEditorValidationMessageUiModel?,
        val showDiscardConfirmation: Boolean,
        val dirty: Boolean,
    ) : DetailTagEditorUiModel
}

data class TagEditorValidationMessageUiModel(
    @param:PluralsRes @get:PluralsRes val pluralResourceId: Int,
    val quantity: Int,
)
