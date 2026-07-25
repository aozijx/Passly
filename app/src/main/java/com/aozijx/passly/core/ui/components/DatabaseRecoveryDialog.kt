package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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

@Composable
fun DatabaseRecoveryDialog(
    modifier: Modifier = Modifier,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onClearDatabase: () -> Unit,
    onCloseApp: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.database_recovery_clear_confirm_title)) },
            text = { Text(stringResource(R.string.database_recovery_clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        showClearConfirmation = false
                        onClearDatabase()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.database_recovery_clear_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(24.dp),
        title = { Text(stringResource(R.string.vault_plain_export_db_error_title)) },
        text = { Text(stringResource(R.string.vault_plain_export_db_error_message)) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCloseApp, enabled = !isBusy) {
                        Text(stringResource(R.string.vault_plain_export_db_error_dismiss))
                    }
                    TextButton(onClick = onRetry, enabled = !isBusy) {
                        Text(stringResource(R.string.vault_database_retry))
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = { showClearConfirmation = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.database_recovery_clear_action),
                        modifier = Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {}
    )
}
