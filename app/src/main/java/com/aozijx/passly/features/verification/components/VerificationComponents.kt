package com.aozijx.passly.features.verification.components

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aozijx.passly.R
import com.aozijx.passly.features.common.toUiMessage
import com.aozijx.passly.features.verification.AppPasswordSetDialog
import com.aozijx.passly.features.verification.VerificationGateway

@Composable
internal fun BiometricUnlockButton(
    authInProgress: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !authInProgress,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (authInProgress) "..." else stringResource(R.string.auth_verify_now),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun PasswordUnlockSection(
    appPassword: String,
    showPasswordInput: Boolean,
    authInProgress: Boolean,
    onPasswordChange: (String) -> Unit,
    onExpandInput: () -> Unit,
    onUnlockRequest: () -> Unit
) {
    if (showPasswordInput) {
        OutlinedTextField(
            value = appPassword,
            onValueChange = onPasswordChange,
            singleLine = true,
            label = { Text(stringResource(R.string.auth_app_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (appPassword.isNotBlank() && !authInProgress) onUnlockRequest() }
            ),
            enabled = !authInProgress,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    FilledTonalButton(
        onClick = {
            if (!showPasswordInput) {
                onExpandInput()
                return@FilledTonalButton
            }
            onUnlockRequest()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = if (showPasswordInput) appPassword.isNotBlank() && !authInProgress else !authInProgress,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = if (authInProgress) "..." else stringResource(R.string.auth_unlock_with_app_password),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun SetPasswordEntrySection(
    authInProgress: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !authInProgress,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = stringResource(R.string.auth_set_app_password),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun SetPasswordDialogSection(
    activity: FragmentActivity,
    gateway: VerificationGateway,
    authInProgress: Boolean,
    appPassword: String,
    appPasswordConfirm: String,
    passwordMismatchMessage: String,
    emptyPasswordMessage: String,
    passwordSetSuccessMessage: String,
    passwordSetFailedMessage: String,
    onAuthInProgressChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onDialogVisibilityChange: (Boolean) -> Unit,
    onPasswordInputVisibilityChange: (Boolean) -> Unit
) {
    AppPasswordSetDialog(
        newPassword = appPassword,
        confirmPassword = appPasswordConfirm,
        onNewPasswordChange = onPasswordChange,
        onConfirmPasswordChange = onPasswordConfirmChange,
        onConfirm = {
            if (appPassword != appPasswordConfirm) {
                Toast.makeText(activity, passwordMismatchMessage, Toast.LENGTH_SHORT).show()
                return@AppPasswordSetDialog
            }
            if (appPassword.isBlank()) {
                Toast.makeText(activity, emptyPasswordMessage, Toast.LENGTH_SHORT).show()
                return@AppPasswordSetDialog
            }
            if (authInProgress) return@AppPasswordSetDialog

            onAuthInProgressChange(true)
            val password = appPassword.toCharArray()
            gateway.bootstrapAppPassword(password) { result ->
                password.fill('\u0000')
                onAuthInProgressChange(false)
                result.onSuccess {
                    onPasswordChange("")
                    onPasswordConfirmChange("")
                    onDialogVisibilityChange(false)
                    onPasswordInputVisibilityChange(false)
                    Toast.makeText(activity, passwordSetSuccessMessage, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(
                        activity,
                        error.toUiMessage(passwordSetFailedMessage),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onDismiss = {
            if (!authInProgress) {
                onDialogVisibilityChange(false)
                onPasswordChange("")
                onPasswordConfirmChange("")
            }
        }
    )
}