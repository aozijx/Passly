package com.aozijx.passly.feature.settings.presentation.ui.permissions

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.core.permission.catalog.ManifestCapability
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.core.platform.storage.PersistedDirectoryAccessStatus
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementScreenAction
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementUiState
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.model.SettingsItemValue
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
                items =
                    state.runtimePermissions.map { item ->
                        navigationSettingsGroupItem(
                            key = "permissions.runtime.${item.permission.name}",
                            title = stringResource(item.permission.titleRes),
                            subtitle = stringResource(item.access.summaryRes),
                            value = SettingsItemValue.Text(stringResource(item.access.actionLabelRes)),
                            onClick = {
                                onAction(PermissionManagementScreenAction.RuntimePermissionSelected(item.permission))
                            },
                        )
                    },
            )
        }

        item {
            SettingsSectionTitle(
                text = stringResource(R.string.settings_permissions_backup_section),
                modifier = Modifier.padding(top = 24.dp),
            )
            when (state.backupDirectoryReadState) {
                RecoverableReadState.Loading -> {
                    CircularProgressIndicator()
                }

                is RecoverableReadState.Failed -> {
                    Text(stringResource(R.string.settings_read_failed))
                    TextButton(onClick = { onAction(PermissionManagementScreenAction.RetryBackupDirectoryRead) }) {
                        Text(stringResource(R.string.retry))
                    }
                }

                is RecoverableReadState.Ready -> {
                    val directoryStatus = stringResource(state.backupDirectory.status.summaryRes)
                    SegmentedSettingsGroup(
                        items =
                            listOf(
                                navigationSettingsGroupItem(
                                    key = "permissions.backup.directory",
                                    title = stringResource(R.string.settings_permissions_backup_directory),
                                    subtitle = directoryStatus,
                                    value = SettingsItemValue.Text(stringResource(R.string.settings_permissions_backup_manage)),
                                    onClick = {
                                        onAction(PermissionManagementScreenAction.OpenBackupSettings)
                                    },
                                ),
                            ),
                    )
                }
            }
        }

        item {
            SettingsSectionTitle(
                text = stringResource(R.string.settings_permissions_capabilities_section),
                modifier = Modifier.padding(top = 24.dp),
            )
            SegmentedSettingsGroup(
                items =
                    state.manifestCapabilities.map { item ->
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
    get() =
        when (this) {
            RuntimePermission.CAMERA -> R.string.settings_permissions_camera
            RuntimePermission.POST_NOTIFICATIONS -> R.string.settings_permissions_notifications
        }

private val RuntimePermissionAccess.summaryRes: Int
    get() =
        when (this) {
            RuntimePermissionAccess.GRANTED -> R.string.settings_permissions_status_granted
            RuntimePermissionAccess.REQUESTABLE -> R.string.settings_permissions_status_requestable
            RuntimePermissionAccess.SETTINGS_REQUIRED -> R.string.settings_permissions_status_settings_required
            RuntimePermissionAccess.NOT_APPLICABLE -> R.string.settings_permissions_status_not_applicable
        }

private val RuntimePermissionAccess.actionLabelRes: Int
    get() =
        when (this) {
            RuntimePermissionAccess.GRANTED,
            RuntimePermissionAccess.SETTINGS_REQUIRED,
            RuntimePermissionAccess.NOT_APPLICABLE,
            -> R.string.settings_permissions_action_manage

            RuntimePermissionAccess.REQUESTABLE -> R.string.settings_permissions_action_request
        }

private val PersistedDirectoryAccessStatus.summaryRes: Int
    get() =
        when (this) {
            PersistedDirectoryAccessStatus.NOT_CONFIGURED -> {
                R.string.settings_permissions_backup_not_configured
            }

            PersistedDirectoryAccessStatus.GRANTED -> {
                R.string.settings_permissions_backup_granted
            }

            PersistedDirectoryAccessStatus.GRANT_MISSING -> {
                R.string.settings_permissions_backup_grant_missing
            }

            PersistedDirectoryAccessStatus.UNAVAILABLE -> {
                R.string.settings_permissions_backup_unavailable
            }
        }

private val ManifestCapability.titleRes: Int
    get() =
        when (this) {
            ManifestCapability.INTERNET -> {
                R.string.settings_permissions_capability_network
            }

            ManifestCapability.VIBRATE -> {
                R.string.settings_permissions_capability_vibration
            }

            ManifestCapability.USE_BIOMETRIC -> {
                R.string.settings_permissions_capability_biometric
            }

            ManifestCapability.PACKAGE_VISIBILITY -> {
                R.string.settings_permissions_capability_app_visibility
            }
        }

private val ManifestCapability.summaryRes: Int
    get() =
        when (this) {
            ManifestCapability.INTERNET -> {
                R.string.settings_permissions_capability_network_summary
            }

            ManifestCapability.VIBRATE -> {
                R.string.settings_permissions_capability_vibration_summary
            }

            ManifestCapability.USE_BIOMETRIC -> {
                R.string.settings_permissions_capability_biometric_summary
            }

            ManifestCapability.PACKAGE_VISIBILITY -> {
                R.string.settings_permissions_capability_app_visibility_summary
            }
        }
