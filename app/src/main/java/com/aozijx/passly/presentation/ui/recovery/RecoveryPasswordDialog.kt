package com.aozijx.passly.presentation.ui.recovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.dialog.PasslyAlertDialog
import com.aozijx.passly.core.ui.components.settings.PasswordStrengthIndicator

data class RecoveryPasswordStrengthUiModel(val score: Int, val labelRes: Int, val tone: RecoveryPasswordStrengthTone)
enum class RecoveryPasswordStrengthTone { ERROR, MUTED_ERROR, TERTIARY, SECONDARY, PRIMARY }

@Composable
fun RecoveryPasswordDialog(newPassword: String, confirmPassword: String, strength: RecoveryPasswordStrengthUiModel, confirmEnabled: Boolean, onNewPasswordChange: (String) -> Unit, onConfirmPasswordChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit, isBusy: Boolean, errorMessage: String?) {
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    PasslyAlertDialog(
        title = stringResource(R.string.recovery_mode_password_dialog_title),
        confirmText = stringResource(R.string.save),
        dismissText = stringResource(R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = { if (!isBusy) onDismiss() },
        confirmEnabled = !isBusy && confirmEnabled,
    ) {
        Column {
            Text(stringResource(R.string.recovery_mode_password_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            PasswordField(newPassword, onNewPasswordChange, !isBusy, stringResource(R.string.recovery_mode_new_password), newVisible) { newVisible = !newVisible }
            if (newPassword.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PasswordStrengthIndicator(strength.score / 100f, strength.color(), stringResource(strength.labelRes))
            }
            Spacer(Modifier.height(12.dp))
            PasswordField(confirmPassword, onConfirmPasswordChange, !isBusy, stringResource(R.string.recovery_mode_confirm_password), confirmVisible, ImeAction.Done, errorMessage) { confirmVisible = !confirmVisible }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, enabled: Boolean, label: String, visible: Boolean, ime: ImeAction = ImeAction.Next, error: String? = null, toggle: () -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), enabled, label = { Text(label) }, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ime), singleLine = true, trailingIcon = { IconButton(toggle) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, stringResource(if (visible) R.string.recovery_mode_hide_password else R.string.recovery_mode_show_password)) } }, isError = error != null, supportingText = error?.let { message -> { Text(message) } })
}

@Composable
private fun RecoveryPasswordStrengthUiModel.color(): Color = when (tone) {
    RecoveryPasswordStrengthTone.ERROR -> MaterialTheme.colorScheme.error
    RecoveryPasswordStrengthTone.MUTED_ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    RecoveryPasswordStrengthTone.TERTIARY -> MaterialTheme.colorScheme.tertiary
    RecoveryPasswordStrengthTone.SECONDARY -> MaterialTheme.colorScheme.secondary
    RecoveryPasswordStrengthTone.PRIMARY -> MaterialTheme.colorScheme.primary
}
