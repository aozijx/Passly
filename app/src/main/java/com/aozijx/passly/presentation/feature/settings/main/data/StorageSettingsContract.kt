package com.aozijx.passly.presentation.feature.settings.main.data

data class StorageSettingsUiState(
    val cache: StorageSizeUiState = StorageSizeUiState.Loading,
    val logs: StorageSizeUiState = StorageSizeUiState.Loading,
)

sealed interface StorageSizeUiState {
    data object Loading : StorageSizeUiState
    data class Available(val formattedSize: String) : StorageSizeUiState
    data object Unavailable : StorageSizeUiState
}

sealed interface StorageSettingsAction {
    data object PageEntered : StorageSettingsAction
    data object ClearCache : StorageSettingsAction
    data object ClearLogs : StorageSettingsAction
}

sealed interface StorageSettingsEffect {
    data object CacheCleared : StorageSettingsEffect
    data object LogsCleared : StorageSettingsEffect
    data object LogClearFailed : StorageSettingsEffect
}
