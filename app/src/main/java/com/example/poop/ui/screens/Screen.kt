package com.example.poop.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null // 导航图标
) {
    data object Home : Screen("home", "首页")
    data object Detail : Screen("detail", "详情页")
}