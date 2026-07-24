package com.aozijx.passly.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.common.ActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.domain.authentication.AuthenticationMethod
import com.aozijx.passly.feature.auth.presentation.AuthenticationViewModel
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordSetDialog

@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel
) {
    val methods by viewModel.methodAvailability.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subtitle = stringResource(R.string.vault_auth_subtitle)
    val activeMethod = uiState.activeMethod

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
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

        Column(
            modifier = Modifier
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
                ActionButton(
                    icon = Icons.Default.Fingerprint,
                    text = stringResource(R.string.auth_biometric_unlock),
                    progress = activeMethod == AuthenticationMethod.BIOMETRIC,
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.BIOMETRIC,
                    onClick = viewModel::verifyWithBiometric
                )
            }

            if (methods.appPassword) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    value = uiState.appPassword,
                    expanded = uiState.expandedMethod == AuthenticationMethod.APP_PASSWORD,
                    progress = activeMethod == AuthenticationMethod.APP_PASSWORD,
                    collapsedText = stringResource(R.string.auth_password_unlock),
                    expandedText = stringResource(R.string.auth_password_verify),
                    inputLabel = stringResource(R.string.auth_app_password_label),
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.APP_PASSWORD,
                    onValueChange = viewModel::onAppPasswordChange,
                    onExpandedChange = {
                        viewModel.onInputExpanded(AuthenticationMethod.APP_PASSWORD, it)
                    },
                    onAction = viewModel::verifyWithAppPassword
                )
            }

            if (!methods.biometric && !methods.appPassword) {
                ActionButton(
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.auth_set_app_password),
                    progress = uiState.isSettingAppPassword,
                    enabled = activeMethod == null && !uiState.isSettingAppPassword,
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
                InputActionButton(
                    value = uiState.recoveryCode,
                    expanded = uiState.expandedMethod == AuthenticationMethod.RECOVERY_CODE,
                    progress = activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    icon = Icons.Default.Restore,
                    collapsedText = stringResource(R.string.auth_use_recovery_code),
                    expandedText = stringResource(R.string.auth_recovery_code_unlock),
                    inputLabel = stringResource(R.string.auth_recovery_code_label),
                    enabled = activeMethod == null || activeMethod == AuthenticationMethod.RECOVERY_CODE,
                    onValueChange = viewModel::onRecoveryCodeChange,
                    onExpandedChange = {
                        viewModel.onInputExpanded(AuthenticationMethod.RECOVERY_CODE, it)
                    },
                    onAction = viewModel::unlockWithRecoveryCode
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (uiState.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = uiState.newAppPassword.toPlainString(),
            confirmPassword = uiState.confirmAppPassword.toPlainString(),
            onNewPasswordChange = viewModel::onNewAppPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmAppPasswordChange,
            onConfirm = viewModel::bootstrapAppPassword,
            onDismiss = viewModel::onDismissSetPasswordDialog
        )
    }
}
