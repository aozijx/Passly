package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.settingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
internal fun DataManagementDetail(
    state: DataUiState,
    isClearingDatabase: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit,
    onClearDatabase: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        DataSettingsSection(
            isAutoDownloadIcons = state.isAutoDownloadIcons,
            onAutoDownloadIconsChange = onAutoDownloadIconsChange
        )

        Spacer(modifier = Modifier.height(24.dp))
        SettingsSectionTitle(text = stringResource(R.string.settings_data_dangerous_actions))
        RoundedGroup(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            items = listOf(
                settingsGroupItem(
                    key = "data.clear_database",
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.settings_database_recovery_clear_action),
                    subtitle = stringResource(R.string.settings_data_clear_database_description),
                    onClick = { showClearConfirmation = true }
                )
            )
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!isClearingDatabase) showClearConfirmation = false
            },
            title = { Text(stringResource(R.string.settings_database_recovery_clear_confirm_title)) },
            text = {
                Text(stringResource(R.string.settings_database_recovery_clear_confirm_message))
            },
            confirmButton = {
                TextButton(
                    enabled = !isClearingDatabase,
                    onClick = {
                        showClearConfirmation = false
                        onClearDatabase()
                    }
                ) {
                    Text(
                        text = if (isClearingDatabase) {
                            stringResource(R.string.settings_data_clearing_database)
                        } else {
                            stringResource(R.string.settings_database_recovery_clear_confirm)
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isClearingDatabase,
                    onClick = { showClearConfirmation = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
