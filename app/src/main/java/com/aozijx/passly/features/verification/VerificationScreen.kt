package com.aozijx.passly.features.verification

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.features.common.toUiMessage
import com.aozijx.passly.features.verification.components.BiometricUnlockButton
import com.aozijx.passly.features.verification.components.PasswordUnlockSection
import com.aozijx.passly.features.verification.components.SetPasswordDialogSection
import com.aozijx.passly.features.verification.components.SetPasswordEntrySection

@Composable
fun VerificationScreen(
    gateway: VerificationGateway,
    activity: FragmentActivity,
    preferPasswordFirst: Boolean = true
) {
    val appPasswordEnabled by gateway.isAppPasswordEnabled.collectAsStateWithLifecycle()
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
        gateway.verifyWithBiometric(activity, title, subtitle) { result ->
            authInProgress = false
            if (result is AppResult.Failure) {
                Toast.makeText(activity, result.error.toUiMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requestPasswordAuth() {
        if (authInProgress) return
        authInProgress = true
        gateway.verifyWithAppPassword(appPassword.toCharArray()) { result ->
            authInProgress = false
            when (result) {
                is AppResult.Success -> {
                    appPassword = ""
                    showPasswordInput = false
                }

                is AppResult.Failure -> {
                    Toast.makeText(activity, result.error.toUiMessage(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LockIcon()

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
                BiometricUnlockButton(authInProgress) { requestBiometricAuth() }
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
                BiometricUnlockButton(authInProgress) { requestBiometricAuth() }
            }
        } else {
            if (!biometricAvailable) {
                SetPasswordEntrySection(authInProgress) { showSetPasswordDialog = true }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                BiometricUnlockButton(authInProgress) { requestBiometricAuth() }
            }
        }
    }

    if (showSetPasswordDialog) {
        SetPasswordDialogSection(
            activity = activity,
            gateway = gateway,
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
private fun LockIcon() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}