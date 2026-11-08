package com.aozijx.passly.feature.backup.presentation.ui

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
import com.aozijx.passly.core.ui.autofill.excludeFromAutofill
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.presentation.BackupUiAction
import com.aozijx.passly.feature.backup.presentation.ui.model.BackupRestoreSheetUiState
import com.aozijx.passly.presentation.shared.entry.labelRes

@Composable
internal fun BackupExportOptionsContent(
    state: BackupRestoreSheetUiState.ExportOptions,
    onAction: (BackupUiAction) -> Unit,
) {
    BackupSheetColumn(scrollable = true) {
        Text(
            text = stringResource(state.titleResource),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        ExportSecurityNotice(state.securityNoticeResource, state.securityNoticeIsWarning)

        if (state.requiresPassword) {
            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(BackupUiAction.UpdatePassword(it)) },
                modifier = Modifier.fillMaxWidth().excludeFromAutofill(),
                label = { Text(stringResource(R.string.settings_backup_password_label)) },
                supportingText = { Text(stringResource(R.string.settings_backup_password_warning)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                singleLine = true,
            )
        }

        Text(stringResource(R.string.settings_backup_content_scope), style = MaterialTheme.typography.titleMedium)
        if (state.supportsResources) {
            BackupSwitchRow(
                stringResource(R.string.settings_backup_include_icons),
                stringResource(R.string.settings_backup_include_icons_description),
                state.includeIcons,
                { onAction(BackupUiAction.UpdateIncludeIcons(it)) },
            )
            BackupSwitchRow(
                stringResource(R.string.settings_backup_include_attachments),
                stringResource(R.string.settings_backup_include_attachments_description),
                state.includeAttachments,
                { onAction(BackupUiAction.UpdateIncludeAttachments(it)) },
            )
        }
        BackupSwitchRow(
            stringResource(R.string.settings_backup_include_deleted),
            stringResource(R.string.settings_backup_include_deleted_description),
            state.includeDeleted,
            { onAction(BackupUiAction.UpdateIncludeDeleted(it)) },
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
                    onAction(
                        if (state.allEntryTypesSelected) BackupUiAction.ClearEntryTypes
                        else BackupUiAction.SelectAllEntryTypes,
                    )
                },
            ) {
                Text(
                    stringResource(
                        if (state.allEntryTypesSelected) {
                            R.string.clear
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
            EntryType.entries.forEach { type ->
                FilterChip(
                    selected = type in state.includedEntryTypes,
                    onClick = { onAction(BackupUiAction.ToggleIncludedEntryType(type)) },
                    label = { Text(stringResource(type.labelRes)) },
                )
            }
        }
        if (state.needsEntryTypeSelection) {
            Text(
                stringResource(R.string.settings_backup_entry_type_required),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider()
        Text(
            text =
                state.configuredDirectoryLabel?.let {
                    stringResource(R.string.settings_backup_save_to_configured_directory, it)
                } ?: stringResource(R.string.settings_backup_choose_destination_after_confirm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                onAction(
                    BackupUiAction.SubmitExport(
                        useConfiguredDirectory = state.configuredDirectoryLabel != null,
                    ),
                )
            },
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_backup_start_export))
        }
    }
}

@Composable
private fun ExportSecurityNotice(
    @StringRes messageResource: Int,
    isWarning: Boolean,
) {
    Text(
        text = stringResource(messageResource),
        style = MaterialTheme.typography.bodyMedium,
        color =
            if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
