package com.aozijx.passly.features.settings.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.features.settings.state.SettingsUiState

@Composable
fun LockTimeoutDialog(
    currentTimeoutMs: Long,
    onTimeoutSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = SettingsUiState.LOCK_TIMEOUT_PRESETS
    var selectedTimeout by remember(currentTimeoutMs) {
        mutableLongStateOf(currentTimeoutMs.coerceAtLeast(AppDefaults.MIN_LOCK_TIMEOUT_MS))
    }
    var customSeconds by remember(currentTimeoutMs) { mutableStateOf((currentTimeoutMs / 1000L).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动锁定时间") },
        text = {
            Column {
                presets.forEach { (ms, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTimeout = ms
                                customSeconds = (ms / 1000L).toString()
                            }
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTimeout == ms,
                            onClick = {
                                selectedTimeout = ms
                                customSeconds = (ms / 1000L).toString()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customSeconds,
                    onValueChange = { if (it.all(Char::isDigit)) customSeconds = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("自定义秒数（最少 5 秒）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "当前选择: ${formatLockTimeoutText(selectedTimeout)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val customMs = customSeconds.toLongOrNull()?.times(1000L)
                val timeout =
                    if (customMs != null && customMs >= AppDefaults.MIN_LOCK_TIMEOUT_MS) customMs else selectedTimeout
                onTimeoutSelected(timeout)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}