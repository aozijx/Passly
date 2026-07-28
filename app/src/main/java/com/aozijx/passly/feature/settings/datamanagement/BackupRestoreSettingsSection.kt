package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
internal fun BackupRestoreSettingsSection(
    pathLabel: String,
    recentExportFileName: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onPickPath: () -> Unit,
    onTestWrite: () -> Unit,
    onClearPath: (() -> Unit)?
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_backup_restore_section))
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "backup.export",
                icon = Icons.Default.FileUpload,
                title = stringResource(R.string.settings_backup_export_action),
                subtitle = stringResource(R.string.settings_backup_export_action_description),
                onClick = onExport
            ),
            navigationSettingsGroupItem(
                key = "backup.import",
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.settings_backup_import_action),
                subtitle = stringResource(
                    R.string.settings_backup_import_action_description,
                    stringResource(R.string.app_name)
                ),
                onClick = onImport
            )
        )
    )

    Spacer(Modifier.height(24.dp))

    SettingsSectionTitle(text = stringResource(R.string.settings_backup_storage_section))
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "backup.directory",
                title = stringResource(R.string.settings_backup_default_directory),
                subtitle = pathLabel,
                onClick = onPickPath
            ),
            navigationSettingsGroupItem(
                key = "backup.test_write",
                title = stringResource(R.string.settings_backup_test_write),
                onClick = onTestWrite
            ),
            navigationSettingsGroupItem(
                key = "backup.last_export",
                title = stringResource(R.string.settings_backup_recent_export),
                subtitle = recentExportFileName,
                onClick = {}
            ),
            navigationSettingsGroupItem(
                key = "backup.clear_directory",
                visible = onClearPath != null,
                title = stringResource(R.string.settings_backup_clear_default_directory),
                onClick = onClearPath ?: {}
            )
        )
    )
}
