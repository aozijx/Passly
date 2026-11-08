package com.aozijx.passly.feature.vault.presentation.detail.model

import androidx.annotation.PluralsRes
import com.aozijx.passly.feature.vault.presentation.detail.DetailTagEditorOverlay

sealed interface DetailTagEditorUiModel {
    data object Hidden : DetailTagEditorUiModel

    data class Visible(
        val draftTags: DetailTagChipsUiState,
        val availableTags: Set<String>,
        val input: String,
        val suggestions: DetailTagChipsUiState,
        val validationMessage: TagEditorValidationMessageUiModel?,
        val overlay: DetailTagEditorOverlay,
        val dirty: Boolean,
    ) : DetailTagEditorUiModel
}

data class TagEditorValidationMessageUiModel(
    @PluralsRes val pluralResourceId: Int,
    val quantity: Int,
)
