package com.aozijx.passly.feature.settings.presentation.main.data

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.DiagnosticsLogRecordingMode
import com.aozijx.passly.presentation.shared.components.group.model.SettingsItemValue

data class StorageSettingsUiState(
    val cache: SettingsItemValue = SettingsItemValue.Loading,
    val logs: SettingsItemValue = SettingsItemValue.Loading,
    val logRecordingMode: RecoverableReadState<DiagnosticsLogRecordingMode> = RecoverableReadState.Loading,
)

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
