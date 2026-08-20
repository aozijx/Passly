package com.aozijx.passly.presentation.vault.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.query.EntryListItem

@Composable
fun DeleteConfirmDialog(
    item: EntryListItem,
    requestAuthentication: (onSuccess: () -> Unit) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_delete_title)) },
        text = { Text(stringResource(R.string.vault_delete_message, item.title)) },
        confirmButton = {
            TextButton(onClick = {
                requestAuthentication(onConfirm)
            }) {
                Text(
                    stringResource(R.string.vault_auth_decrypt_subtitle_generic),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
