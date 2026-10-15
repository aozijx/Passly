package com.aozijx.passly.presentation.feature.settings.ui.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.feature.settings.main.diagnostics.DiagnosticsUiState

@Composable
fun LogSettingsSection(
    state: DiagnosticsUiState,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRequestClear: () -> Unit,
    onConfirmClear: () -> Unit,
    onDismissClear: () -> Unit,
) {
    var showViewer by rememberSaveable { mutableStateOf(false) }
    val logManagementTitle = stringResource(R.string.settings_log_management_title)
    val viewLogsTitle = stringResource(R.string.settings_log_view_title)
    val viewLogsSubtitle = stringResource(R.string.settings_log_view_subtitle)
    val saveLogsTitle = stringResource(R.string.settings_log_save_action)
    val saveLogsSubtitle = stringResource(R.string.settings_log_save_action_subtitle)
    val shareLogsTitle = stringResource(R.string.settings_log_share_action)
    val shareLogsSubtitle = stringResource(R.string.settings_log_share_action_subtitle)
    val clearLogsTitle = stringResource(R.string.settings_log_clear_action)
    SettingsSectionTitle(text = logManagementTitle)
    SegmentedSettingsGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "logs.view",
                icon = Icons.Default.BugReport,
                title = viewLogsTitle,
                subtitle = viewLogsSubtitle,
                onClick = { showViewer = true },
            ),
            navigationSettingsGroupItem(
                key = "logs.save",
                icon = Icons.Default.SaveAlt,
                title = saveLogsTitle,
                subtitle = saveLogsSubtitle,
                onClick = onSave,
            ),
            navigationSettingsGroupItem(
                key = "logs.share",
                icon = Icons.Default.Share,
                title = shareLogsTitle,
                subtitle = shareLogsSubtitle,
                onClick = onShare,
            ),
            navigationSettingsGroupItem(
                key = "logs.clear",
                icon = Icons.Default.DeleteSweep,
                title = clearLogsTitle,
                value = formatLogSize(state.logByteCount),
                onClick = onRequestClear,
            )
        )
    )

    if (showViewer) {
        LogViewerSheet(
            state = state,
            onDismiss = { showViewer = false },
        )
    }
    if (state.isClearConfirmationVisible) {
        ClearLogsConfirmDialog(
            onConfirm = onConfirmClear,
            onDismiss = onDismissClear,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerSheet(state: DiagnosticsUiState, onDismiss: () -> Unit) {
    val displayText = when {
        state.isLoading -> stringResource(R.string.settings_log_loading)
        state.visibleLines.isEmpty() -> stringResource(R.string.settings_log_empty)
        else -> state.visibleLines.joinToString("\n") { it.text }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetMaxWidth = Dp.Unspecified,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = displayText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun ClearLogsConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_log_clear_action)) },
        text = { Text(stringResource(R.string.settings_log_clear_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_log_clear_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatLogSize(byteCount: Int): String =
    if (byteCount < 1024) "$byteCount B" else "${byteCount / 1024} KB"
