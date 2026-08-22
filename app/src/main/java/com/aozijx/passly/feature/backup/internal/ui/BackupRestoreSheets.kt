package com.aozijx.passly.feature.backup.internal.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.text.localizedName
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState

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
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
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
            text = stringResource(R.string.settings_backup_format_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.settings_backup_format_picker_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FormatCard(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.settings_backup_format_encrypted),
            subtitle = stringResource(R.string.settings_backup_format_encrypted_description),
            onClick = { onFormatSelected(BackupExportUiFormat.ENCRYPTED) }
        )
        FormatCard(
            icon = Icons.Default.Code,
            title = stringResource(R.string.settings_backup_format_json),
            subtitle = stringResource(R.string.settings_backup_format_json_description),
            onClick = { onFormatSelected(BackupExportUiFormat.JSON) }
        )
        FormatCard(
            icon = Icons.Default.Description,
            title = stringResource(R.string.settings_backup_format_text),
            subtitle = stringResource(R.string.settings_backup_format_text_description),
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
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
            text = stringResource(exportTitleResource(state.selectedExportFormat)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        ExportSecurityNotice(state.selectedExportFormat)

        if (state.selectedExportFormat.requiresPassword) {
            OutlinedTextField(
                value = state.backupPassword.toTextFieldString(),
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_backup_password_label)) },
                supportingText = {
                    Text(stringResource(R.string.settings_backup_password_warning))
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true
            )
        }

        Text(
            stringResource(R.string.settings_backup_content_scope),
            style = MaterialTheme.typography.titleMedium
        )
        if (state.selectedExportFormat.supportsResources) {
            BackupSwitchRow(
                title = stringResource(R.string.settings_backup_include_icons),
                subtitle = stringResource(R.string.settings_backup_include_icons_description),
                checked = state.includeIcons,
                onCheckedChange = onIncludeIconsChange
            )
            BackupSwitchRow(
                title = stringResource(R.string.settings_backup_include_attachments),
                subtitle = stringResource(R.string.settings_backup_include_attachments_description),
                checked = state.includeAttachments,
                onCheckedChange = onIncludeAttachmentsChange
            )
        }
        BackupSwitchRow(
            title = stringResource(R.string.settings_backup_include_deleted),
            subtitle = stringResource(R.string.settings_backup_include_deleted_description),
            checked = state.includeDeleted,
            onCheckedChange = onIncludeDeletedChange
        )

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.settings_backup_entry_types),
                style = MaterialTheme.typography.titleMedium
            )
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
                    stringResource(
                        if (state.includedEntryTypes.size == EntryType.entries.size) {
                            R.string.settings_backup_clear_selection
                        } else {
                            R.string.settings_backup_select_all
                        }
                    )
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
                    label = { Text(type.localizedName()) }
                )
            }
        }
        if (state.includedEntryTypes.isEmpty()) {
            Text(
                stringResource(R.string.settings_backup_entry_type_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider()
        Text(
            text = if (configuredDirectoryLabel == null) {
                stringResource(R.string.settings_backup_choose_destination_after_confirm)
            } else {
                stringResource(
                    R.string.settings_backup_save_to_configured_directory,
                    configuredDirectoryLabel
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onExport,
            enabled = state.canSubmitExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_backup_start_export))
        }
    }
}

@Composable
private fun ExportSecurityNotice(format: BackupExportUiFormat) {
    val text = when (format) {
        BackupExportUiFormat.ENCRYPTED ->
            stringResource(R.string.settings_backup_encrypted_security_notice)

        BackupExportUiFormat.JSON ->
            stringResource(R.string.settings_backup_json_security_notice)

        BackupExportUiFormat.TEXT ->
            stringResource(R.string.settings_backup_text_security_notice)
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
            text = stringResource(R.string.settings_backup_import_options_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(
                R.string.settings_backup_import_format_description,
                stringResource(R.string.app_name)
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.settings_backup_import_mode),
            style = MaterialTheme.typography.titleMedium
        )
        ImportModeCard(
            selected = state.importMode == ImportMode.APPEND,
            title = stringResource(R.string.settings_backup_import_append),
            subtitle = stringResource(R.string.settings_backup_import_append_description),
            onClick = { onImportModeChange(ImportMode.APPEND) }
        )
        ImportModeCard(
            selected = state.importMode == ImportMode.OVERWRITE,
            title = stringResource(R.string.settings_backup_import_overwrite),
            subtitle = stringResource(R.string.settings_backup_import_overwrite_description),
            onClick = { onImportModeChange(ImportMode.OVERWRITE) }
        )
        OutlinedTextField(
            value = state.backupPassword.toTextFieldString(),
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_backup_import_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            singleLine = true
        )
        Text(
            stringResource(R.string.settings_backup_import_atomicity_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (state.importMode == ImportMode.OVERWRITE) {
                        R.string.settings_backup_confirm_overwrite_import
                    } else {
                        R.string.settings_backup_start_import
                    }
                )
            )
        }
    }
}

private fun SensitiveValue.toTextFieldString(): String {
    val chars = toCharArray()
    return try {
        chars.concatToString()
    } finally {
        chars.fill('\u0000')
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

@StringRes
private fun exportTitleResource(format: BackupExportUiFormat): Int = when (format) {
    BackupExportUiFormat.ENCRYPTED -> R.string.settings_backup_encrypted_options_title
    BackupExportUiFormat.JSON -> R.string.settings_backup_json_options_title
    BackupExportUiFormat.TEXT -> R.string.settings_backup_text_options_title
}
