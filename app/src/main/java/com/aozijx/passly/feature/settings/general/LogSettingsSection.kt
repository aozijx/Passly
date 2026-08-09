package com.aozijx.passly.feature.settings.general

import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle

@Composable
fun LogSettingsSection(viewModel: DiagnosticsSettingsViewModel) {
    val context = LocalContext.current
    val fileLoggingEnabled by viewModel.fileLoggingEnabled.collectAsStateWithLifecycle()
    var showViewerDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var logContent by remember { mutableStateOf<String?>(null) }
    var logSize by remember { mutableStateOf("0 B") }

    val logManagementTitle = stringResource(R.string.settings_log_management_title)
    val encryptedLogTitle = stringResource(R.string.settings_log_encrypted_title)
    val encryptedLogSubtitle = stringResource(R.string.settings_log_encrypted_subtitle)
    val viewLogsTitle = stringResource(R.string.settings_log_view_title)
    val exportLogsTitle = stringResource(R.string.settings_log_export_action)
    val exportLogsSubtitle = stringResource(R.string.settings_log_export_action_subtitle)
    val clearLogsTitle = stringResource(R.string.settings_log_clear_action)
    val exportFailedMessage = stringResource(R.string.settings_log_export_failed)

    LaunchedEffect(viewModel, exportFailedMessage) {
        viewModel.events.collect { event ->
            when (event) {
                DiagnosticsSettingsEffect.ExportFailed ->
                    Toast.makeText(context, exportFailedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(showViewerDialog) {
        if (showViewerDialog) {
            logContent = null
            val content = viewModel.readPage()
            logContent = content
            logSize = formatLogSize(content.toByteArray(Charsets.UTF_8).size)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            logContent = null
        }
    }

    SettingsSectionTitle(text = logManagementTitle)
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "logs.diagnostics",
                icon = Icons.Default.BugReport,
                title = encryptedLogTitle,
                subtitle = encryptedLogSubtitle,
                checked = fileLoggingEnabled,
                onCheckedChange = viewModel::setFileLoggingEnabled
            ),
            navigationSettingsGroupItem(
                key = "logs.view",
                iconPlaceholder = true,
                title = viewLogsTitle,
                onClick = {
                    showViewerDialog = true
                }
            ),
            navigationSettingsGroupItem(
                key = "logs.export",
                icon = Icons.Default.SaveAlt,
                title = exportLogsTitle,
                subtitle = exportLogsSubtitle,
                onClick = viewModel::authenticateAndExport
            ),
            navigationSettingsGroupItem(
                key = "logs.clear",
                icon = Icons.Default.DeleteSweep,
                title = clearLogsTitle,
                value = logSize,
                onClick = { showClearConfirmDialog = true }
            )
        )
    )

    if (showViewerDialog) {
        LogViewerSheet(
            content = logContent,
            onDismiss = {
                showViewerDialog = false
                logContent = null
            }
        )
    }

    if (showClearConfirmDialog) {
        ClearLogsConfirmDialog(
            onConfirm = {
                viewModel.clear()
                logContent = null
                logSize = "0 B"
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerSheet(content: String?, onDismiss: () -> Unit) {
    val displayText = when {
        content == null -> stringResource(R.string.settings_log_loading)
        content.isBlank() -> stringResource(R.string.settings_log_empty)
        else -> content
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
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
