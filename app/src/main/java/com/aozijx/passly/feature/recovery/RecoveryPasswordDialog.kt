package com.aozijx.passly.feature.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.aozijx.passly.core.ui.components.dialog.PasslyAlertDialog
import com.aozijx.passly.core.ui.components.settings.PasswordStrengthIndicator
import com.aozijx.passly.R
import com.aozijx.passly.domain.access.policy.AppPasswordPolicy
import com.aozijx.passly.domain.entry.model.PasswordStrengthLevel
import com.aozijx.passly.domain.entry.policy.PasswordStrengthEvaluator

@Composable
internal fun RecoveryPasswordDialog(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isBusy: Boolean,
    errorMessage: String?,
) {
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val passwordStrength = remember(newPassword) {
        PasswordStrengthEvaluator.evaluate(newPassword)
    }
    val strengthColor = when (passwordStrength.level) {
        PasswordStrengthLevel.VERY_WEAK -> MaterialTheme.colorScheme.error
        PasswordStrengthLevel.WEAK -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        PasswordStrengthLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        PasswordStrengthLevel.GOOD -> MaterialTheme.colorScheme.secondary
        PasswordStrengthLevel.STRONG -> MaterialTheme.colorScheme.primary
    }
    val strengthText = stringResource(
        when (passwordStrength.level) {
            PasswordStrengthLevel.VERY_WEAK -> R.string.recovery_mode_password_strength_very_weak
            PasswordStrengthLevel.WEAK -> R.string.recovery_mode_password_strength_weak
            PasswordStrengthLevel.MEDIUM -> R.string.recovery_mode_password_strength_medium
            PasswordStrengthLevel.GOOD -> R.string.recovery_mode_password_strength_good
            PasswordStrengthLevel.STRONG -> R.string.recovery_mode_password_strength_strong
        }
    )

    PasslyAlertDialog(
        title = stringResource(R.string.recovery_mode_password_dialog_title),
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = { if (!isBusy) onDismiss() },
        confirmEnabled = !isBusy &&
                AppPasswordPolicy.DEFAULT.acceptsLength(newPassword.length) &&
                newPassword == confirmPassword,
    ) {
        Column {
            Text(
                text = stringResource(R.string.recovery_mode_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.recovery_mode_new_password)) },
                visualTransformation = if (newPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
                trailingIcon = {
                    PasswordVisibilityButton(
                        visible = newPasswordVisible,
                        onClick = { newPasswordVisible = !newPasswordVisible },
                    )
                },
            )
            if (newPassword.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthIndicator(
                    progress = passwordStrength.score / 100f,
                    color = strengthColor,
                    text = strengthText,
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.recovery_mode_confirm_password)) },
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
                trailingIcon = {
                    PasswordVisibilityButton(
                        visible = confirmPasswordVisible,
                        onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    )
                },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { message ->
                    { Text(message) }
                },
            )
        }
    }
}

@Composable
private fun PasswordVisibilityButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = stringResource(
                if (visible) {
                    R.string.recovery_mode_hide_password
                } else {
                    R.string.recovery_mode_show_password
                }
            ),
        )
    }
}
