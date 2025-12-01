package com.example.poop.ui.component.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.poop.MainActivity
import com.example.poop.ui.screens.animation.AnimationActivity
import com.example.poop.ui.screens.detail.DetailActivity
import com.example.poop.ui.screens.profile.ProfileActivity

// 底部导航项数据类
data class BottomNavItem(
    val title: String,
    val icon: ImageVector? = null, // 使用资源ID，方便自定义图标
    val activityClass: Class<*>, // 要跳转的Activity类
)

// 定义底部导航项
val navItems = listOf(
    BottomNavItem(
        title = "首页",
        icon = Icons.Rounded.Home,
        activityClass = MainActivity::class.java,       // 点击跳转到首页
    ),
    BottomNavItem(
        title = "详情",
        icon = Icons.Rounded.Info,
        activityClass = DetailActivity::class.java,     // 点击跳转到详情页
    ),
    BottomNavItem(
        title = "我的",
        icon = Icons.Rounded.Person,
        activityClass = ProfileActivity::class.java, // 跳转到个人资料页
    ),
    BottomNavItem(
        title = "动画",
        icon = Icons.Rounded.PlayArrow,
        activityClass = AnimationActivity::class.java, // 跳转到动画页
    )
)
