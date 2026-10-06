package com.aozijx.passly.presentation.feature.settings.permissions

import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.platform.path.UriDisplayNameFormatter

object PermissionManagementUiMapper {
    fun map(
        runtimeAccess: Map<RuntimePermission, RuntimePermissionAccess>,
        directoryUri: String?,
        directoryStatus: PersistedDirectoryAccessStatus,
    ): PermissionManagementUiState = PermissionManagementUiState(
        runtimePermissions = RuntimePermission.entries.mapNotNull { permission ->
            val access = runtimeAccess[permission] ?: RuntimePermissionAccess.NOT_APPLICABLE
            if (access == RuntimePermissionAccess.NOT_APPLICABLE) return@mapNotNull null
            RuntimePermissionUiModel(
                permission = permission,
                access = access,
                action = access.toUiAction(),
            )
        },
        backupDirectory = BackupDirectoryUiModel(
            label = UriDisplayNameFormatter.format(directoryUri),
            status = directoryStatus,
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
    RuntimePermissionAccess.NOT_APPLICABLE ->
        error("Not-applicable permissions must be filtered before UI mapping")
}
