package com.example.poop.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.poop.BuildConfig
import com.example.poop.R
import com.example.poop.i8n.LocaleConfigReader
import com.example.poop.ui.navigation.Screen
import com.example.poop.ui.navigation.TopBarConfig
import com.example.poop.ui.screens.settings.components.AllRoundedShape
import com.example.poop.ui.screens.settings.components.BottomRoundedShape
import com.example.poop.ui.screens.settings.components.LogDetailDialog
import com.example.poop.ui.screens.settings.components.MiddleShape
import com.example.poop.ui.screens.settings.components.SectionTitle
import com.example.poop.ui.screens.settings.components.SettingsClickableItem
import com.example.poop.ui.screens.settings.components.SettingsSwitchItem
import com.example.poop.ui.screens.settings.components.TopRoundedShape
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val changelogUrl = stringResource(R.string.changelog)

    val languages = remember(context) {
        LocaleConfigReader.getSupportedLanguages(context)
    }

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

    if (uiState.exportStatus == ExportStatus.Loading) {
        AlertDialog(
            onDismissRequest = { },
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

    LogDetailDialog(
        isVisible = uiState.logContent != null || uiState.isLogLoading || uiState.logError != null,
        isLoading = uiState.isLogLoading,
        content = uiState.logContent,
        error = uiState.logError,
        title = uiState.logTitle,
        onDismiss = { viewModel.clearLogContent() }
    )

    var showLanguageDialog by remember { mutableStateOf(false) }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("选择语言", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    languages.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setLanguage(option.tag)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.language == option.tag,
                                onClick = {
                                    viewModel.setLanguage(option.tag)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionTitle("通知与权限") }
            item {
                SettingsSwitchItem(
                    title = "开启消息通知",
                    subtitle = "接收重要提醒与更新通知",
                    checked = uiState.isNotificationsEnabled,
                    onCheckedChange = viewModel::toggleNotifications,
                    shape = TopRoundedShape,
                    showDivider = true,
                    icon = Icons.Default.Notifications
                )
            }
            item {
                SettingsClickableItem(
                    title = "权限管理",
                    subtitle = "管理应用所需的系统权限",
                    value = "系统设置",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    shape = BottomRoundedShape,
                    itemIcon = Icons.Default.Security
                )
            }

            item { SectionTitle("外观展示") }
            item {
                SettingsSwitchItem(
                    title = "深色模式",
                    subtitle = if (uiState.isDarkMode) "已开启" else "跟随系统设置",
                    checked = uiState.isDarkMode,
                    onCheckedChange = viewModel::toggleDarkMode,
                    shape = TopRoundedShape,
                    showDivider = true,
                    icon = Icons.Default.DarkMode
                )
            }
            item {
                SettingsSwitchItem(
                    title = "动态颜色",
                    subtitle = "从壁纸提取主色调 (仅 Android 12+)",
                    checked = uiState.isDynamicColor,
                    onCheckedChange = viewModel::toggleDynamicColor,
                    shape = MiddleShape,
                    showDivider = true,
                    icon = Icons.Default.Palette
                )
            }
            item {
                SettingsClickableItem(
                    title = "语言设置",
                    subtitle = "设置应用显示语言",
                    value = languages.find { it.tag == uiState.language }?.displayName ?: stringResource(R.string.follow_system),
                    onClick = { showLanguageDialog = true },
                    shape = BottomRoundedShape,
                    itemIcon = Icons.Default.Translate
                )
            }

            item { SectionTitle("数据管理") }
            item {
                SettingsClickableItem(
                    title = "清除应用缓存",
                    value = uiState.cacheSize,
                    onClick = { viewModel.clearCache() },
                    shape = AllRoundedShape,
                    itemIcon = Icons.Default.Storage
                )
            }

            item { SectionTitle("关于与反馈") }
            item {
                SettingsClickableItem(
                    title = "查看本地日志",
                    onClick = { viewModel.viewLocalLogs() },
                    shape = TopRoundedShape,
                    showDivider = true,
                    itemIcon = Icons.AutoMirrored.Filled.List
                )
            }
            item {
                SettingsClickableItem(
                    title = "导出本地日志",
                    onClick = { viewModel.exportLogs() },
                    shape = MiddleShape,
                    showDivider = true,
                    itemIcon = Icons.Default.BugReport
                )
            }
            item {
                SettingsClickableItem(
                    title = "查看更新日志",
                    onClick = { viewModel.fetchLog(changelogUrl) },
                    shape = MiddleShape,
                    showDivider = true,
                    itemIcon = Icons.Default.History
                )
            }
            item {
                SettingsClickableItem(
                    title = "检查更新",
                    onClick = { },
                    shape = MiddleShape,
                    showDivider = true,
                    itemIcon = Icons.Default.Update
                )
            }
            item {
                SettingsClickableItem(
                    title = "版本号",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = {
                        versionTapCount++
                        if (versionTapCount >= 3) {
                            if (Screen.isVaultAvailable()) {
                                val intent = Intent().apply {
                                    setClassName(context.packageName, "com.example.poop.ui.screens.vault.VaultActivity")
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "当前版本不支持保险箱", Toast.LENGTH_SHORT).show()
                            }
                            versionTapCount = 0
                        }
                    },
                    shape = BottomRoundedShape,
                    itemIcon = Icons.Default.Info,
                    showArrow = false
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}