package com.aozijx.passly.presentation.feature.autofill.legacy.save

sealed interface LegacySaveUiState {
    data object Loading : LegacySaveUiState
    data object Saved : LegacySaveUiState
    data object Unrecoverable : LegacySaveUiState
}
