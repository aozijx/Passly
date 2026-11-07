package com.aozijx.passly.feature.settings.presentation.permissions

import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus

object PermissionManagementUiMapper {
    fun map(
        runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess>,
        directoryStatus: PersistedDirectoryAccessStatus,
    ): PermissionManagementUiState =
        PermissionManagementUiState(
            runtimePermissions =
                RuntimePermission.entries.map { permission ->
                    RuntimePermissionUiModel(
                        permission = permission,
                        access = runtimeAccess[permission] ?: RuntimePermissionAccess.NOT_APPLICABLE,
                    )
                },
            backupDirectory =
                BackupDirectoryUiModel(
                    status = directoryStatus,
                ),
            manifestCapabilities =
                ManifestCapability.entries.map { capability ->
                    ManifestCapabilityUiModel(capability = capability)
                },
        )
}
