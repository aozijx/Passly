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
    data object Vault : Screen("vault", "保险箱", Icons.Rounded.AddCard, isBottomNav = true) // 修改为 true 以在导航栏显示
    data object Detail : Screen("detail", "详情", Icons.Rounded.Info, isBottomNav = true)
    data object Scanner : Screen("scanner", "扫码", Icons.Rounded.Search)
    data object Setting : Screen("setting", "设置", Icons.Rounded.Settings)

    // SDK 分析路由
    data object AppAnalysis : Screen("app_analysis", "应用分析", Icons.AutoMirrored.Rounded.List)

    companion object {
        private const val VAULT_ACTIVITY_CLASS = "com.example.poop.ui.screens.vault.VaultActivity"

        /**
         * 动态获取底部导航项：如果 Vault 功能存在，则将其加入
         */
        val bottomNavItems: List<Screen> get() = buildList {
            add(Home)
            if (isVaultAvailable()) add(Vault)
            add(Profile)
            add(Detail)
        }

        /**
         * 检查当前包中是否存在 VaultActivity
         */
        fun isVaultAvailable(): Boolean = try {
            Class.forName(VAULT_ACTIVITY_CLASS)
            true
        } catch (_: Exception) {
            false
        }

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
