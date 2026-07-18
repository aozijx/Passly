package com.aozijx.passly.feature.settings.security.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.aozijx.passly.R

@Composable
fun RecoveryCodeDetail(
    hasRecoveryEnvelope: Boolean,
    verifyResult: Boolean?,
    onCreateRecoveryCode: () -> Unit,
    onRegenerate: () -> Unit,
    onVerifyCode: (String) -> Unit,
    onClearVerifyResult: () -> Unit,
    onAuthRequired: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit
) {
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var verifyInput by remember { mutableStateOf("") }
    val viewAuthTitle = stringResource(R.string.settings_recovery_code_auth_title)
    val viewAuthSubtitle = stringResource(R.string.settings_recovery_code_auth_subtitle)

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
            text = stringResource(R.string.settings_recovery_code_save_hint),
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
                onClick = {
                    onAuthRequired(viewAuthTitle, viewAuthSubtitle) {
                        onCreateRecoveryCode()
                    }
                },
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

            OutlinedTextField(
                value = verifyInput,
                onValueChange = {
                    verifyInput = it
                    onClearVerifyResult()
                },
                singleLine = true,
                placeholder = {
                    Text(stringResource(R.string.settings_recovery_code_verify_hint))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (verifyInput.isNotBlank()) {
                        onVerifyCode(verifyInput.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = verifyInput.isNotBlank()
            ) {
                Text(stringResource(R.string.verify))
            }

            // 验证结果
            verifyResult?.let { valid ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (valid) Icons.Default.CheckCircle
                        else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (valid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            if (valid) R.string.settings_recovery_code_verify_valid
                            else R.string.settings_recovery_code_verify_invalid
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (valid) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showRegenerateConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
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
                    onAuthRequired(viewAuthTitle, viewAuthSubtitle, onRegenerate)
                }) {
                    Text(
                        stringResource(R.string.confirm),
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
