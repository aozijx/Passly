package com.example.poop.ui.screens.vault.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.VaultViewModel

@Composable
fun BackupPasswordDialog(activity: FragmentActivity, viewModel: VaultViewModel) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { viewModel.dismissBackupPasswordDialog() },
        title = { Text(if (viewModel.isExporting) "设置备份密码" else "输入备份密码") },
        text = {
            Column {
                Text(
                    text = if (viewModel.isExporting) "请设置一个密码来加密备份文件。恢复时需要此密码。"
                           else "请输入备份文件的密码以解密数据。"
                )
                TextField(
                    value = viewModel.backupPassword,
                    onValueChange = { viewModel.backupPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text("密码") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (viewModel.backupPassword.isNotEmpty()) {
                        viewModel.authenticate(
                            activity = activity,
                            title = "验证身份",
                            subtitle = if (viewModel.isExporting) "验证以导出数据" else "验证以导入数据"
                        ) {
                            viewModel.processBackupAction(context)
                        }
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissBackupPasswordDialog() }) {
                Text("取消")
            }
        }
    )
}
