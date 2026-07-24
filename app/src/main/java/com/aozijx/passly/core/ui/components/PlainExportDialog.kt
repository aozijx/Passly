package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

enum class PlainExportDialogType {
    DatabaseError,
    NormalExport
}

@Composable
fun PlainExportDialog(
    modifier: Modifier = Modifier,
    type: PlainExportDialogType,
    onExportBackup: () -> Unit,
    onResetOrCancel: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    val title: String
    val message: String
    val confirmText: String
    val dismissText: String
    val dismissColor: Color

    when (type) {
        PlainExportDialogType.DatabaseError -> {
            title = stringResource(R.string.vault_plain_export_db_error_title)
            message = stringResource(R.string.vault_plain_export_db_error_message)
            confirmText = stringResource(R.string.vault_database_retry)
            dismissText = stringResource(R.string.vault_plain_export_db_error_dismiss)
            dismissColor = MaterialTheme.colorScheme.error
        }

        PlainExportDialogType.NormalExport -> {
            title = stringResource(R.string.vault_plain_export_title)
            message = stringResource(R.string.vault_plain_export_message)
            confirmText = stringResource(R.string.vault_plain_export_confirm)
            dismissText = stringResource(R.string.cancel)
            dismissColor = MaterialTheme.colorScheme.primary
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(24.dp),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onExportBackup) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onResetOrCancel) {
                Text(
                    text = dismissText, color = dismissColor
                )
            }
        })
}
