package com.aozijx.passly.presentation.feature.backup.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupExportFormatUiModel

@Composable
internal fun BackupFormatPickerContent(
    onFormatSelected: (BackupExportFormatUiModel) -> Unit,
) {
    BackupSheetColumn {
        Text(
            text = stringResource(R.string.settings_backup_format_picker_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.settings_backup_format_picker_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BackupFormatCard(
            icon = Icons.Default.Lock,
            title = stringResource(R.string.settings_backup_format_encrypted),
            subtitle = stringResource(R.string.settings_backup_format_encrypted_description),
            onClick = { onFormatSelected(BackupExportFormatUiModel.ENCRYPTED) },
        )
        BackupFormatCard(
            icon = Icons.Default.Code,
            title = stringResource(R.string.settings_backup_format_json),
            subtitle = stringResource(R.string.settings_backup_format_json_description),
            onClick = { onFormatSelected(BackupExportFormatUiModel.JSON) },
        )
        BackupFormatCard(
            icon = Icons.Default.Description,
            title = stringResource(R.string.settings_backup_format_text),
            subtitle = stringResource(R.string.settings_backup_format_text_description),
            onClick = { onFormatSelected(BackupExportFormatUiModel.TEXT) },
        )
    }
}

@Composable
private fun BackupFormatCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
