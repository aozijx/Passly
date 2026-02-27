package com.example.poop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "首页", Icons.Rounded.Home)
    data object Profile : Screen("profile", "我的", Icons.Rounded.Person)
    data object Animation : Screen("animation", "动画", Icons.Rounded.PlayArrow)
    data object Detail : Screen("detail", "详情", Icons.Rounded.Info)
    data object Scanner : Screen("scanner", "扫码", Icons.Rounded.Search)
    data object Setting : Screen("setting", "设置", Icons.Rounded.Settings)
    // SDK 分析路由
    data object AppAnalysis : Screen("app_analysis", "应用分析", Icons.AutoMirrored.Rounded.List)
}
