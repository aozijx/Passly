package com.aozijx.passly.feature.autofill.presentation.legacy.save

sealed interface LegacySaveUiState {
    data object Loading : LegacySaveUiState
    data object Saved : LegacySaveUiState
    data object Unrecoverable : LegacySaveUiState
}
