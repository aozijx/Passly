package com.aozijx.passly.feature.settings.apppassword.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R

@Composable
fun AppPasswordActionDialog(
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onDisablePassword: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_password_manage_title)) },
        text = { Text(stringResource(R.string.app_password_manage_description)) },
        confirmButton = {
            TextButton(onClick = onChangePassword) {
                Text(stringResource(R.string.app_password_change_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDisablePassword) {
                Text(stringResource(R.string.app_password_disable_action))
            }
        }
    )
}
