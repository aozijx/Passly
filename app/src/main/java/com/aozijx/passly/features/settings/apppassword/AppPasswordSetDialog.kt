package com.aozijx.passly.features.settings.apppassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AppPasswordSetDialog(
    newPassword: String,
    confirmPassword: String,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置应用解锁密码") },
        text = {
            Column {
                Text("安全建议：至少 6 位，并混合字母、数字或符号中的两种。")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("新密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("确认密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}