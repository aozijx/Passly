package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.fragment.app.FragmentActivity
import com.example.poop.R
import com.example.poop.ui.screens.vault.VaultViewModel

@Composable
fun BackupPasswordDialog(activity: FragmentActivity, viewModel: VaultViewModel) {
    val context = LocalContext.current
    val authTitle = stringResource(R.string.vault_backup_auth_title)
    val authSubtitleExport = stringResource(R.string.vault_backup_auth_subtitle_export)
    val authSubtitleImport = stringResource(R.string.vault_backup_auth_subtitle_import)
    val confirmText = stringResource(R.string.action_confirm)
    val cancelText = stringResource(R.string.action_cancel)
    val passwordLabel = stringResource(R.string.label_password)

    AlertDialog(
        onDismissRequest = { viewModel.dismissBackupPasswordDialog() },
        title = {
            Text(
                if (viewModel.isExporting) stringResource(R.string.vault_backup_title_export)
                else stringResource(R.string.vault_backup_title_import)
            )
        },
        text = {
            Column {
                Text(
                    text = if (viewModel.isExporting) stringResource(R.string.vault_backup_message_export)
                    else stringResource(R.string.vault_backup_message_import)
                )
                TextField(
                    value = viewModel.backupPassword,
                    onValueChange = { viewModel.backupPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text(passwordLabel) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (viewModel.backupPassword.isNotEmpty()) {
                        val authSubtitle = if (viewModel.isExporting) authSubtitleExport else authSubtitleImport
                        viewModel.authenticate(
                            activity = activity,
                            title = authTitle,
                            subtitle = authSubtitle
                        ) {
                            viewModel.processBackupAction(context)
                        }
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissBackupPasswordDialog() }) {
                Text(cancelText)
            }
        }
    )
}
