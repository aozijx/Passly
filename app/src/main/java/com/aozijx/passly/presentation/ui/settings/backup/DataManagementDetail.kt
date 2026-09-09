package com.aozijx.passly.presentation.ui.settings.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.group.RoundedGroup
import com.aozijx.passly.presentation.ui.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.settings.backup.model.DataManagementEventHandler

@Composable
internal fun DataManagementDetail(
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
                    key = "data.database_reset",
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.settings_database_reset_title),
                    subtitle = stringResource(R.string.settings_database_reset_summary),
                    onClick = eventHandler::onOpenDatabaseReset,
                )
            )
        )
    }
}
