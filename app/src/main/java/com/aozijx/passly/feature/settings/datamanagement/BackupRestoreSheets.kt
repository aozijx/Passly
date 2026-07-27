package com.aozijx.passly.feature.settings.datamanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.contract.BackupUiState
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat

internal enum class BackupSheet {
    FORMAT_PICKER,
    EXPORT_OPTIONS,
    IMPORT_OPTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRestoreSheetHost(
    sheet: BackupSheet?,
    state: BackupUiState,
    configuredDirectoryLabel: String?,
    onDismiss: () -> Unit,
    onFormatSelected: (BackupExportUiFormat) -> Unit,
    onPasswordChange: (String) -> Unit,
    onIncludeIconsChange: (Boolean) -> Unit,
    onIncludeAttachmentsChange: (Boolean) -> Unit,
    onIncludeDeletedChange: (Boolean) -> Unit,
    onIncludedEntryTypesChange: (Set<EntryType>) -> Unit,
    onImportModeChange: (ImportMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    if (sheet == null) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        when (sheet) {
            BackupSheet.FORMAT_PICKER -> BackupFormatPicker(
                onFormatSelected = onFormatSelected
            )
            BackupSheet.EXPORT_OPTIONS -> BackupExportOptionsContent(
                state = state,
                configuredDirectoryLabel = configuredDirectoryLabel,
                onPasswordChange = onPasswordChange,
                onIncludeIconsChange = onIncludeIconsChange,
                onIncludeAttachmentsChange = onIncludeAttachmentsChange,
                onIncludeDeletedChange = onIncludeDeletedChange,
                onIncludedEntryTypesChange = onIncludedEntryTypesChange,
                onExport = onExport
            )
            BackupSheet.IMPORT_OPTIONS -> BackupImportOptionsContent(
                state = state,
                onPasswordChange = onPasswordChange,
                onImportModeChange = onImportModeChange,
                onImport = onImport
            )
        }
    }
}

@Composable
private fun BackupFormatPicker(
    onFormatSelected: (BackupExportUiFormat) -> Unit
) {
    SheetColumn {
        Text(
            text = "选择导出格式",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "每种格式使用独立的导出设置。加密备份适合恢复，JSON 适合迁移，TXT 仅供人工阅读。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FormatCard(
            icon = Icons.Default.Lock,
            title = "加密备份",
            subtitle = "完整、自描述、密码加密，可用于恢复",
            onClick = { onFormatSelected(BackupExportUiFormat.ENCRYPTED) }
        )
        FormatCard(
            icon = Icons.Default.Code,
            title = "JSON 备份",
            subtitle = "明文结构化数据，可用于迁移和外部处理",
            onClick = { onFormatSelected(BackupExportUiFormat.JSON) }
        )
        FormatCard(
            icon = Icons.Default.Description,
            title = "可读 TXT",
            subtitle = "每个字段单独成行，不包含图片和附件",
            onClick = { onFormatSelected(BackupExportUiFormat.TEXT) }
        )
    }
}

@Composable
private fun FormatCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BackupExportOptionsContent(
    state: BackupUiState,
    configuredDirectoryLabel: String?,
    onPasswordChange: (String) -> Unit,
    onIncludeIconsChange: (Boolean) -> Unit,
    onIncludeAttachmentsChange: (Boolean) -> Unit,
    onIncludeDeletedChange: (Boolean) -> Unit,
    onIncludedEntryTypesChange: (Set<EntryType>) -> Unit,
    onExport: () -> Unit
) {
    SheetColumn(scrollable = true) {
        Text(
            text = exportTitle(state.selectedExportFormat),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        ExportSecurityNotice(state.selectedExportFormat)

        if (state.selectedExportFormat.requiresPassword) {
            OutlinedTextField(
                value = state.backupPassword,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备份密码") },
                supportingText = { Text("密码不会保存，遗失后无法恢复备份") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
        }

        Text("内容范围", style = MaterialTheme.typography.titleMedium)
        if (state.selectedExportFormat.supportsResources) {
            BackupSwitchRow(
                title = "自定义图标",
                subtitle = "导出保存在应用中的条目图标",
                checked = state.includeIcons,
                onCheckedChange = onIncludeIconsChange
            )
            BackupSwitchRow(
                title = "附件与图片",
                subtitle = "导出已提交的附件文件，可能显著增加文件大小",
                checked = state.includeAttachments,
                onCheckedChange = onIncludeAttachmentsChange
            )
        }
        BackupSwitchRow(
            title = "回收站条目",
            subtitle = "包含尚未永久删除的条目",
            checked = state.includeDeleted,
            onCheckedChange = onIncludeDeletedChange
        )

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("条目类型", style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = {
                    onIncludedEntryTypesChange(
                        if (state.includedEntryTypes.size == EntryType.entries.size) {
                            emptySet()
                        } else {
                            EntryType.entries.toSet()
                        }
                    )
                }
            ) {
                Text(
                    if (state.includedEntryTypes.size == EntryType.entries.size) "清除"
                    else "全选"
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EntryType.entries.forEach { type ->
                FilterChip(
                    selected = type in state.includedEntryTypes,
                    onClick = {
                        val updated = state.includedEntryTypes.toMutableSet().apply {
                            if (!add(type)) remove(type)
                        }
                        onIncludedEntryTypesChange(updated)
                    },
                    label = { Text(type.displayName) }
                )
            }
        }
        if (state.includedEntryTypes.isEmpty()) {
            Text(
                "至少选择一种条目类型",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider()
        Text(
            text = if (configuredDirectoryLabel == null) {
                "确认后选择保存位置"
            } else {
                "将保存到默认目录：$configuredDirectoryLabel"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onExport,
            enabled = state.canSubmitExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始导出")
        }
    }
}

@Composable
private fun ExportSecurityNotice(format: BackupExportUiFormat) {
    val text = when (format) {
        BackupExportUiFormat.ENCRYPTED ->
            "使用独立随机盐和 nonce 加密，可包含完整恢复数据。"
        BackupExportUiFormat.JSON ->
            "JSON 是明文文件，可能包含密码、OTP Secret 和附件内容。请仅保存到可信位置。"
        BackupExportUiFormat.TEXT ->
            "TXT 是明文且不可用于完整恢复，仅包含便于人工阅读的字段。"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (format == BackupExportUiFormat.ENCRYPTED) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        }
    )
}

@Composable
private fun BackupSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackupImportOptionsContent(
    state: BackupUiState,
    onPasswordChange: (String) -> Unit,
    onImportModeChange: (ImportMode) -> Unit,
    onImport: () -> Unit
) {
    SheetColumn(scrollable = true) {
        Text(
            text = "导入与恢复",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "文件格式会根据内容自动识别。加密 Passly 备份需要输入正确密码；JSON 和第三方格式可留空。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("导入方式", style = MaterialTheme.typography.titleMedium)
        ImportModeCard(
            selected = state.importMode == ImportMode.APPEND,
            title = "合并",
            subtitle = "保留现有数据，并写入备份条目",
            onClick = { onImportModeChange(ImportMode.APPEND) }
        )
        ImportModeCard(
            selected = state.importMode == ImportMode.OVERWRITE,
            title = "覆盖",
            subtitle = "清空现有保险库后恢复备份，风险较高",
            onClick = { onImportModeChange(ImportMode.OVERWRITE) }
        )
        OutlinedTextField(
            value = state.backupPassword,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("备份密码（如需要）") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Text(
            "导入前会验证完整文件；失败不会留下部分恢复数据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.importMode == ImportMode.OVERWRITE) "确认覆盖并导入" else "开始导入")
        }
    }
}

@Composable
private fun ImportModeCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetColumn(
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (scrollable) Modifier.verticalScroll(rememberScrollState())
                else Modifier
            )
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

private fun exportTitle(format: BackupExportUiFormat): String = when (format) {
    BackupExportUiFormat.ENCRYPTED -> "加密备份设置"
    BackupExportUiFormat.JSON -> "JSON 导出设置"
    BackupExportUiFormat.TEXT -> "TXT 导出设置"
}
