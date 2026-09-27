package com.aozijx.passly.presentation.feature.settings.backup

data class DataManagementSettingsUiState(
    val directoryUri: String? = null,
)

sealed interface DataManagementSettingsUiAction {
    data class BackupDirectoryPicked(val uri: String?) : DataManagementSettingsUiAction
    data object ClearBackupDirectory : DataManagementSettingsUiAction
}

enum class DataManagementSettingsEffect {
    BackupDirectorySelectionCancelled,
    BackupDirectoryPermissionDenied,
    BackupDirectoryUnavailable,
}
