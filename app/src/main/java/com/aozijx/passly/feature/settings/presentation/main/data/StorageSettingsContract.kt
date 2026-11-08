package com.aozijx.passly.feature.settings.presentation.main.data

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode

data class StorageSettingsUiState(
    val cache: StorageSizeUiState = StorageSizeUiState.Loading,
    val logs: StorageSizeUiState = StorageSizeUiState.Loading,
    val logRecordingMode: RecoverableReadState<DiagnosticsLogRecordingMode> = RecoverableReadState.Loading,
)

sealed interface StorageSizeUiState {
    val formattedSize: String? get() = null
    val isLoading: Boolean get() = false

    data object Loading : StorageSizeUiState {
        override val isLoading = true
    }

    data class Available(
        override val formattedSize: String,
    ) : StorageSizeUiState

    data object Unavailable : StorageSizeUiState
}

sealed interface StorageSettingsAction {
    data object RetryLogRecordingMode : StorageSettingsAction

    data object ClearCache : StorageSettingsAction

    data object ClearLogs : StorageSettingsAction

    data class SetLogRecordingMode(
        val mode: DiagnosticsLogRecordingMode,
    ) : StorageSettingsAction
}

sealed interface StorageSettingsEffect {
    data object CacheCleared : StorageSettingsEffect

    data object CacheClearFailed : StorageSettingsEffect

    data object LogsCleared : StorageSettingsEffect

    data object LogClearFailed : StorageSettingsEffect

    data object LogRecordingSettingsSaveFailed : StorageSettingsEffect
}
