package com.aozijx.passly.presentation.feature.settings.ui.diagnostics

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.feature.settings.diagnostics.DiagnosticsLogLine
import com.aozijx.passly.feature.settings.diagnostics.ExportAction
import com.aozijx.passly.feature.settings.diagnostics.LogFormat
import com.aozijx.passly.presentation.feature.settings.main.diagnostics.DiagnosticsAction
import com.aozijx.passly.presentation.feature.settings.main.diagnostics.DiagnosticsUiState
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onAction: (DiagnosticsAction) -> Unit,
    onBack: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedLine by remember { mutableStateOf<DiagnosticsLogLine?>(null) }

    SettingsSecondaryPage(
        title = stringResource(R.string.settings_diagnostics_title),
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.settings_diagnostics_menu))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_log_save_action)) },
                        onClick = {
                            menuExpanded = false
                            onAction(DiagnosticsAction.RequestExport(ExportAction.EXPORT_LOCAL))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_log_share_action)) },
                        onClick = {
                            menuExpanded = false
                            onAction(DiagnosticsAction.RequestExport(ExportAction.SHARE))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_log_clear_action)) },
                        onClick = {
                            menuExpanded = false
                            onAction(DiagnosticsAction.RequestClear)
                        },
                    )
                }
            }
        },
    ) {
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onAction(DiagnosticsAction.SearchChanged(it)) },
                label = { Text(stringResource(R.string.settings_diagnostics_trace_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            LevelFilters(state, onAction)
        }
        when {
            state.isLoading -> item { StatusText(stringResource(R.string.settings_log_loading)) }
            state.isSessionRestricted -> item {
                StatusText(stringResource(R.string.settings_diagnostics_session_restricted))
            }
            state.visibleLines.isEmpty() -> item { StatusText(stringResource(R.string.settings_log_empty)) }
            else -> items(state.visibleLines.size) { index ->
                val line = state.visibleLines[index]
                Text(
                    text = line.text,
                    fontFamily = FontFamily.Monospace,
                    color = line.level.semanticColor(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { selectedLine = line },
                        )
                        .padding(vertical = 6.dp),
                )
            }
        }
    }

    state.pendingExportAction?.let { action ->
        FormatSheet(
            action = action,
            onSelect = { onAction(DiagnosticsAction.SelectFormat(it)) },
            onDismiss = { onAction(DiagnosticsAction.DismissFormatSheet) },
        )
    }
    if (state.isClearConfirmationVisible) {
        ConfirmClearDialog(onAction)
    }
    selectedLine?.let { line ->
        CopyDialog(
            onCopyRow = {
                selectedLine = null
                onAction(DiagnosticsAction.CopyRow(line))
            },
            onCopyTrace = {
                selectedLine = null
                onAction(DiagnosticsAction.CopyTrace(line.traceId))
            },
            onDismiss = { selectedLine = null },
        )
    }
}

@Composable
private fun LevelFilters(state: DiagnosticsUiState, onAction: (DiagnosticsAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(LogLevel.ERROR, LogLevel.WARN, LogLevel.INFO, LogLevel.DEBUG).forEach { level ->
            FilterChip(
                selected = level in state.selectedLevels,
                onClick = { onAction(DiagnosticsAction.ToggleLevel(level)) },
                label = { Text(level.name) },
            )
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSheet(
    action: ExportAction,
    onSelect: (LogFormat) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text(
                text = if (action == ExportAction.SHARE) {
                    stringResource(R.string.settings_diagnostics_share_format)
                } else {
                    stringResource(R.string.settings_diagnostics_export_format)
                },
                style = MaterialTheme.typography.titleLarge,
            )
            FormatOption(LogFormat.JSONL, R.string.settings_diagnostics_jsonl_description, onSelect)
            FormatOption(LogFormat.TSV, R.string.settings_diagnostics_tsv_description, onSelect)
            FormatOption(LogFormat.ZIP, R.string.settings_diagnostics_zip_description, onSelect)
        }
    }
}

@Composable
private fun FormatOption(format: LogFormat, description: Int, onSelect: (LogFormat) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(format) }
            .padding(vertical = 14.dp),
    ) {
        Text(format.name, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ConfirmClearDialog(onAction: (DiagnosticsAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(DiagnosticsAction.DismissClear) },
        title = { Text(stringResource(R.string.settings_log_clear_action)) },
        text = { Text(stringResource(R.string.settings_log_clear_confirm_message)) },
        confirmButton = {
            TextButton(onClick = { onAction(DiagnosticsAction.ConfirmClear) }) {
                Text(stringResource(R.string.settings_log_clear_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(DiagnosticsAction.DismissClear) }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CopyDialog(onCopyRow: () -> Unit, onCopyTrace: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_diagnostics_copy_title)) },
        confirmButton = {
            TextButton(onClick = onCopyTrace) {
                Text(stringResource(R.string.settings_diagnostics_copy_trace))
            }
        },
        dismissButton = {
            TextButton(onClick = onCopyRow) {
                Text(stringResource(R.string.settings_diagnostics_copy_row))
            }
        },
    )
}

@Composable
private fun LogLevel.semanticColor(): Color = when (this) {
    LogLevel.FATAL -> MaterialTheme.colorScheme.error
    LogLevel.ERROR -> MaterialTheme.colorScheme.error
    LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
    LogLevel.INFO,
    LogLevel.DEBUG,
        -> MaterialTheme.colorScheme.onSurface
}
