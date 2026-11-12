package com.aozijx.passly.feature.settings.presentation.backup

sealed interface BackupDirectoryUiState {
    data object Unconfigured : BackupDirectoryUiState

    data class Configured(
        val uri: String,
    ) : BackupDirectoryUiState {
        init {
            require(uri.isNotBlank())
        }
    }
}

sealed interface DataManagementSettingsUiAction {
    data object RetryRead : DataManagementSettingsUiAction

    data class BackupDirectoryPicked(
        val uri: String?,
        val pageOwner: Any,
        val requestId: String,
    ) : DataManagementSettingsUiAction

    data object ClearBackupDirectory : DataManagementSettingsUiAction
}

enum class DataManagementSettingsEffect {
    BackupDirectorySelectionCancelled,
    BackupDirectoryPermissionDenied,
    BackupDirectoryUnavailable,
    BackupDirectoryCleared,
    BackupDirectoryOperationFailed,
}
