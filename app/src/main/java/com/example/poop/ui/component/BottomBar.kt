package com.example.poop.ui.component

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.poop.data.BottomNavItem
import com.example.poop.data.navItems

/**
 * 1. 触感反馈：点击时增加 TextHandleMove 类型的震动反馈。
 * 2. 动态样式：选中项文字稍微加粗，未选中项更简洁。
 * 3. 默认参数：简化外部调用，直接从 navItems 获取数据。
 * 4. 导航优化：使用 REORDER_TO_FRONT 保持页面状态。
 */
@Composable
fun SimpleBottomBar(
    activityClass: Class<*>,
    modifier: Modifier = Modifier,
    items: List<BottomNavItem> = navItems
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp, // M3 标准高度，更有层次感
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        items.forEach { item ->
            val isSelected = item.activityClass == activityClass
            
            NavigationBarItem(
                icon = {
                    item.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.title,
                        style = if (isSelected) {
                            MaterialTheme.typography.labelLarge // 选中时文字略大
                        } else {
                            MaterialTheme.typography.labelSmall
                        }
                    )
                },
                selected = isSelected,
                alwaysShowLabel = true,
                onClick = {
                    if (!isSelected) {
                        // 增加震动反馈
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigateToActivity(context, item.activityClass)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            )
        }
    }
}

private fun navigateToActivity(context: Context, targetClass: Class<*>) {
    val intent = Intent(context, targetClass).apply {
        // 使用 REORDER_TO_FRONT：如果 Activity 已存在，将其移至前台而非重新创建
        // 这能更好地模拟多标签切换的流畅感
        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
    }
    
    // 使用简单的渐变动画替代生硬的直接显示
    val options = ActivityOptions.makeCustomAnimation(
        context,
        android.R.anim.fade_in,
        android.R.anim.fade_out
    )
    context.startActivity(intent, options.toBundle())
}
