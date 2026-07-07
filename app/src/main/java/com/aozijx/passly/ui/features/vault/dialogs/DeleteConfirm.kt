package com.aozijx.passly.ui.features.vault.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.auth.biometric.BiometricPromptLauncher
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.ui.features.main.MainViewModel

@Composable
fun DeleteConfirmDialog(
    mainViewModel: MainViewModel,
    launcher: BiometricPromptLauncher,
    item: VaultEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val deleteTitle = stringResource(R.string.vault_delete_title)
    val authSubtitle = stringResource(R.string.vault_auth_decrypt_subtitle_generic)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_delete_title)) },
        text = { Text(stringResource(R.string.vault_delete_message, item.title)) },
        confirmButton = {
            TextButton(onClick = {
                mainViewModel.requestAuth(
                    launcher = launcher,
                    title = deleteTitle,
                    subtitle = authSubtitle,
                    onSuccess = { onConfirm() })
            }) {
                Text(
                    stringResource(R.string.vault_delete_title),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        })
}