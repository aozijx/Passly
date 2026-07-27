package com.aozijx.passly.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
    isBusy: Boolean,
    onRetry: () -> Unit,
    onRecoverDatabase: () -> Unit,
    onCloseApp: () -> Unit,
    onDismissRequest: () -> Unit = {}
) {
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
                    onClick = onRecoverDatabase,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.database_recovery_create_new_action),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        dismissButton = {}
    )
}
