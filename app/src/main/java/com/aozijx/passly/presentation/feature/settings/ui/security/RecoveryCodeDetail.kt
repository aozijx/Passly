package com.aozijx.passly.presentation.feature.settings.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.crypto.MemoryCleaner
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.core.ui.components.common.InputActionButtonConfig
import com.aozijx.passly.core.ui.components.common.InputActionButtonState
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeDraftStatus
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsAction
import com.aozijx.passly.presentation.feature.settings.security.RecoveryCodeSettingsUiState

@Composable
fun RecoveryCodeDetail(
    state: RecoveryCodeSettingsUiState,
    onAction: (RecoveryCodeSettingsAction) -> Unit,
) {
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val verifyInput = state.verificationInput.toUiString()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 安全提示
        Text(
            text = stringResource(R.string.settings_recovery_code_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(
                R.string.settings_recovery_code_save_hint,
                stringResource(R.string.app_name)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.hasRecoveryCode) {
            Text(
                text = stringResource(R.string.settings_recovery_code_saved),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!state.hasRecoveryCode) {
            Button(
                onClick = { onAction(RecoveryCodeSettingsAction.Generate) },
                enabled = state.draftStatus != RecoveryCodeDraftStatus.CREATING,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text(stringResource(R.string.settings_recovery_code_create))
            }
        }

        if (state.hasRecoveryCode) {
            Spacer(modifier = Modifier.height(16.dp))

            // 验证恢复码
            Text(
                text = stringResource(R.string.recovery_code_verify),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            InputActionButton(
                state = InputActionButtonState(
                    value = verifyInput,
                    expanded = isExpanded,
                    progress = state.isVerifying,
                    result = state.verificationResult,
                ),
                config = InputActionButtonConfig(
                    icon = Icons.Default.Restore,
                    containerColor = when (state.verificationResult) {
                        true -> MaterialTheme.colorScheme.secondaryContainer
                        false -> MaterialTheme.colorScheme.errorContainer
                        else -> null
                    },
                    collapsedText = stringResource(R.string.restore_access),
                    expandedText = stringResource(R.string.recovery_code_verify),
                    inputLabel = stringResource(R.string.recovery_code_label),
                    successText = stringResource(R.string.settings_recovery_code_verify_valid),
                    errorText = stringResource(R.string.settings_recovery_code_verify_invalid),
                ),
                onValueChange = {
                    onAction(RecoveryCodeSettingsAction.VerificationInputChanged(it))
                },
                onExpandedChange = { isExpanded = it },
                onAction = {
                    onAction(RecoveryCodeSettingsAction.Verify)
                },
                onResultConsumed = {
                    onAction(RecoveryCodeSettingsAction.ClearVerificationResult)
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showRegenerateConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_recovery_code_regenerate))
            }
        }
    }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = {
                Text(stringResource(R.string.settings_recovery_code_regenerate))
            },
            text = {
                Text(stringResource(R.string.settings_recovery_code_regenerate_confirm_message))
            },
            confirmButton = {
                TextButton(onClick = {
                    showRegenerateConfirm = false
                    onAction(RecoveryCodeSettingsAction.Generate)
                }) {
                    Text(
                        stringResource(R.string.settings_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun SensitiveValue.toUiString(): String {
    val chars = toCharArray()
    return try {
        String(chars)
    } finally {
        MemoryCleaner.wipeCharArray(chars)
    }
}
