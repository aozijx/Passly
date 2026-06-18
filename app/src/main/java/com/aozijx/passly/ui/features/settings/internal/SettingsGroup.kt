package com.aozijx.passly.ui.features.settings.internal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class SettingsGroup(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SECURITY(
        icon = Icons.Default.Lock,
        title = "安全设置",
        subtitle = "自动锁定、密码与生物识别"
    ),
    PRIVACY(
        icon = Icons.Default.Visibility,
        title = "隐私设置",
        subtitle = "防窥保护、翻转锁定与内容隐藏"
    ),
    APPEARANCE(
        icon = Icons.Default.Palette,
        title = "外观设置",
        subtitle = "主题配色、字体语言与动态取色"
    ),
    INTERFACE(
        icon = Icons.Default.SpaceDashboard,
        title = "界面设置",
        subtitle = "沉浸式体验、状态栏与标签栏"
    ),
    INTERACTION(
        icon = Icons.Default.TouchApp,
        title = "交互与操作",
        subtitle = "手势控制与自动填充"
    ),
    DATA_MANAGEMENT(
        icon = Icons.Default.Storage,
        title = "数据管理",
        subtitle = "自动下载与备份恢复"
    ),
    GENERAL(
        icon = Icons.Default.Info,
        title = "通用",
        subtitle = "关于、缓存与应用信息"
    )
}