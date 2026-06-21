package com.aozijx.passly.ui.features.verification.components

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import com.aozijx.passly.core.crypto.memory.SecureString
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordSetDialog
import com.aozijx.passly.ui.features.verification.VerificationViewModel

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
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = if (authInProgress) "..." else stringResource(R.string.auth_verify_now),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun PasswordUnlockSection(
    appPassword: SecureString,
    showPasswordInput: Boolean,
    authInProgress: Boolean,
    onPasswordChange: (String) -> Unit,
    onExpandInput: () -> Unit,
    onUnlockRequest: () -> Unit
) {
    if (showPasswordInput) {
        OutlinedTextField(
            value = appPassword.toPlainString(),
            onValueChange = onPasswordChange,
            singleLine = true,
            label = { Text(stringResource(R.string.auth_app_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (!appPassword.isEmpty && !authInProgress) onUnlockRequest() }
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
        enabled = if (showPasswordInput) !appPassword.isEmpty && !authInProgress else !authInProgress,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
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
        Icon(
            imageVector = Icons.Default.Password,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.auth_set_app_password),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
internal fun SetPasswordDialogSection(
    activity: FragmentActivity,
    viewModel: VerificationViewModel,
    appPassword: SecureString,
    appPasswordConfirm: SecureString,
    passwordMismatchMessage: String,
    emptyPasswordMessage: String,
    passwordSetSuccessMessage: String,
    passwordSetFailedMessage: String
) {
    AppPasswordSetDialog(
        newPassword = appPassword.toPlainString(),
        confirmPassword = appPasswordConfirm.toPlainString(),
        onNewPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onPasswordConfirmChange,
        onConfirm = {
            viewModel.bootstrapAppPassword(
                validation = {
                    if (appPassword.toPlainString() != appPasswordConfirm.toPlainString()) {
                        Toast.makeText(activity, passwordMismatchMessage, Toast.LENGTH_SHORT).show()
                        return@bootstrapAppPassword false
                    }
                    if (appPassword.isEmpty) {
                        Toast.makeText(activity, emptyPasswordMessage, Toast.LENGTH_SHORT).show()
                        return@bootstrapAppPassword false
                    }
                    true
                },
                onComplete = { success ->
                    if (success) {
                        Toast.makeText(activity, passwordSetSuccessMessage, Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(activity, passwordSetFailedMessage, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )
        },
        onDismiss = viewModel::onDismissSetPasswordDialog
    )
}