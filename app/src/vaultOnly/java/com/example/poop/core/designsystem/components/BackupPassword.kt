package com.example.poop.core.designsystem.components

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.poop.MainViewModel
import com.example.poop.R
import com.example.poop.core.util.BackupManager
import com.example.poop.features.settings.SettingsViewModel

/**
 * 备份/恢复密码输入对话框
 */
@Composable
fun BackupPasswordDialog(
    activity: FragmentActivity, 
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val authTitle = stringResource(R.string.vault_backup_auth_title)
    val authSubtitleExport = stringResource(R.string.vault_backup_auth_subtitle_export)
    val authSubtitleImport = stringResource(R.string.vault_backup_auth_subtitle_import)
    val confirmText = stringResource(R.string.action_confirm)
    val cancelText = stringResource(R.string.action_cancel)
    val passwordLabel = stringResource(R.string.label_password)

    AlertDialog(
        onDismissRequest = { settingsViewModel.dismissBackupPasswordDialog() },
        title = {
            Text(
                if (settingsViewModel.isExporting) stringResource(R.string.vault_backup_title_export)
                else stringResource(R.string.vault_backup_title_import)
            )
        },
        text = {
            Column {
                Text(
                    text = if (settingsViewModel.isExporting) stringResource(R.string.vault_backup_message_export)
                    else stringResource(R.string.vault_backup_message_import),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = settingsViewModel.backupPassword,
                    onValueChange = { settingsViewModel.backupPassword = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = { Text(passwordLabel) }
                )
                
                if (!settingsViewModel.isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "导入模式",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settingsViewModel.importMode == BackupManager.ImportMode.OVERWRITE,
                                    onClick = { settingsViewModel.importMode = BackupManager.ImportMode.OVERWRITE },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsViewModel.importMode == BackupManager.ImportMode.OVERWRITE,
                                onClick = { settingsViewModel.importMode = BackupManager.ImportMode.OVERWRITE }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "覆盖现有数据", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "删除所有现有数据，然后导入备份内容",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = settingsViewModel.importMode == BackupManager.ImportMode.APPEND,
                                    onClick = { settingsViewModel.importMode = BackupManager.ImportMode.APPEND },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settingsViewModel.importMode == BackupManager.ImportMode.APPEND,
                                onClick = { settingsViewModel.importMode = BackupManager.ImportMode.APPEND }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "追加新数据", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "保留现有数据，将备份内容追加到数据库中",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (settingsViewModel.backupPassword.isNotEmpty()) {
                        val authSubtitle = if (settingsViewModel.isExporting) authSubtitleExport else authSubtitleImport
                        mainViewModel.authenticate(
                            activity = activity,
                            title = authTitle,
                            subtitle = authSubtitle
                        ) {
                            settingsViewModel.processBackupAction(context)
                        }
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = { settingsViewModel.dismissBackupPasswordDialog() }) {
                Text(cancelText)
            }
        }
    )
}
