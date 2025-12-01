package com.example.poop.ui.component

import android.content.Intent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.poop.ui.component.navigation.BottomNavItem

// 底部导航栏组件
@Composable
fun SimpleBottomBar(
    items: List<BottomNavItem>,
    activityClass: Class<*>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavigationBar(
        modifier = modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            // 判断是否选中：当前Activity类 == 导航项的currentActivityClass
            val isSelected = item.activityClass == activityClass

            NavigationBarItem(
                icon = {
                    item.icon?.let {
                        Icon(
                            it,
                            contentDescription = item.title,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
//                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    // 避免重复点击当前页面
                    if (!isSelected) {
                        val intent = Intent(context, item.activityClass).apply {
                            // 加Flag防止重复创建
                            flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}