package com.example.poop.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.data.VaultEntry

@Composable
fun DeleteConfirmDialog(
    activity: FragmentActivity,
    item: VaultEntry,
    viewModel: MainViewModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_delete_title)) },
        text = { Text(stringResource(R.string.vault_delete_message, item.title)) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.authenticate(
                    activity = activity,
                    title = activity.getString(R.string.vault_delete_title),
                    subtitle = activity.getString(R.string.vault_auth_decrypt_subtitle_generic)
                ) {
                    onConfirm()
                }
            }) {
                Text(stringResource(R.string.vault_delete_title), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
