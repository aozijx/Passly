package com.aozijx.passly.presentation.ui.settings.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
import com.aozijx.passly.presentation.ui.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.backup.model.DataManagementDetailState
import com.aozijx.passly.presentation.ui.settings.backup.model.DataManagementEventHandler

@Composable
internal fun DataManagementDetail(
    state: DataManagementDetailState,
    eventHandler: DataManagementEventHandler,
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_data_storage))
        RoundedGroup(
            items = listOf(
                navigationSettingsGroupItem(
                    key = "data.trash",
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_trash_title),
                    subtitle = stringResource(R.string.settings_trash_description),
                    onClick = eventHandler::onOpenTrash,
                ),
                navigationSettingsGroupItem(
                    key = "data.database_recovery",
                    icon = Icons.Default.Restore,
                    title = stringResource(R.string.settings_database_recovery_title),
                    subtitle = stringResource(R.string.settings_database_recovery_summary),
                    onClick = eventHandler::onOpenDatabaseRecovery,
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
        DataSettingsSection(
            isAutoDownloadIcons = state.isAutoDownloadIcons,
            onAutoDownloadIconsChange = eventHandler::onAutoDownloadIconsChanged,
        )
    }
}
