package com.aozijx.passly.presentation.ui.settings.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
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
import com.aozijx.passly.presentation.ui.vault.list.trash.TrashBottomSheet
import com.aozijx.passly.presentation.ui.vault.list.trash.TrashEntryUiModel
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.settingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
internal fun DataManagementDetail(
    state: DataManagementDetailState,
    recoveryState: DatabaseRecoverySheetState,
    isClearingDatabase: Boolean,
    onAutoDownloadIconsChange: (Boolean) -> Unit,
    onRestoreTrashEntry: (entryId: String, expectedVersion: Int) -> Unit,
    onDeleteTrashEntry: (entryId: String, expectedVersion: Int) -> Unit,
    onEmptyTrash: () -> Unit,
    onClearTrashError: () -> Unit,
    onRefreshRecoveryPackages: () -> Unit,
    onClearRecoveryResult: () -> Unit,
    onScanRecoveryPackage: (String) -> Unit,
    onRestoreRecoveryPackage: (String) -> Unit,
    onToggleRecoveryType: (String) -> Unit,
    onDeleteRecoveryPackage: (String) -> Unit,
    onClearDatabase: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showTrash by remember { mutableStateOf(false) }
    var showDatabaseRecovery by remember { mutableStateOf(false) }

    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionTitle(text = stringResource(R.string.settings_data_storage))
        RoundedGroup(
            items = listOf(
                navigationSettingsGroupItem(
                    key = "data.trash",
                    icon = Icons.Default.DeleteSweep,
                    title = stringResource(R.string.settings_trash_title),
                    subtitle = when {
                        state.isTrashLoading ->
                            stringResource(R.string.settings_trash_loading)

                        state.deletedEntries.isEmpty() ->
                            stringResource(R.string.settings_trash_empty)

                        else ->
                            stringResource(
                                R.string.settings_trash_count,
                                state.deletedEntries.size
                            )
                    },
                    onClick = { showTrash = true }
                ),
                navigationSettingsGroupItem(
                    key = "data.database_recovery",
                    icon = Icons.Default.Restore,
                    title = stringResource(R.string.settings_database_recovery_title),
                    subtitle = stringResource(R.string.settings_database_recovery_summary),
                    onClick = {
                        showDatabaseRecovery = true
                        onRefreshRecoveryPackages()
                    },
                )
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
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

    TrashBottomSheet(
        visible = showTrash,
        entries = state.deletedEntries,
        isLoading = state.isTrashLoading,
        activeEntryId = state.activeTrashEntryId,
        isEmptying = state.isEmptyingTrash,
        error = state.trashError,
        onDismiss = {
            showTrash = false
            onClearTrashError()
        },
        onRestore = { entry ->
            onRestoreTrashEntry(entry.id, entry.version)
        },
        onDelete = { entry ->
            onDeleteTrashEntry(entry.id, entry.version)
        },
        onEmpty = onEmptyTrash,
        onClearError = onClearTrashError
    )

    DatabaseRecoverySheet(
        visible = showDatabaseRecovery,
        state = recoveryState,
        onDismiss = { showDatabaseRecovery = false },
        onClearResult = onClearRecoveryResult,
        onScan = onScanRecoveryPackage,
        onRestore = onRestoreRecoveryPackage,
        onToggleType = onToggleRecoveryType,
        onDelete = onDeleteRecoveryPackage,
    )

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

internal data class DataManagementDetailState(
    val isAutoDownloadIcons: Boolean,
    val deletedEntries: List<TrashEntryUiModel>,
    val isTrashLoading: Boolean,
    val activeTrashEntryId: String?,
    val isEmptyingTrash: Boolean,
    val trashError: String?,
)
