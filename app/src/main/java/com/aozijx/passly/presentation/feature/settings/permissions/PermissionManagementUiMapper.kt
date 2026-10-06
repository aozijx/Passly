package com.aozijx.passly.presentation.feature.settings.permissions

import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus

object PermissionManagementUiMapper {
    fun map(
        runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess>,
        directoryUri: String?,
        directoryStatus: PersistedDirectoryAccessStatus,
    ): PermissionManagementUiState = PermissionManagementUiState(
        runtimePermissions = RuntimePermission.entries.map { permission ->
            val access = runtimeAccess[permission] ?: RuntimePermissionAccess.NOT_APPLICABLE
            RuntimePermissionUiModel(
                permission = permission,
                access = access,
                action = access.toUiAction(),
            )
        },
        backupDirectory = BackupDirectoryUiModel(
            uri = directoryUri,
            status = directoryStatus,
            action = directoryStatus.toUiAction(),
            canClear = !directoryUri.isNullOrBlank(),
        ),
        manifestCapabilities = ManifestCapability.entries.map { capability ->
            ManifestCapabilityUiModel(capability = capability)
        },
    )
}

private fun RuntimePermissionAccess.toUiAction(): RuntimePermissionUiAction = when (this) {
    RuntimePermissionAccess.GRANTED,
    RuntimePermissionAccess.SETTINGS_REQUIRED -> RuntimePermissionUiAction.OPEN_SETTINGS
    RuntimePermissionAccess.REQUESTABLE -> RuntimePermissionUiAction.REQUEST
    RuntimePermissionAccess.NOT_APPLICABLE -> RuntimePermissionUiAction.NONE
}

private fun PersistedDirectoryAccessStatus.toUiAction(): BackupDirectoryUiAction = when (this) {
    PersistedDirectoryAccessStatus.NOT_CONFIGURED -> BackupDirectoryUiAction.SELECT
    PersistedDirectoryAccessStatus.GRANTED -> BackupDirectoryUiAction.CHANGE
    PersistedDirectoryAccessStatus.GRANT_MISSING,
    PersistedDirectoryAccessStatus.UNAVAILABLE -> BackupDirectoryUiAction.REAUTHORIZE
}
