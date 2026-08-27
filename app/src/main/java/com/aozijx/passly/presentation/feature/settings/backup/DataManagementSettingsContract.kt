package com.aozijx.passly.presentation.feature.settings.backup

import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetailState

data class DataManagementSettingsUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
)

internal fun DataManagementSettingsUiState.toDetailState() = DataManagementDetailState(
    isAutoDownloadIcons = isAutoDownloadIcons,
)

sealed interface DataManagementSettingsUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataManagementSettingsUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataManagementSettingsUiAction
    data object ClearBackupDirectory : DataManagementSettingsUiAction
}
