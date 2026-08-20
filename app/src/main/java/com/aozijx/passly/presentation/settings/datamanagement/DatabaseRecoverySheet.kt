package com.aozijx.passly.presentation.settings.datamanagement

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
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsAction
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryPackage
import com.aozijx.passly.data.local.database.model.DatabaseRecoveryStatus
import com.aozijx.passly.domain.entry.model.EntryType
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatabaseRecoverySheet(
    visible: Boolean,
    state: DataManagementSettingsUiState,
    onDismiss: () -> Unit,
    onAction: (DataManagementSettingsAction) -> Unit,
) {
    if (!visible) return
    var deletePackageId by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = {
            onAction(DataManagementSettingsAction.ClearRecoveryResult)
            onDismiss()
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
            state.recoveryError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.recoveryReport?.let { report ->
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
            when {
                state.isRecoveryLoading -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.recoveryPackages.isEmpty() -> Text(
                    stringResource(R.string.database_recovery_empty),
                )

                else -> state.recoveryPackages.forEach { recoveryPackage ->
                    RecoveryPackageCard(
                        recoveryPackage = recoveryPackage,
                        state = state,
                        onScan = {
                            onAction(
                                DataManagementSettingsAction.ScanRecoveryPackage(
                                    recoveryPackage.id,
                                ),
                            )
                        },
                        onRestore = {
                            onAction(
                                DataManagementSettingsAction.RestoreRecoveryPackage(
                                    recoveryPackage.id,
                                ),
                            )
                        },
                        onToggleType = {
                            onAction(DataManagementSettingsAction.ToggleRecoveryType(it))
                        },
                        onDelete = { deletePackageId = recoveryPackage.id },
                    )
                    HorizontalDivider()
                }
            }
        }
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
                        onAction(DataManagementSettingsAction.DeleteRecoveryPackage(packageId))
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
    recoveryPackage: DatabaseRecoveryPackage,
    state: DataManagementSettingsUiState,
    onScan: () -> Unit,
    onRestore: () -> Unit,
    onToggleType: (EntryType) -> Unit,
    onDelete: () -> Unit,
) {
    val busy = state.activeRecoveryPackageId == recoveryPackage.id
    val scan = state.recoveryScan?.takeIf { it.packageId == recoveryPackage.id }
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
            it.recoverableByType.forEach { (type, count) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = type in state.selectedRecoveryTypes,
                        onCheckedChange = { onToggleType(type) },
                        enabled = !state.isRecoveryBusy,
                    )
                    Text("${type.name.replace('_', ' ')} ($count)")
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = onDelete, enabled = !state.isRecoveryBusy) {
                Text(
                    stringResource(R.string.database_recovery_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = onScan, enabled = !state.isRecoveryBusy) {
                Text(stringResource(R.string.database_recovery_scan))
            }
            if (scan != null && scan.recoverableEntries > 0) {
                Button(
                    onClick = onRestore,
                    enabled = !state.isRecoveryBusy && state.selectedRecoveryTypes.isNotEmpty(),
                ) { Text(stringResource(R.string.database_recovery_restore)) }
            }
        }
    }
}

@Composable
private fun recoveryStatusLabel(status: DatabaseRecoveryStatus): String = stringResource(
    when (status) {
        DatabaseRecoveryStatus.PENDING_SCAN -> R.string.database_recovery_status_pending
        DatabaseRecoveryStatus.RECOVERABLE -> R.string.database_recovery_status_recoverable
        DatabaseRecoveryStatus.PARTIALLY_RECOVERABLE -> R.string.database_recovery_status_partial
        DatabaseRecoveryStatus.RESTORED -> R.string.database_recovery_status_restored
        DatabaseRecoveryStatus.UNREADABLE -> R.string.database_recovery_status_unreadable
    },
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
