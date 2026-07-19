package com.aozijx.passly.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.domain.model.backup.ImportMode

data class BackupPasswordDialogState(
    val isExporting: Boolean = false,
    val importMode: ImportMode = ImportMode.OVERWRITE,
    val includeImages: Boolean = false,
    val backupPassword: String = ""
)

@Composable
fun BackupPasswordDialog(
    state: BackupPasswordDialogState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onImportModeChange: (ImportMode) -> Unit,
    onIncludeImagesChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)
    val passwordLabel = stringResource(R.string.password)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.padding(horizontal = 24.dp),
        title = {
            Text(
                if (state.isExporting) stringResource(R.string.vault_backup_title_export)
                else stringResource(R.string.vault_backup_title_import)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. 导入模式选择 (仅在导入时显示)
                if (!state.isExporting) {
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
                                    selected = state.importMode == ImportMode.OVERWRITE,
                                    onClick = { onImportModeChange(ImportMode.OVERWRITE) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.importMode == ImportMode.OVERWRITE,
                                onClick = { onImportModeChange(ImportMode.OVERWRITE) })
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
                                    selected = state.importMode == ImportMode.APPEND,
                                    onClick = { onImportModeChange(ImportMode.APPEND) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.importMode == ImportMode.APPEND,
                                onClick = { onImportModeChange(ImportMode.APPEND) })
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
                if (state.isExporting) {
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
                            checked = state.includeImages,
                            onCheckedChange = onIncludeImagesChange
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. 提示文本
                Text(
                    text = if (state.isExporting) stringResource(R.string.vault_backup_message_export)
                    else stringResource(R.string.vault_backup_message_import),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. 密码输入框
                OutlinedTextField(
                    value = state.backupPassword,
                    onValueChange = onPasswordChange,
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    label = {
                        Text(passwordLabel)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val canProceed =
                        if (state.isExporting) state.backupPassword.isNotEmpty() else true
                    if (canProceed) {
                        onConfirm()
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        }
    )
}