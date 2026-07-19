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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle
import kotlinx.coroutines.launch

@Composable
fun LogSettingsSection(viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fileLoggingEnabled by viewModel.fileLoggingEnabled.collectAsStateWithLifecycle()
    var showViewerDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var logContent by remember { mutableStateOf("") }
    var logSize by remember { mutableStateOf("") }

    val logManagementTitle = stringResource(R.string.log_management_title)

    fun refreshLogInfo() {
        scope.launch {
            val content = viewModel.readPage()
            logContent = content
            logSize = if (content.isEmpty()) "0 B" else "%d KB".format(content.length / 1024)
        }
    }

    SettingsSectionTitle(text = logManagementTitle)
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "logs.diagnostics",
                icon = Icons.Default.BugReport,
                title = "加密诊断日志",
                subtitle = "开启后记录 24 小时",
                checked = fileLoggingEnabled,
                onCheckedChange = viewModel::setFileLoggingEnabled
            ),
            navigationSettingsGroupItem(
                key = "logs.view",
                title = "查看日志",
                onClick = {
                    refreshLogInfo()
                    showViewerDialog = true
                }
            ),
            navigationSettingsGroupItem(
                key = "logs.export",
                icon = Icons.Default.SaveAlt,
                title = "导出日志",
                subtitle = "验证身份后生成临时明文文件",
                onClick = { viewModel.authenticateAndExport(context) }
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
                viewModel.clear()
                logContent = ""
                logSize = "0 B"
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
        ) {
            Text(
                text = content.ifBlank { "暂无日志" },
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
