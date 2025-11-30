package com.example.poop.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.poop.MainActivity

// 底部导航项数据类
data class BottomNavItem(
    val title: String,
    val icon: ImageVector? = null, // 使用资源ID，方便自定义图标
    val activityClass: Class<*>? = null, // 要跳转的Activity类
    val currentActivityClass: Class<*> // 当前Activity类（用于选中判断）
)

// 定义底部导航项
val navItems = listOf(
    BottomNavItem(
        title = "首页",
        icon = Icons.Rounded.Home,
        activityClass = MainActivity::class.java,       // 点击跳转到首页
        currentActivityClass = MainActivity::class.java // 首页Activity对应这个项
    ),
    BottomNavItem(
        title = "详情",
        icon = Icons.Rounded.Info,
        activityClass = com.example.poop.ui.screens.detail.DetailActivity::class.java,     // 点击跳转到详情页
        currentActivityClass = com.example.poop.ui.screens.detail.DetailActivity::class.java // 详情页Activity对应这个项
    ),
    BottomNavItem(
        title = "我的",
        icon = Icons.Rounded.Person,
        activityClass = com.example.poop.ui.screens.profile.ProfileActivity::class.java, // 跳转到个人资料页
        currentActivityClass = com.example.poop.ui.screens.profile.ProfileActivity::class.java // 个人资料页Activity对应这个项
    )
)