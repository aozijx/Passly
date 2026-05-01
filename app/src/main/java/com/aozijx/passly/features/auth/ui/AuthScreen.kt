package com.aozijx.passly.features.auth.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.features.auth.AuthCoordinator
import com.aozijx.passly.features.settings.components.dialogs.AppPasswordSetDialog

/**
 * 授权验证屏幕：系统自动选择可用生物识别方式，提供重试入口。
 */
@Composable
fun AuthScreen(
    authCoordinator: AuthCoordinator,
    activity: FragmentActivity,
    preferPasswordFirst: Boolean = true
) {
    val appPasswordEnabled by authCoordinator.isAppPasswordEnabled.collectAsStateWithLifecycle()
    val biometricAvailable =
        BiometricManager.from(activity)
            .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    var authInProgress by remember { mutableStateOf(false) }
    var appPassword by remember { mutableStateOf("") }
    var appPasswordConfirm by remember { mutableStateOf("") }
    var showPasswordInput by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    val title = stringResource(R.string.vault_auth_decrypt_title)
    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val passwordMismatchMessage = stringResource(R.string.auth_password_mismatch)
    val emptyPasswordMessage = stringResource(R.string.auth_password_empty)
    val passwordSetSuccessMessage = stringResource(R.string.auth_password_set_success)
    val passwordSetFailedMessage = stringResource(R.string.auth_password_set_failed)

    fun requestBiometricAuth() {
        if (authInProgress) return
        authInProgress = true
        authCoordinator.authenticate(
            activity = activity,
            title = title,
            subtitle = subtitle,
            onSuccess = { authInProgress = false },
            onError = { authInProgress = false })
    }

    fun requestPasswordAuth() {
        if (authInProgress) return
        authInProgress = true
        authCoordinator.authenticateWithAppPassword(
            password = appPassword.toCharArray(),
            onSuccess = {
                authInProgress = false
                appPassword = ""
                showPasswordInput = false
            },
            onError = { authInProgress = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 顶部：安全状态标识
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = CircleShape
                ), contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.vault_locked_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(64.dp))

        if (appPasswordEnabled) {
            if (!preferPasswordFirst && biometricAvailable) {
                BiometricUnlockButton(
                    authInProgress = authInProgress,
                    onClick = { requestBiometricAuth() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            PasswordUnlockSection(
                appPassword = appPassword,
                showPasswordInput = showPasswordInput,
                authInProgress = authInProgress,
                onPasswordChange = { appPassword = it },
                onExpandInput = { showPasswordInput = true },
                onUnlockRequest = { requestPasswordAuth() }
            )

            if (preferPasswordFirst && biometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                BiometricUnlockButton(
                    authInProgress = authInProgress,
                    onClick = { requestBiometricAuth() }
                )
            }
        } else {
            if (!biometricAvailable) {
                SetPasswordEntrySection(
                    authInProgress = authInProgress,
                    onClick = { showSetPasswordDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))

                BiometricUnlockButton(
                    authInProgress = authInProgress,
                    onClick = { requestBiometricAuth() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.auth_enable_password_in_settings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSetPasswordDialog) {
        SetPasswordDialogSection(
            activity = activity,
            authCoordinator = authCoordinator,
            authInProgress = authInProgress,
            appPassword = appPassword,
            appPasswordConfirm = appPasswordConfirm,
            passwordMismatchMessage = passwordMismatchMessage,
            emptyPasswordMessage = emptyPasswordMessage,
            passwordSetSuccessMessage = passwordSetSuccessMessage,
            passwordSetFailedMessage = passwordSetFailedMessage,
            onAuthInProgressChange = { authInProgress = it },
            onPasswordChange = { appPassword = it },
            onPasswordConfirmChange = { appPasswordConfirm = it },
            onDialogVisibilityChange = { showSetPasswordDialog = it },
            onPasswordInputVisibilityChange = { showPasswordInput = it }
        )
    }
}

@Composable
private fun SetPasswordEntrySection(
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
private fun SetPasswordDialogSection(
    activity: FragmentActivity,
    authCoordinator: AuthCoordinator,
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
            authCoordinator.bootstrapAppPassword(password) { result ->
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
                        error.message ?: passwordSetFailedMessage,
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

@Composable
private fun BiometricUnlockButton(
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
            text = if (authInProgress) "验证中..." else stringResource(R.string.auth_verify_now),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun PasswordUnlockSection(
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
            text = if (authInProgress) "验证中..." else stringResource(R.string.auth_unlock_with_app_password),
            style = MaterialTheme.typography.titleMedium
        )
    }
}