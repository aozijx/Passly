package com.aozijx.passly.presentation.ui.settings.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupImportModeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetEventHandler
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetUiState

@Composable
internal fun BackupImportOptionsContent(
    state: BackupRestoreSheetUiState,
    eventHandler: BackupRestoreSheetEventHandler,
) {
    BackupSheetColumn(scrollable = true) {
        Text(
            text = stringResource(R.string.settings_backup_import_options_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(R.string.settings_backup_import_format_description, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(R.string.settings_backup_import_mode), style = MaterialTheme.typography.titleMedium)
        ImportModeCard(
            selected = state.importMode == BackupImportModeUiModel.APPEND,
            title = stringResource(R.string.settings_backup_import_append),
            subtitle = stringResource(R.string.settings_backup_import_append_description),
            onClick = { eventHandler.onImportModeChanged(BackupImportModeUiModel.APPEND) },
        )
        ImportModeCard(
            selected = state.importMode == BackupImportModeUiModel.OVERWRITE,
            title = stringResource(R.string.settings_backup_import_overwrite),
            subtitle = stringResource(R.string.settings_backup_import_overwrite_description),
            onClick = { eventHandler.onImportModeChanged(BackupImportModeUiModel.OVERWRITE) },
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = eventHandler::onPasswordChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_backup_import_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            singleLine = true,
        )
        Text(
            stringResource(R.string.settings_backup_import_atomicity_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = eventHandler::onImportRequested, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (state.importMode == BackupImportModeUiModel.OVERWRITE) {
                        R.string.settings_backup_confirm_overwrite_import
                    } else {
                        R.string.settings_backup_start_import
                    },
                ),
            )
        }
    }
}

@Composable
private fun ImportModeCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
