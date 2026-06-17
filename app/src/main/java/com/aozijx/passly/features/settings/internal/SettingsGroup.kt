package com.aozijx.passly.features.settings.internal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class SettingsGroup(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    SECURITY_PRIVACY(
        icon = Icons.Default.Security,
        title = "安全与隐私",
        subtitle = "密码、锁定、生物识别与防窥保护"
    ),
    DISPLAY_APPEARANCE(
        icon = Icons.Default.Palette,
        title = "显示与外观",
        subtitle = "沉浸式体验、主题配色、卡片样式与标签栏"
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