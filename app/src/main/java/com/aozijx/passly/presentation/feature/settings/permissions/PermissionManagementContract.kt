package com.aozijx.passly.presentation.feature.settings.permissions

import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus

data class PermissionManagementUiState(
    val runtimePermissions: List<RuntimePermissionUiModel> = emptyList(),
    val backupDirectory: BackupDirectoryUiModel = BackupDirectoryUiModel(),
    val manifestCapabilities: List<ManifestCapabilityUiModel> = emptyList(),
)

data class RuntimePermissionUiModel(
    val permission: RuntimePermission,
    val access: RuntimePermissionAccess,
    val action: RuntimePermissionUiAction,
)

enum class RuntimePermissionUiAction {
    REQUEST,
    OPEN_SETTINGS,
    NONE,
}

data class BackupDirectoryUiModel(
    val uri: String? = null,
    val status: PersistedDirectoryAccessStatus =
        PersistedDirectoryAccessStatus.NOT_CONFIGURED,
    val action: BackupDirectoryUiAction = BackupDirectoryUiAction.SELECT,
    val canClear: Boolean = false,
)

enum class BackupDirectoryUiAction {
    SELECT,
    CHANGE,
    REAUTHORIZE,
}

data class ManifestCapabilityUiModel(
    val capability: ManifestCapability,
    val interactive: Boolean = false,
)

sealed interface PermissionManagementScreenAction {
    data class RuntimePermissionSelected(
        val permission: RuntimePermission,
    ) : PermissionManagementScreenAction

    data object SelectBackupDirectory : PermissionManagementScreenAction
    data object ClearBackupDirectory : PermissionManagementScreenAction
}

enum class PermissionManagementEffect {
    BackupDirectorySelectionCancelled,
    BackupDirectoryPermissionDenied,
    BackupDirectoryUnavailable,
}
