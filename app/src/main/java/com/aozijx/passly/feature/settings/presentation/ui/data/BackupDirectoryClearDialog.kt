package com.aozijx.passly.feature.settings.presentation.ui.data

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
internal fun BackupDirectoryClearDialog(
    isClearing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        title = { Text(stringResource(R.string.settings_backup_clear_directory_title)) },
        text = { Text(stringResource(R.string.settings_backup_clear_directory_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isClearing) {
                if (isClearing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(stringResource(R.string.settings_backup_clear_selection))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isClearing) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
