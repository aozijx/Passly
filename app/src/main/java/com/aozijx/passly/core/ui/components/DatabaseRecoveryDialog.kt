package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
fun DatabaseRecoveryDialog(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onCloseApp: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(24.dp),
        title = { Text(stringResource(R.string.vault_plain_export_db_error_title)) },
        text = { Text(stringResource(R.string.vault_plain_export_db_error_message)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.vault_database_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCloseApp) {
                Text(
                    text = stringResource(R.string.vault_plain_export_db_error_dismiss),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
