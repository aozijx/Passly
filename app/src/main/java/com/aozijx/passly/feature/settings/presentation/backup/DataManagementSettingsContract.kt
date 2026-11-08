package com.aozijx.passly.feature.settings.presentation.backup

import com.aozijx.passly.app.settings.ReadFailure

sealed interface DataManagementSettingsUiState {
    data object Loading : DataManagementSettingsUiState

    data class Failed(
        val failure: ReadFailure,
    ) : DataManagementSettingsUiState

    data class Ready(
        val directory: BackupDirectoryUiState,
    ) : DataManagementSettingsUiState
}

sealed interface BackupDirectoryUiState {
    data object Unconfigured : BackupDirectoryUiState

    data class Configured(val uri: String) : BackupDirectoryUiState {
        init {
            require(uri.isNotBlank())
        }
    }
}

val DataManagementSettingsUiState.isReady: Boolean
    get() = this is DataManagementSettingsUiState.Ready

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
