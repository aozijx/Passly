package com.aozijx.passly.presentation.feature.backup.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupExportFormatUiModel
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetEventHandler
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
internal fun BackupExportOptionsContent(
    state: BackupRestoreSheetUiState,
    eventHandler: BackupRestoreSheetEventHandler,
) {
    BackupSheetColumn(scrollable = true) {
        Text(
            text = stringResource(exportTitleResource(state.selectedExportFormat)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        ExportSecurityNotice(state.selectedExportFormat)

        if (state.selectedExportFormat.requiresPassword) {
            OutlinedTextField(
                value = state.password,
                onValueChange = eventHandler::onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_backup_password_label)) },
                supportingText = { Text(stringResource(R.string.settings_backup_password_warning)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
            )
        }

        Text(stringResource(R.string.settings_backup_content_scope), style = MaterialTheme.typography.titleMedium)
        if (state.selectedExportFormat.supportsResources) {
            BackupSwitchRow(
                stringResource(R.string.settings_backup_include_icons),
                stringResource(R.string.settings_backup_include_icons_description),
                state.includeIcons,
                eventHandler::onIncludeIconsChanged,
            )
            BackupSwitchRow(
                stringResource(R.string.settings_backup_include_attachments),
                stringResource(R.string.settings_backup_include_attachments_description),
                state.includeAttachments,
                eventHandler::onIncludeAttachmentsChanged,
            )
        }
        BackupSwitchRow(
            stringResource(R.string.settings_backup_include_deleted),
            stringResource(R.string.settings_backup_include_deleted_description),
            state.includeDeleted,
            eventHandler::onIncludeDeletedChanged,
        )

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_backup_entry_types), style = MaterialTheme.typography.titleMedium)
            TextButton(
                onClick = {
                    eventHandler.onIncludedEntryTypesChanged(
                        if (state.includedEntryTypes.size == EntryTypeUiModel.entries.size) {
                            emptySet()
                        } else {
                            EntryTypeUiModel.entries.toSet()
                        },
                    )
                },
            ) {
                Text(
                    stringResource(
                        if (state.includedEntryTypes.size == EntryTypeUiModel.entries.size) {
                            R.string.settings_backup_clear_selection
                        } else {
                            R.string.settings_backup_select_all
                        },
                    ),
                )
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            EntryTypeUiModel.entries.forEach { type ->
                FilterChip(
                    selected = type in state.includedEntryTypes,
                    onClick = {
                        val updated = state.includedEntryTypes.toMutableSet().apply {
                            if (!add(type)) remove(type)
                        }
                        eventHandler.onIncludedEntryTypesChanged(updated)
                    },
                    label = { Text(stringResource(type.labelRes)) },
                )
            }
        }
        if (state.includedEntryTypes.isEmpty()) {
            Text(
                stringResource(R.string.settings_backup_entry_type_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()
        Text(
            text = state.configuredDirectoryLabel?.let {
                stringResource(R.string.settings_backup_save_to_configured_directory, it)
            } ?: stringResource(R.string.settings_backup_choose_destination_after_confirm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = eventHandler::onExportRequested,
            enabled = state.canSubmitExport,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_backup_start_export))
        }
    }
}

@Composable
private fun ExportSecurityNotice(format: BackupExportFormatUiModel) {
    val text = when (format) {
        BackupExportFormatUiModel.ENCRYPTED -> stringResource(R.string.settings_backup_encrypted_security_notice)
        BackupExportFormatUiModel.JSON -> stringResource(R.string.settings_backup_json_security_notice)
        BackupExportFormatUiModel.TEXT -> stringResource(R.string.settings_backup_text_security_notice)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (format == BackupExportFormatUiModel.ENCRYPTED) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun BackupSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@StringRes
private fun exportTitleResource(format: BackupExportFormatUiModel): Int = when (format) {
    BackupExportFormatUiModel.ENCRYPTED -> R.string.settings_backup_encrypted_options_title
    BackupExportFormatUiModel.JSON -> R.string.settings_backup_json_options_title
    BackupExportFormatUiModel.TEXT -> R.string.settings_backup_text_options_title
}
