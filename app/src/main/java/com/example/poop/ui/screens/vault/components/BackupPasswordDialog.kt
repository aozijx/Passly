package com.example.poop.ui.screens.vault.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.ui.screens.vault.VaultViewModel
import com.example.poop.util.BackupManager
import com.example.poop.util.BiometricHelper

@Composable
fun BackupPasswordDialog(
    activity: FragmentActivity,
    viewModel: VaultViewModel
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { viewModel.dismissBackupPasswordDialog() },
        title = { Text(if (viewModel.isExporting) "设置备份密码" else "导入恢复选项") },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                Text(
                    if (viewModel.isExporting) 
                        "此密码用于加密备份文件，请务必牢记。若忘记此密码，备份数据将无法恢复。" 
                    else "请输入导出该备份文件时设置的密码，并选择导入方式。"
                )
                
                if (!viewModel.isExporting) {
                    Column(Modifier.selectableGroup()) {
                        Text("导入方式：", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row(
                            Modifier.fillMaxWidth().height(48.dp)
                                .selectable(
                                    selected = (viewModel.importMode == BackupManager.ImportMode.OVERWRITE),
                                    onClick = { viewModel.importMode = BackupManager.ImportMode.OVERWRITE },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (viewModel.importMode == BackupManager.ImportMode.OVERWRITE),
                                onClick = null
                            )
                            Text(text = "覆盖现有数据", modifier = Modifier.padding(start = 16.dp))
                        }
                        Row(
                            Modifier.fillMaxWidth().height(48.dp)
                                .selectable(
                                    selected = (viewModel.importMode == BackupManager.ImportMode.APPEND),
                                    onClick = { viewModel.importMode = BackupManager.ImportMode.APPEND },
                                    role = Role.RadioButton
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (viewModel.importMode == BackupManager.ImportMode.APPEND),
                                onClick = null
                            )
                            Text(text = "追加到现有数据", modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.backupPassword,
                    onValueChange = { viewModel.backupPassword = it },
                    label = { Text("备份密码") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = if (viewModel.isExporting) "备份导出验证" else "恢复导入验证",
                        subtitle = "请验证身份以执行该安全操作"
                    ) {
                        viewModel.processBackupAction(context)
                    }
                },
                enabled = viewModel.backupPassword.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissBackupPasswordDialog() }) { Text("取消") }
        }
    )
}
