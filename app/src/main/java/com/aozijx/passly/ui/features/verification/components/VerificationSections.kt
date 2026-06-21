package com.aozijx.passly.ui.features.verification.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
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
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.memory.SecureString

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