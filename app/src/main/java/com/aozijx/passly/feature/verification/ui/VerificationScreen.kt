package com.aozijx.passly.feature.verification.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.domain.authentication.AuthenticationState
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordSetDialog
import com.aozijx.passly.feature.verification.VerificationViewModel
import com.aozijx.passly.feature.verification.ui.components.BiometricUnlockButton
import com.aozijx.passly.feature.verification.ui.components.SetPasswordEntrySection

@Composable
fun VerificationScreen(
    viewModel: VerificationViewModel
) {
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val methods by viewModel.methodAvailability.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val isAuthInProgress = authState !is AuthenticationState.Locked

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 居中图标和标题
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 底部操作区
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (methods.biometric) {
                BiometricUnlockButton(
                    authInProgress = isAuthInProgress,
                    onClick = viewModel::verifyWithBiometric
                )
            }

            if (methods.appPassword) {
                if (methods.biometric) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                PasswordUnlockButton(
                    authInProgress = isAuthInProgress,
                    onClick = viewModel::verifyWithAppPassword
                )
            }

            if (!methods.biometric && !methods.appPassword) {
                SetPasswordEntrySection(
                    authInProgress = isAuthInProgress,
                    onClick = viewModel::onShowSetPasswordDialog
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (methods.recoveryCode) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = viewModel::unlockWithRecoveryCode,
                    enabled = !isAuthInProgress
                ) {
                    Text(
                        text = stringResource(R.string.auth_use_recovery_code),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (uiState.showSetPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        AppPasswordSetDialog(
            newPassword = password,
            confirmPassword = confirmPassword,
            onNewPasswordChange = { password = it },
            onConfirmPasswordChange = { confirmPassword = it },
            onConfirm = {
                if (password != confirmPassword || password.isEmpty()) return@AppPasswordSetDialog
                viewModel.bootstrapAppPassword(password.toCharArray())
                password = ""
                confirmPassword = ""
            },
            onDismiss = {
                password = ""
                confirmPassword = ""
                viewModel.onDismissSetPasswordDialog()
            }
        )
    }
}

@Composable
private fun PasswordUnlockButton(
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
        AnimatedContent(
            targetState = authInProgress,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "password_loading"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.auth_password_unlock),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
