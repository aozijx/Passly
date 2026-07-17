package com.aozijx.passly.feature.settings.internal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.aozijx.passly.feature.settings.navigation.SettingsRoute

internal enum class SettingsGroup(
    val sectionTitle: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: SettingsRoute
) {
    APPEARANCE(
        sectionTitle = "显示与外观",
        icon = Icons.Default.Palette,
        title = "外观设置",
        subtitle = "主题配色、字体语言与动态取色",
        route = SettingsRoute.Appearance
    ),
    INTERFACE(
        sectionTitle = "显示与外观",
        icon = Icons.Default.SpaceDashboard,
        title = "界面设置",
        subtitle = "沉浸式体验、状态栏与标签栏",
        route = SettingsRoute.Interface
    ),
    SECURITY(
        sectionTitle = "安全与隐私",
        icon = Icons.Default.Lock,
        title = "安全设置",
        subtitle = "自动锁定、密码与生物识别",
        route = SettingsRoute.Security
    ),
    PRIVACY(
        sectionTitle = "安全与隐私",
        icon = Icons.Default.Visibility,
        title = "隐私设置",
        subtitle = "防窥保护、翻转锁定与内容隐藏",
        route = SettingsRoute.Privacy
    ),
    INTERACTION(
        sectionTitle = "交互与操作",
        icon = Icons.Default.TouchApp,
        title = "交互与操作",
        subtitle = "手势控制与自动填充",
        route = SettingsRoute.Interaction
    ),
    DATA_MANAGEMENT(
        sectionTitle = "备份与恢复",
        icon = Icons.Default.Storage,
        title = "备份与恢复",
        subtitle = "备份与恢复",
        route = SettingsRoute.DataManagement
    ),
    RECOVERY_CODE(
        sectionTitle = "备份与恢复",
        icon = Icons.Default.Key,
        title = "恢复码",
        subtitle = "用于账户恢复，仅生成一次",
        route = SettingsRoute.RecoveryCode
    ),
    GENERAL(
        sectionTitle = "通用",
        icon = Icons.Default.Info,
        title = "通用",
        subtitle = "关于、缓存与应用信息",
        route = SettingsRoute.General
    )
}