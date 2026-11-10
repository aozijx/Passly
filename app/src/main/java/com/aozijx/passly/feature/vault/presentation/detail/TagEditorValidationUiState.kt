package com.aozijx.passly.feature.vault.presentation.detail

import androidx.annotation.PluralsRes

sealed interface TagEditorValidationUiState {
    data object None : TagEditorValidationUiState

    data class Invalid(
        @param:PluralsRes @get:PluralsRes val pluralResourceId: Int,
        val quantity: Int,
    ) : TagEditorValidationUiState
}
