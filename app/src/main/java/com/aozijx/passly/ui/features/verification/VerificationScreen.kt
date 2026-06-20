package com.aozijx.passly.ui.features.verification

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.ui.features.verification.components.BiometricUnlockButton
import com.aozijx.passly.ui.features.verification.components.PasswordUnlockSection
import com.aozijx.passly.ui.features.verification.components.SetPasswordDialogSection
import com.aozijx.passly.ui.features.verification.components.SetPasswordEntrySection

@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel,
    activity: FragmentActivity,
    preferPasswordFirst: Boolean = true
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appPasswordEnabled by viewModel.isAppPasswordEnabled.collectAsStateWithLifecycle()
    val biometricAvailable = BiometricManager.from(activity)
        .canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    val title = stringResource(R.string.vault_auth_decrypt_title)
    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val passwordMismatchMessage = stringResource(R.string.auth_password_mismatch)
    val emptyPasswordMessage = stringResource(R.string.auth_password_empty)

    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { message ->
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
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
                BiometricUnlockButton(state.authInProgress) {
                    viewModel.verifyWithBiometric(activity, title, subtitle)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            PasswordUnlockSection(
                appPassword = state.appPassword,
                showPasswordInput = state.showPasswordInput,
                authInProgress = state.authInProgress,
                onPasswordChange = viewModel::onPasswordChange,
                onExpandInput = viewModel::onShowPasswordInput,
                onUnlockRequest = viewModel::verifyWithAppPassword
            )

            if (preferPasswordFirst && biometricAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                BiometricUnlockButton(state.authInProgress) {
                    viewModel.verifyWithBiometric(activity, title, subtitle)
                }
            }
        } else {
            if (!biometricAvailable) {
                SetPasswordEntrySection(state.authInProgress) {
                    viewModel.onShowSetPasswordDialog()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                BiometricUnlockButton(state.authInProgress) {
                    viewModel.verifyWithBiometric(activity, title, subtitle)
                }
            }
        }
    }

    if (state.showSetPasswordDialog) {
        SetPasswordDialogSection(
            activity = activity,
            viewModel = viewModel,
            authInProgress = state.authInProgress,
            appPassword = state.appPassword,
            appPasswordConfirm = state.appPasswordConfirm,
            passwordMismatchMessage = passwordMismatchMessage,
            emptyPasswordMessage = emptyPasswordMessage,
            passwordSetSuccessMessage = stringResource(R.string.auth_password_set_success),
            passwordSetFailedMessage = stringResource(R.string.auth_password_set_failed)
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
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}