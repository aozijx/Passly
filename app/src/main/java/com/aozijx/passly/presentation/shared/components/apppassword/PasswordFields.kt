package com.aozijx.passly.presentation.shared.components.apppassword

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.core.ui.components.settings.PasswordStrengthIndicator
import com.aozijx.passly.presentation.mapping.PasswordConfirmationUiState
import com.aozijx.passly.presentation.mapping.PasswordStrengthUiState

@Composable
fun PasswordFields(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDone: () -> Unit,
    strengthUiState: PasswordStrengthUiState,
    confirmationState: PasswordConfirmationUiState,
    enabled: Boolean = true,
) {
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = newPassword,
        onValueChange = onNewPasswordChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.authentication_app_password_label)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.LockOpen,
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }, enabled = enabled) {
                Icon(
                    imageVector =
                        if (newPasswordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                    contentDescription =
                        stringResource(
                            if (newPasswordVisible) {
                                R.string.hide_password
                            } else {
                                R.string.show_password
                            },
                        ),
                )
            }
        },
        visualTransformation =
            if (newPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Next) },
            ),
        modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
        shape = MaterialTheme.shapes.large,
    )

    when (val strength = strengthUiState) {
        PasswordStrengthUiState.Hidden -> {
            Unit
        }

        is PasswordStrengthUiState.Visible -> {
            Spacer(modifier = Modifier.height(4.dp))
            PasswordStrengthIndicator(
                progress = strength.progress,
                color = strength.color,
                text = stringResource(strength.textResId),
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        enabled = enabled,
        singleLine = true,
        label = { Text(stringResource(R.string.authentication_app_password_confirm_label)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
            )
        },
        trailingIcon = {
            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }, enabled = enabled) {
                Icon(
                    imageVector =
                        if (confirmPasswordVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                    contentDescription =
                        stringResource(
                            if (confirmPasswordVisible) {
                                R.string.hide_password
                            } else {
                                R.string.show_password
                            },
                        ),
                )
            }
        },
        visualTransformation =
            if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        isError = confirmationState == PasswordConfirmationUiState.Mismatch,
        supportingText =
            if (confirmationState == PasswordConfirmationUiState.Mismatch) {
                { Text(stringResource(R.string.authentication_app_password_mismatch)) }
            } else {
                null
            },
        modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
        shape = MaterialTheme.shapes.large,
    )
}
