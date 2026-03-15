package com.example.poop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val isBottomNav: Boolean = false,
    val showBackIcon: Boolean = !isBottomNav
) {
    data object Home : Screen("home", "首页", Icons.Rounded.Home, isBottomNav = true)
    data object Profile : Screen("profile", "我的", Icons.Rounded.Person, isBottomNav = true)
    data object Vault : Screen("vault", "保险箱", Icons.Rounded.AddCard)
    data object Detail : Screen("detail", "详情", Icons.Rounded.Info, isBottomNav = true)
    data object Scanner : Screen("scanner", "扫码", Icons.Rounded.Search)
    data object Setting : Screen("setting", "设置", Icons.Rounded.Settings)

    // SDK 分析路由
    data object AppAnalysis : Screen("app_analysis", "应用分析", Icons.AutoMirrored.Rounded.List)

    companion object {
        // 使用 getter 避免初始化顺序导致的 null 问题
        val bottomNavItems get() = listOf(Home, Profile, Vault, Detail)

        // 辅助函数：根据 route 找 Screen 对象
        fun fromRoute(route: String?): Screen? = when (route) {
            Home.route -> Home
            Vault.route -> Vault
            Profile.route -> Profile
            Detail.route -> Detail
            Scanner.route -> Scanner
            Setting.route -> Setting
            AppAnalysis.route -> AppAnalysis
            else -> null
        }
    }
}
