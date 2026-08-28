package com.aozijx.passly.presentation.ui.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.common.ActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButtonConfig
import com.aozijx.passly.core.ui.components.common.InputActionButtonState

enum class AuthenticationInputMethod {
    BIOMETRIC,
    APP_PASSWORD,
    RECOVERY_CODE,
}

@Immutable
class AuthenticationContentState(
    val biometricAvailable: Boolean,
    val appPasswordAvailable: Boolean,
    val recoveryCodeAvailable: Boolean,
    val recoveryUnlockVisible: Boolean,
    val activeMethod: AuthenticationInputMethod?,
    val expandedMethod: AuthenticationInputMethod?,
    val appPassword: String,
    val recoveryCode: String,
    val biometricFailureMessage: String?,
    val appPasswordFailureMessage: String?,
    val recoveryCodeFailureMessage: String?,
    val isSettingAppPassword: Boolean,
)

data class AuthenticationEventHandler(
    val onLockIconClick: () -> Unit,
    val onBiometricClick: () -> Unit,
    val onAppPasswordChange: (String) -> Unit,
    val onAppPasswordExpandedChange: (Boolean) -> Unit,
    val onAppPasswordSubmit: () -> Unit,
    val onRecoveryCodeChange: (String) -> Unit,
    val onRecoveryCodeExpandedChange: (Boolean) -> Unit,
    val onRecoveryCodeSubmit: () -> Unit,
    val onVerificationFailureConsumed: () -> Unit,
    val onSetPasswordClick: () -> Unit,
)

@Composable
fun AuthenticationContent(
    state: AuthenticationContentState,
    onEvent: AuthenticationEventHandler,
    modifier: Modifier = Modifier,
) {
    val lockIconInteractionSource = remember { MutableInteractionSource() }
    val appPasswordLabel = stringResource(R.string.auth_app_password_label)
    val recoveryCodeLabel = stringResource(R.string.recovery_code_label)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clickable(
                        interactionSource = lockIconInteractionSource,
                        indication = null,
                        onClick = onEvent.onLockIconClick,
                    ),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.vault_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.vault_auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.biometricFailureMessage?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (state.biometricAvailable) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Fingerprint,
                    text = stringResource(R.string.auth_biometric_unlock),
                    progress = state.activeMethod == AuthenticationInputMethod.BIOMETRIC,
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationInputMethod.BIOMETRIC,
                    onClick = onEvent.onBiometricClick,
                )
            }

            if (state.appPasswordAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state = InputActionButtonState(
                        value = state.appPassword,
                        expanded = state.expandedMethod == AuthenticationInputMethod.APP_PASSWORD,
                        progress = state.activeMethod == AuthenticationInputMethod.APP_PASSWORD,
                        result = state.appPasswordFailureMessage?.let { false },
                    ),
                    config = InputActionButtonConfig(
                        collapsedText = stringResource(R.string.auth_password_unlock),
                        expandedText = stringResource(R.string.auth_password_verify),
                        inputLabel = appPasswordLabel,
                        errorText = state.appPasswordFailureMessage
                            ?: stringResource(R.string.auth_error_failed),
                    ),
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationInputMethod.APP_PASSWORD,
                    onValueChange = onEvent.onAppPasswordChange,
                    onExpandedChange = onEvent.onAppPasswordExpandedChange,
                    onAction = onEvent.onAppPasswordSubmit,
                    onResultConsumed = onEvent.onVerificationFailureConsumed,
                )
            }

            if (!state.biometricAvailable && !state.appPasswordAvailable &&
                !state.recoveryCodeAvailable
            ) {
                ActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Password,
                    text = stringResource(R.string.auth_set_app_password),
                    progress = state.isSettingAppPassword,
                    enabled = state.activeMethod == null && !state.isSettingAppPassword,
                    onClick = onEvent.onSetPasswordClick,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auth_biometric_unavailable_password_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.recoveryCodeAvailable && state.recoveryUnlockVisible) {
                Spacer(modifier = Modifier.height(8.dp))
                InputActionButton(
                    state = InputActionButtonState(
                        value = state.recoveryCode,
                        expanded = state.expandedMethod == AuthenticationInputMethod.RECOVERY_CODE,
                        progress = state.activeMethod == AuthenticationInputMethod.RECOVERY_CODE,
                        result = state.recoveryCodeFailureMessage?.let { false },
                    ),
                    config = InputActionButtonConfig(
                        icon = Icons.Default.Restore,
                        collapsedText = stringResource(R.string.restore_access),
                        expandedText = stringResource(R.string.recovery_code_verify),
                        inputLabel = recoveryCodeLabel,
                        errorText = state.recoveryCodeFailureMessage
                            ?: stringResource(R.string.auth_error_failed),
                    ),
                    enabled = state.activeMethod == null ||
                        state.activeMethod == AuthenticationInputMethod.RECOVERY_CODE,
                    onValueChange = onEvent.onRecoveryCodeChange,
                    onExpandedChange = onEvent.onRecoveryCodeExpandedChange,
                    onAction = onEvent.onRecoveryCodeSubmit,
                    onResultConsumed = onEvent.onVerificationFailureConsumed,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
