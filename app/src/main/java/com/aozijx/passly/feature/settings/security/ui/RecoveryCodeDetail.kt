package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.common.InputActionButton
import com.aozijx.passly.security.crypto.SecureString

@Composable
fun RecoveryCodeDetail(
    hasRecoveryEnvelope: Boolean,
    verifyResult: Boolean?,
    onCreateRecoveryCode: () -> Unit,
    onRegenerate: () -> Unit,
    onVerifyCode: (CharArray) -> Unit,
    onClearVerifyResult: () -> Unit
) {
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var verifyInput by remember { mutableStateOf(SecureString.EMPTY) }
    var isExpanded by remember { mutableStateOf(false) }

    // 显式追踪验证进度，不依赖 verifyInput/isExpanded 的同步状态推导
    var isVerifying by remember { mutableStateOf(false) }
    LaunchedEffect(verifyResult) {
        if (verifyResult != null) {
            isVerifying = false
        }
    }

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

        if (hasRecoveryEnvelope) {
            Text(
                text = stringResource(R.string.settings_recovery_code_saved),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!hasRecoveryEnvelope) {
            Button(
                onClick = onCreateRecoveryCode,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_recovery_code_create))
            }
        }

        if (hasRecoveryEnvelope) {
            Spacer(modifier = Modifier.height(16.dp))

            // 验证恢复码
            Text(
                text = stringResource(R.string.settings_recovery_code_verify_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            InputActionButton(
                value = verifyInput,
                expanded = isExpanded,
                progress = isVerifying,
                result = verifyResult,
                icon = Icons.Default.Restore,
                containerColor = when (verifyResult) {
                    true -> MaterialTheme.colorScheme.secondaryContainer
                    false -> MaterialTheme.colorScheme.errorContainer
                    else -> null
                },
                collapsedText = stringResource(R.string.auth_use_recovery_code),
                expandedText = stringResource(R.string.auth_recovery_code_unlock),
                inputLabel = stringResource(R.string.auth_recovery_code_label),
                successText = stringResource(R.string.settings_recovery_code_verify_valid),
                errorText = stringResource(R.string.settings_recovery_code_verify_invalid),
                onValueChange = {
                    verifyInput = SecureString.fromString(it)
                    onClearVerifyResult()
                },
                onExpandedChange = { isExpanded = it },
                onAction = {
                    if (!verifyInput.isEmpty) {
                        isVerifying = true
                        onVerifyCode(verifyInput.toCharArray())
                    }
                },
                onResultConsumed = onClearVerifyResult
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { showRegenerateConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                    onRegenerate()
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
