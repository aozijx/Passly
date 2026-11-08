package com.aozijx.passly.feature.settings.presentation.ui.security

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R

@Composable
fun AppPasswordActionDialog(
    isBusy: Boolean = false,
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onDisablePassword: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(stringResource(R.string.settings_app_password_title)) },
        confirmButton = {
            TextButton(onClick = onChangePassword, enabled = !isBusy) {
                Text(stringResource(R.string.settings_app_password_change_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDisablePassword, enabled = !isBusy) {
                Text(stringResource(R.string.settings_app_password_disable_action))
            }
        },
    )
}
