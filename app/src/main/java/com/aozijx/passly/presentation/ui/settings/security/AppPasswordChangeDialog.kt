package com.aozijx.passly.presentation.ui.settings.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.shared.components.apppassword.PasswordFields

data class AppPasswordChangeDialogState(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String,
    val confirmEnabled: Boolean,
)

interface AppPasswordChangeDialogEventHandler {
    fun onCurrentPasswordChanged(password: String)
    fun onNewPasswordChanged(password: String)
    fun onConfirmPasswordChanged(password: String)
    fun onConfirm()
    fun onDismiss()
}

@Composable
fun AppPasswordChangeDialog(
    state: AppPasswordChangeDialogState,
    eventHandler: AppPasswordChangeDialogEventHandler,
) {
    AlertDialog(
        onDismissRequest = eventHandler::onDismiss,
        title = { Text(stringResource(R.string.settings_auth_change_app_password)) },
        text = {
            Column {
                OutlinedTextField(
                    value = state.currentPassword,
                    onValueChange = eventHandler::onCurrentPasswordChanged,
                    label = { Text(stringResource(R.string.settings_auth_current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordFields(
                    newPassword = state.newPassword,
                    confirmPassword = state.confirmPassword,
                    onNewPasswordChange = eventHandler::onNewPasswordChanged,
                    onConfirmPasswordChange = eventHandler::onConfirmPasswordChanged,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = eventHandler::onConfirm,
                enabled = state.confirmEnabled,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = eventHandler::onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
