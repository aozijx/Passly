package com.aozijx.passly.core.ui.components.apppassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.settings.PasswordStrengthIndicator
import com.aozijx.passly.core.ui.components.settings.rememberPasswordStrength

@Composable
fun PasswordFields(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit
) {
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val strengthUiState = rememberPasswordStrength(newPassword)

    OutlinedTextField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        singleLine = true,
        label = { Text(stringResource(R.string.auth_app_password_label)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                Icon(
                    imageVector = if (newPasswordVisible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = stringResource(
                        if (newPasswordVisible) R.string.hide_password
                        else R.string.show_password
                    )
                )
            }
        },
        visualTransformation = if (newPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    )

    if (newPassword.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        PasswordStrengthIndicator(
            progress = strengthUiState.progress,
            color = strengthUiState.color,
            text = stringResource(strengthUiState.textResId),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        singleLine = true,
        label = { Text(stringResource(R.string.settings_auth_app_password_confirm_label)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                Icon(
                    imageVector = if (confirmPasswordVisible) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    contentDescription = stringResource(
                        if (confirmPasswordVisible) R.string.hide_password
                        else R.string.show_password
                    )
                )
            }
        },
        visualTransformation = if (confirmPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
        supportingText = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            { Text(stringResource(R.string.settings_auth_password_mismatch)) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    )
}