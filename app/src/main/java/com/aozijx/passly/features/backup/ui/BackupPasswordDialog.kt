package com.aozijx.passly.features.backup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.features.backup.BackupCoordinator

/**
 * 备份/恢复密码输入对话框
 */
@Composable
fun BackupPasswordDialog(
    backupCoordinator: BackupCoordinator,
    onAuthRequired: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val authTitle = stringResource(R.string.vault_backup_auth_title)
    val authSubtitleExport = stringResource(R.string.vault_backup_auth_subtitle_export)
    val authSubtitleImport = stringResource(R.string.vault_backup_auth_subtitle_import)
    val confirmText = stringResource(R.string.action_confirm)
    val cancelText = stringResource(R.string.action_cancel)
    val passwordLabel = stringResource(R.string.label_password)

    AlertDialog(
        onDismissRequest = { backupCoordinator.dismissBackupPasswordDialog() },
        modifier = Modifier.padding(horizontal = 24.dp),
        title = {
            Text(
                if (backupCoordinator.isExporting) stringResource(R.string.vault_backup_title_export)
                else stringResource(R.string.vault_backup_title_import)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 导入模式选择 (仅在导入时显示)
                if (!backupCoordinator.isExporting) {
                    Text(
                        text = stringResource(R.string.backup_import_mode_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = backupCoordinator.importMode == BackupImportMode.OVERWRITE,
                                    onClick = {
                                        backupCoordinator.importMode = BackupImportMode.OVERWRITE
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = backupCoordinator.importMode == BackupImportMode.OVERWRITE,
                                onClick = {
                                    backupCoordinator.importMode = BackupImportMode.OVERWRITE
                                })
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_import_mode_overwrite_title),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(R.string.backup_import_mode_overwrite_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = backupCoordinator.importMode == BackupImportMode.APPEND,
                                    onClick = {
                                        backupCoordinator.importMode = BackupImportMode.APPEND
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = backupCoordinator.importMode == BackupImportMode.APPEND,
                                onClick = {
                                    backupCoordinator.importMode = BackupImportMode.APPEND
                                })
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.backup_import_mode_append_title),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(R.string.backup_import_mode_append_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. 导出选项：是否包含图片 (仅在导出时显示)
                if (backupCoordinator.isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.backup_include_media_title),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.backup_include_media_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = backupCoordinator.includeImagesInBackup,
                            onCheckedChange = { backupCoordinator.includeImagesInBackup = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. 提示文本
                Text(
                    text = if (backupCoordinator.isExporting) stringResource(R.string.vault_backup_message_export)
                    else stringResource(R.string.vault_backup_message_import),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. 密码输入框
                OutlinedTextField(
                    value = backupCoordinator.backupPassword,
                    onValueChange = { backupCoordinator.backupPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text(passwordLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val canProceed = backupCoordinator.isExporting
                        .let { isExport -> if (isExport) backupCoordinator.backupPassword.isNotEmpty() else true }
                    if (canProceed) {
                         val authSubtitle =
                             if (backupCoordinator.isExporting) authSubtitleExport else authSubtitleImport
                         backupCoordinator.processBackupAction(context) { onSuccess ->
                             onAuthRequired(authTitle, authSubtitle, onSuccess)
                         }
                     }
                 }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { backupCoordinator.dismissBackupPasswordDialog() }) {
                Text(cancelText)
            }
        })
}