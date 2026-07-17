package com.aozijx.passly.feature.settings.apppassword.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun AppPasswordActionDialog(
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onDisablePassword: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理应用解锁密码") },
        text = { Text("你可以修改密码，或关闭该功能。") },
        confirmButton = {
            TextButton(onClick = onChangePassword) { Text("修改密码") }
        },
        dismissButton = {
            TextButton(onClick = onDisablePassword) { Text("关闭") }
        }
    )
}
