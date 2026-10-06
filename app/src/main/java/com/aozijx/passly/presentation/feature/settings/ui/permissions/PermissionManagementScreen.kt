package com.aozijx.passly.presentation.feature.settings.ui.permissions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.feature.settings.permissions.BackupDirectoryUiAction
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementScreenAction
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementUiState
import com.aozijx.passly.presentation.feature.settings.permissions.RuntimePermissionUiAction
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.settingsGroupItem

@Composable
internal fun PermissionManagementScreen(
    state: PermissionManagementUiState,
    onAction: (PermissionManagementScreenAction) -> Unit,
    onBack: (() -> Unit)?,
) {
    SettingsSecondaryPage(
        title = stringResource(R.string.settings_page_permissions),
        onBack = onBack,
    ) {
        item {
            SettingsSectionTitle(
                text = stringResource(R.string.settings_permissions_runtime_section),
            )
            SegmentedSettingsGroup(
                items = state.runtimePermissions.map { item ->
                    val title = stringResource(item.permission.titleRes)
                    val subtitle = stringResource(item.access.summaryRes)
                    when (item.action) {
                        RuntimePermissionUiAction.NONE -> settingsGroupItem(
                            key = "permissions.runtime.${item.permission.name}",
                            title = title,
                            subtitle = subtitle,
                            enabled = false,
                        )
                        RuntimePermissionUiAction.REQUEST,
                        RuntimePermissionUiAction.OPEN_SETTINGS -> navigationSettingsGroupItem(
                            key = "permissions.runtime.${item.permission.name}",
                            title = title,
                            subtitle = subtitle,
                            value = stringResource(item.action.labelRes),
                            onClick = {
                                onAction(
                                    PermissionManagementScreenAction.RuntimePermissionSelected(
                                        item.permission,
                                    ),
                                )
                            },
                        )
                    }
                },
            )
        }

        item {
            SettingsSectionTitle(
                text = stringResource(R.string.settings_permissions_backup_section),
            )
            SegmentedSettingsGroup(
                items = buildList {
                    add(
                        navigationSettingsGroupItem(
                            key = "permissions.backup.directory",
                            title = stringResource(R.string.settings_permissions_backup_directory),
                            subtitle = stringResource(state.backupDirectory.status.summaryRes),
                            value = stringResource(state.backupDirectory.action.labelRes),
                            onClick = {
                                onAction(
                                    PermissionManagementScreenAction.SelectBackupDirectory,
                                )
                            },
                        ),
                    )
                    if (state.backupDirectory.canClear) {
                        add(
                            navigationSettingsGroupItem(
                                key = "permissions.backup.clear",
                                title = stringResource(
                                    R.string.settings_permissions_backup_clear,
                                ),
                                subtitle = stringResource(
                                    R.string.settings_backup_clear_directory_message,
                                ),
                                onClick = {
                                    onAction(
                                        PermissionManagementScreenAction.ClearBackupDirectory,
                                    )
                                },
                            ),
                        )
                    }
                },
            )
        }

        item {
            SettingsSectionTitle(
                text = stringResource(R.string.settings_permissions_capabilities_section),
            )
            SegmentedSettingsGroup(
                items = state.manifestCapabilities.map { item ->
                    settingsGroupItem(
                        key = "permissions.capability.${item.capability.name}",
                        title = stringResource(item.capability.titleRes),
                        subtitle = stringResource(item.capability.summaryRes),
                    )
                },
            )
        }
    }
}

private val RuntimePermission.titleRes: Int
    get() = when (this) {
        RuntimePermission.CAMERA -> R.string.settings_permissions_camera
        RuntimePermission.POST_NOTIFICATIONS -> R.string.settings_permissions_notifications
    }

private val RuntimePermissionAccess.summaryRes: Int
    get() = when (this) {
        RuntimePermissionAccess.GRANTED -> R.string.settings_permissions_status_granted
        RuntimePermissionAccess.REQUESTABLE -> R.string.settings_permissions_status_requestable
        RuntimePermissionAccess.SETTINGS_REQUIRED ->
            R.string.settings_permissions_status_settings_required
        RuntimePermissionAccess.NOT_APPLICABLE ->
            R.string.settings_permissions_status_not_applicable
    }

private val RuntimePermissionUiAction.labelRes: Int
    get() = when (this) {
        RuntimePermissionUiAction.REQUEST -> R.string.settings_permissions_action_request
        RuntimePermissionUiAction.OPEN_SETTINGS -> R.string.settings_permissions_action_manage
        RuntimePermissionUiAction.NONE -> error("Non-interactive permissions have no action label")
    }

private val PersistedDirectoryAccessStatus.summaryRes: Int
    get() = when (this) {
        PersistedDirectoryAccessStatus.NOT_CONFIGURED ->
            R.string.settings_permissions_backup_not_configured
        PersistedDirectoryAccessStatus.GRANTED ->
            R.string.settings_permissions_backup_granted
        PersistedDirectoryAccessStatus.GRANT_MISSING ->
            R.string.settings_permissions_backup_grant_missing
        PersistedDirectoryAccessStatus.UNAVAILABLE ->
            R.string.settings_permissions_backup_unavailable
    }

private val BackupDirectoryUiAction.labelRes: Int
    get() = when (this) {
        BackupDirectoryUiAction.SELECT -> R.string.settings_permissions_backup_select
        BackupDirectoryUiAction.CHANGE -> R.string.settings_permissions_backup_change
        BackupDirectoryUiAction.REAUTHORIZE ->
            R.string.settings_permissions_backup_reauthorize
    }

private val ManifestCapability.titleRes: Int
    get() = when (this) {
        ManifestCapability.INTERNET -> R.string.settings_permissions_capability_network
        ManifestCapability.VIBRATE -> R.string.settings_permissions_capability_vibration
        ManifestCapability.USE_BIOMETRIC -> R.string.settings_permissions_capability_biometric
        ManifestCapability.PACKAGE_VISIBILITY ->
            R.string.settings_permissions_capability_app_visibility
    }

private val ManifestCapability.summaryRes: Int
    get() = when (this) {
        ManifestCapability.INTERNET -> R.string.settings_permissions_capability_network_summary
        ManifestCapability.VIBRATE -> R.string.settings_permissions_capability_vibration_summary
        ManifestCapability.USE_BIOMETRIC ->
            R.string.settings_permissions_capability_biometric_summary
        ManifestCapability.PACKAGE_VISIBILITY ->
            R.string.settings_permissions_capability_app_visibility_summary
    }
