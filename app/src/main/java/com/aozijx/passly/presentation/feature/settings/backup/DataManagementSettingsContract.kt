package com.aozijx.passly.presentation.feature.settings.backup

sealed interface DataManagementSettingsUiState {
    data object Loading : DataManagementSettingsUiState
    data class Ready(val directoryUri: String?) : DataManagementSettingsUiState
}

sealed interface DataManagementSettingsUiAction {
    data class BackupDirectoryPicked(val uri: String?) : DataManagementSettingsUiAction
    data object ClearBackupDirectory : DataManagementSettingsUiAction
}

enum class DataManagementSettingsEffect {
    BackupDirectorySelectionCancelled,
    BackupDirectoryPermissionDenied,
    BackupDirectoryUnavailable,
}
