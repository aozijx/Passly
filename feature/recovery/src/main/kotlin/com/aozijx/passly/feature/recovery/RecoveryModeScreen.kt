package com.aozijx.passly.feature.recovery

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.sensitive.SensitiveValue
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
            SettingsSectionTitle(text = stringResource(R.string.recovery_mode_auth_section))
            RecoveryActionCard(
                icon = Icons.Default.LockReset,
                title = stringResource(R.string.recovery_mode_set_password),
                subtitle = stringResource(R.string.recovery_mode_password_description),
                isLoading = state.isSettingPassword,
                onClick = { viewModel.onIntent(RecoveryModeIntent.SetPasswordClicked) },
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            RecoveryActionCard(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.recovery_mode_exit),
                subtitle = stringResource(R.string.recovery_mode_locked_description),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                onClick = { viewModel.onIntent(RecoveryModeIntent.ExitClicked) },
            )
        }
    }

    if (state.showSetPasswordDialog) {
        RecoveryPasswordDialog(
            newPassword = state.newPassword.toUiString(),
            confirmPassword = state.confirmPassword.toUiString(),
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

@Composable
private fun RecoveryActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        chars.fill('\u0000')
    }
}
