package com.aozijx.passly.feature.settings.general

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aozijx.passly.R
import com.aozijx.passly.core.log.LogExporter
import com.aozijx.passly.core.log.Logcat
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showViewerDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var logContent by remember { mutableStateOf("") }
    var logSize by remember { mutableStateOf("") }

    val logManagementTitle = stringResource(R.string.log_management_title)

    fun refreshLogInfo() {
        scope.launch {
            withContext(Dispatchers.IO) {
                Logcat.readAllLogs().onSuccess { content ->
                    val size = if (content.isEmpty()) "0 B"
                    else "%d KB".format(content.length / 1024)
                    logContent = content
                    logSize = size
                }.onFailure {
                    logContent = ""
                    logSize = "0 B"
                }
            }
        }
    }

    SettingsSectionTitle(text = logManagementTitle)
    RoundedGroup(
        items = listOf(
            navigationSettingsGroupItem(
                key = "logs.view",
            icon = Icons.Default.BugReport,
            title = "查看日志",
            onClick = {
                refreshLogInfo()
                showViewerDialog = true
            }
            ),
            navigationSettingsGroupItem(
                key = "logs.export_all",
            icon = Icons.Default.Description,
            title = "导出所有日志",
            subtitle = "导出所有日志文件为 ZIP",
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        LogExporter.exportAllLogsAsZip(context)
                    }.onSuccess { file ->
                        LogExporter.shareLogsZip(context, file)
                    }
                }
            }
            ),
            navigationSettingsGroupItem(
                key = "logs.export_errors",
            icon = Icons.Default.SaveAlt,
            title = "导出错误日志",
            subtitle = "导出错误日志为文本文件",
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        LogExporter.exportErrorLogsAsTxt(context)
                    }.onSuccess { file ->
                        LogExporter.shareLogsTxt(context, file)
                    }
                }
            }
            ),
            navigationSettingsGroupItem(
                key = "logs.clear",
            icon = Icons.Default.DeleteSweep,
            title = "清除日志",
            value = logSize,
            onClick = { showClearConfirmDialog = true }
        )
        )
    )

    if (showViewerDialog) {
        LogViewerSheet(
            content = logContent,
            onDismiss = { showViewerDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        ClearLogsConfirmDialog(
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        Logcat.clearAllLogs()
                        LogExporter.clearExportedLogs(context)
                    }
                    logContent = ""
                    logSize = "0 B"
                }
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerSheet(content: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetMaxWidth = androidx.compose.ui.unit.Dp.Unspecified,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "日志内容",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = content.ifBlank { "暂无日志" },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
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
        title = { Text("清除日志") },
        text = { Text("确定要清除所有日志文件吗？此操作不可撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("清除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
