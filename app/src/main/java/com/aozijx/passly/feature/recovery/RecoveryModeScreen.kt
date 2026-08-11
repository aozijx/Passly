package com.aozijx.passly.feature.recovery

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.apppassword.AppPasswordSetDialog
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.settingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.recovery.contract.RecoveryModeEffect
import com.aozijx.passly.feature.recovery.contract.RecoveryModeIntent

/** Restricted UI shown after recovery-code verification. No Vault content is mounted here. */
@Composable
fun RecoveryModeScreen(
    viewModel: RecoveryModeViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RecoveryModeEffect.ExitRecovery -> onExit()
                is RecoveryModeEffect.PasswordResetCompleted ->
                    Toast.makeText(
                        context,
                        R.string.recovery_mode_password_reset_success,
                        Toast.LENGTH_LONG
                    ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recovery_mode_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.recovery_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle(text = stringResource(R.string.settings_security_auth_section))
            RoundedGroup(
                items = listOf(
                    navigationSettingsGroupItem(
                        key = "recovery.set_app_password",
                        icon = Icons.Default.LockReset,
                        title = stringResource(R.string.recovery_mode_set_password),
                        subtitle = stringResource(R.string.settings_security_app_password_description),
                        isLoading = state.isSettingPassword,
                        onClick = { viewModel.onIntent(RecoveryModeIntent.SetPasswordClicked) }
                    )
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            RoundedGroup(
                items = listOf(
                    settingsGroupItem(
                        key = "recovery.exit",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.recovery_mode_exit),
                        subtitle = stringResource(R.string.notice_app_locked),
                        onClick = { viewModel.onIntent(RecoveryModeIntent.ExitClicked) }
                    )
                ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }

    if (state.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = state.newPassword,
            confirmPassword = state.confirmPassword,
            onNewPasswordChange = {
                viewModel.onIntent(RecoveryModeIntent.NewPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                viewModel.onIntent(RecoveryModeIntent.ConfirmPasswordChanged(it))
            },
            onConfirm = { viewModel.onIntent(RecoveryModeIntent.SubmitNewPassword) },
            onDismiss = { viewModel.onIntent(RecoveryModeIntent.DismissPasswordDialog) },
            isBusy = state.isSettingPassword,
            errorMessage = state.passwordSetupError
        )
    }

}
