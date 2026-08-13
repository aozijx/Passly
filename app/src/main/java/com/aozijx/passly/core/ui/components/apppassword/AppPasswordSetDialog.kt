package com.aozijx.passly.core.ui.components.apppassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy

@Composable
fun AppPasswordSetDialog(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean = false,
    errorMessage: String? = null
) {
    AlertDialog(
        onDismissRequest = {
            if (!isBusy) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(stringResource(R.string.auth_set_app_password))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_auth_password_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(16.dp))
                PasswordFields(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword,
                    onNewPasswordChange = onNewPasswordChange,
                    onConfirmPasswordChange = onConfirmPasswordChange
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isBusy &&
                        AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length) &&
                        newPassword == confirmPassword
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
