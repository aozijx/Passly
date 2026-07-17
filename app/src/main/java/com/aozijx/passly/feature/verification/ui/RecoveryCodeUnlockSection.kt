package com.aozijx.passly.feature.verification.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

@Composable
internal fun RecoveryCodeUnlockSection(
    code: String,
    expanded: Boolean,
    inProgress: Boolean,
    onToggle: () -> Unit,
    onCodeChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onToggle, enabled = !inProgress) {
            Text(
                text = stringResource(R.string.auth_use_recovery_code),
                style = MaterialTheme.typography.labelSmall
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text(stringResource(R.string.auth_recovery_code_label)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = code.isNotBlank() && !inProgress
                ) {
                    Text(stringResource(R.string.auth_recovery_code_unlock))
                }
            }
        }
    }
}
