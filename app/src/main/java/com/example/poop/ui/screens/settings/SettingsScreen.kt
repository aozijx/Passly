package com.example.poop.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.BuildConfig
import com.example.poop.R
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.ui.screens.settings.components.LogDetailDialog
import com.example.poop.ui.screens.settings.components.SectionTitle
import com.example.poop.ui.screens.settings.components.SettingsClickableItem
import com.example.poop.ui.screens.settings.components.SettingsSwitchItem
import com.example.poop.ui.screens.vault.VaultActivity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val changelogUrl = stringResource(R.string.changelog)

    // 导出处理
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.clearExportStatus() }

    LaunchedEffect(uiState.exportStatus) {
        when (val status = uiState.exportStatus) {
            is ExportStatus.Success -> {
                shareLauncher.launch(Intent.createChooser(status.intent, "导出日志"))
            }
            is ExportStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                viewModel.clearExportStatus()
            }
            else -> {}
        }
    }

    // 导出中加载弹窗
    if (uiState.exportStatus == ExportStatus.Loading) {
        AlertDialog(
            onDismissRequest = { /* 禁止点击外部取消以保证打包完整 */ },
            confirmButton = {},
            title = { Text("正在处理") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("正在准备日志文件...")
                }
            }
        )
    }

    // 权限引导对话框
    if (uiState.showPermissionGuide) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionGuide() },
            title = { Text("需要通知权限") },
            text = { Text("你之前拒绝了通知权限，请前往系统设置手动开启，以便接收重要提醒。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionGuide()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionGuide() }) {
                    Text("取消")
                }
            }
        )
    }

    // 日志显示弹窗
    LogDetailDialog(
        isVisible = uiState.logContent != null || uiState.isLogLoading || uiState.logError != null,
        isLoading = uiState.isLogLoading,
        content = uiState.logContent,
        error = uiState.logError,
        onDismiss = { viewModel.clearLogContent() }
    )

    // 点击计次
    var versionTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(versionTapCount) {
        if (versionTapCount > 0) {
            delay(2000)
            versionTapCount = 0
        }
    }

    TopBarConfig(
        title = "设置",
        centerTitle = true
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionTitle("通用") }
            item {
                SettingsSwitchItem(
                    title = "开启消息通知",
                    checked = uiState.isNotificationsEnabled,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }
            item {
                SettingsClickableItem(
                    title = "权限管理",
                    value = "前往系统设置",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
            item {
                SettingsSwitchItem(
                    title = "深色模式",
                    subtitle = if (uiState.isDarkMode) "已开启" else "跟随系统",
                    checked = uiState.isDarkMode,
                    onCheckedChange = viewModel::toggleDarkMode
                )
            }
            item {
                SettingsSwitchItem(
                    title = "动态颜色",
                    subtitle = "使用系统壁纸颜色作为主题 (Android 12+)",
                    checked = uiState.isDynamicColor,
                    onCheckedChange = viewModel::toggleDynamicColor
                )
            }

            item { SectionTitle("关于") }
            item {
                SettingsClickableItem(
                    title = "清除缓存",
                    value = uiState.cacheSize,
                    onClick = { viewModel.clearCache() }
                )
            }
            item {
                SettingsClickableItem(
                    title = "导出本地日志",
                    subtitle = "将最近的运行日志打包导出",
                    onClick = { viewModel.exportLogs() }
                )
            }
            item {
                SettingsClickableItem(
                    title = "查看更新日志",
                    subtitle = "获取最新版本动态",
                    onClick = {
                        viewModel.fetchLog(changelogUrl)
                    }
                )
            }
            item {
                SettingsClickableItem(
                    title = "版本号",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = {
                        versionTapCount++
                        if (versionTapCount >= 3) {
                            // 点击三次后直接跳
                            context.startActivity(Intent(context, VaultActivity::class.java))
                            versionTapCount = 0
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
