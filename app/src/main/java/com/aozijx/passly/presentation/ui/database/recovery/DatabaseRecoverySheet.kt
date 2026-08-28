package com.aozijx.passly.presentation.ui.database.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoveryEventHandler
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoveryPackageItem
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoveryPackageStatus
import com.aozijx.passly.presentation.ui.database.recovery.model.DatabaseRecoverySheetState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseRecoverySheet(
    state: DatabaseRecoverySheetState,
    eventHandler: DatabaseRecoveryEventHandler,
) {
    var deletePackageId by remember { mutableStateOf<String?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = {
            eventHandler.onClearResult()
            eventHandler.onDismiss()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_database_recovery_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.report?.let { report ->
                Text(
                    stringResource(
                        R.string.database_recovery_report_summary,
                        report.restoredEntries,
                        report.restoredAttachments,
                        report.restoredRevisions,
                        report.skippedConflicts,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.databaseCleared) {
                Text(
                    stringResource(R.string.database_cleared),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            when {
                state.isLoading -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.packages.isEmpty() -> Text(
                    stringResource(R.string.database_recovery_empty),
                )

                else -> state.packages.forEach { recoveryPackage ->
                    RecoveryPackageCard(
                        recoveryPackage = recoveryPackage,
                        state = state,
                        onScan = { eventHandler.onScan(recoveryPackage.id) },
                        onRestore = { eventHandler.onRestore(recoveryPackage.id) },
                        onToggleType = eventHandler::onToggleType,
                        onDelete = { deletePackageId = recoveryPackage.id },
                    )
                    HorizontalDivider()
                }
            }
            HorizontalDivider()
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy && !state.isClearingDatabase,
                onClick = { showClearConfirmation = true },
            ) {
                Text(
                    stringResource(R.string.settings_database_recovery_clear_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isClearingDatabase) showClearConfirmation = false
            },
            title = { Text(stringResource(R.string.settings_database_recovery_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_database_recovery_clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !state.isClearingDatabase,
                    onClick = {
                        showClearConfirmation = false
                        eventHandler.onClearDatabase()
                    },
                ) {
                    Text(
                        stringResource(R.string.settings_database_recovery_clear_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isClearingDatabase,
                    onClick = { showClearConfirmation = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    deletePackageId?.let { packageId ->
        AlertDialog(
            onDismissRequest = { deletePackageId = null },
            title = { Text(stringResource(R.string.database_recovery_delete_confirm_title)) },
            text = { Text(stringResource(R.string.database_recovery_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletePackageId = null
                        eventHandler.onDelete(packageId)
                    },
                ) {
                    Text(
                        stringResource(R.string.database_recovery_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletePackageId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RecoveryPackageCard(
    recoveryPackage: DatabaseRecoveryPackageItem,
    state: DatabaseRecoverySheetState,
    onScan: () -> Unit,
    onRestore: () -> Unit,
    onToggleType: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val busy = state.activePackageId == recoveryPackage.id
    val scan = state.scan?.takeIf { it.packageId == recoveryPackage.id }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(
                R.string.database_recovery_package_label,
                recoveryPackage.id.take(13),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(
                R.string.database_recovery_package_details,
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(recoveryPackage.createdAtEpochMs)),
                formatBytes(recoveryPackage.sizeBytes),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            recoveryStatusLabel(recoveryPackage.status),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
        )
        if (busy) CircularProgressIndicator()
        scan?.let {
            Text(
                stringResource(
                    R.string.database_recovery_scan_summary,
                    it.recoverableEntries,
                    it.conflictingEntries,
                    it.damagedEntries,
                    it.recoverableAttachments,
                ),
            )
            Text(
                stringResource(R.string.database_recovery_select_types),
                style = MaterialTheme.typography.titleSmall,
            )
            it.recoverableTypes.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = type.id in state.selectedTypeIds,
                        onCheckedChange = { onToggleType(type.id) },
                        enabled = !state.isBusy,
                    )
                    Text("${type.label} (${type.count})")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = onDelete, enabled = !state.isBusy) {
                Text(
                    stringResource(R.string.database_recovery_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = onScan, enabled = !state.isBusy) {
                Text(stringResource(R.string.database_recovery_scan))
            }
            if (scan != null && scan.recoverableEntries > 0) {
                Button(
                    onClick = onRestore,
                    enabled = !state.isBusy && state.selectedTypeIds.isNotEmpty(),
                ) { Text(stringResource(R.string.database_recovery_restore)) }
            }
        }
    }
}

@Composable
private fun recoveryStatusLabel(status: DatabaseRecoveryPackageStatus): String = stringResource(
    when (status) {
        DatabaseRecoveryPackageStatus.PENDING_SCAN -> R.string.database_recovery_status_pending
        DatabaseRecoveryPackageStatus.RECOVERABLE -> R.string.database_recovery_status_recoverable
        DatabaseRecoveryPackageStatus.PARTIALLY_RECOVERABLE -> R.string.database_recovery_status_partial
        DatabaseRecoveryPackageStatus.RESTORED -> R.string.database_recovery_status_restored
        DatabaseRecoveryPackageStatus.UNREADABLE -> R.string.database_recovery_status_unreadable
    },
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
