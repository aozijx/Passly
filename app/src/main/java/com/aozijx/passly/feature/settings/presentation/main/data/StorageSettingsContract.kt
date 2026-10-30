package com.aozijx.passly.feature.settings.presentation.main.data

import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode

data class StorageSettingsUiState(
    val cache: StorageSizeUiState = StorageSizeUiState.Loading,
    val logs: StorageSizeUiState = StorageSizeUiState.Loading,
    val logRecordingMode: DiagnosticsLogRecordingMode = DiagnosticsLogRecordingMode.DEFAULT,
)

sealed interface StorageSizeUiState {
    data object Loading : StorageSizeUiState
    data class Available(val formattedSize: String) : StorageSizeUiState
    data object Unavailable : StorageSizeUiState
}

sealed interface StorageSettingsAction {
    data object PageEntered : StorageSettingsAction
    data object PageExited : StorageSettingsAction
    data object ClearCache : StorageSettingsAction
    data object ClearLogs : StorageSettingsAction
    data class SetLogRecordingMode(val mode: DiagnosticsLogRecordingMode) : StorageSettingsAction
}

sealed interface StorageSettingsEffect {
    data object CacheCleared : StorageSettingsEffect
    data object LogsCleared : StorageSettingsEffect
    data object LogClearFailed : StorageSettingsEffect
    data object LogRecordingSettingsSaveFailed : StorageSettingsEffect
}
