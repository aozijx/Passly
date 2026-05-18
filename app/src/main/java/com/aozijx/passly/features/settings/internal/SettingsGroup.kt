package com.aozijx.passly.features.settings.internal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class SettingsGroup(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    SECURITY_PRIVACY(
        title = "安全与隐私",
        subtitle = "密码、锁定、生物识别与防窥保护",
        icon = Icons.Default.Security,
        accentColor = Color(0xFFE53935)
    ),
    DISPLAY_APPEARANCE(
        title = "显示与外观",
        subtitle = "沉浸式体验、主题配色、卡片样式与标签栏",
        icon = Icons.Default.Palette,
        accentColor = Color(0xFF7C3AED)
    ),
    INTERACTION(
        title = "交互与操作",
        subtitle = "手势控制与自动填充",
        icon = Icons.Default.TouchApp,
        accentColor = Color(0xFF1E88E5)
    ),
    DATA_MANAGEMENT(
        title = "数据管理",
        subtitle = "自动下载与备份恢复",
        icon = Icons.Default.Storage,
        accentColor = Color(0xFF43A047)
    )
}