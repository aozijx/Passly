package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import com.example.poop.data.VaultItem
import com.example.poop.ui.screens.vault.utils.BiometricHelper

@Composable
fun DeleteConfirmationDialog(
    activity: FragmentActivity,
    item: VaultItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除 \"${item.title}\" 吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = {
                BiometricHelper.authenticate(
                    activity = activity,
                    title = "确认删除",
                    subtitle = "请验证身份以执行删除操作"
                ) {
                    onConfirm()
                }
            }) {
                Text("确认删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
