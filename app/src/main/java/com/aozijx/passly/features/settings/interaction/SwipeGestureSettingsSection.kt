package com.aozijx.passly.features.settings.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.features.settings.components.navigationSettingsItem
import com.aozijx.passly.features.settings.components.switchSettingsItem
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SettingsRoundedGroup

@Composable
fun SwipeGestureSettingsSection(
    isSwipeEnabled: Boolean,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit
) {
    SettingsGroupTitle(text = "快捷手势")
    SettingsRoundedGroup {
        switchSettingsItem(
            icon = Icons.Default.Swipe,
            title = "列表快捷手势",
            subtitle = "支持条目左右滑动触发快捷操作",
            checked = isSwipeEnabled,
            onCheckedChange = onSwipeEnabledChange
        )
        navigationSettingsItem(
            visible = isSwipeEnabled,
            title = "左滑快捷动作",
            value = when (swipeLeftAction) {
                SwipeActionType.DELETE -> "删除"
                SwipeActionType.DETAIL -> "详情"
                SwipeActionType.COPY_PASSWORD -> "复制密码"
                SwipeActionType.COPY_USERNAME -> "复制账号"
                SwipeActionType.DISABLED -> "禁用"
            },
            onClick = onLeftSwipeActionClick
        )
        navigationSettingsItem(
            visible = isSwipeEnabled,
            title = "右滑快捷动作",
            value = when (swipeRightAction) {
                SwipeActionType.DELETE -> "删除"
                SwipeActionType.DETAIL -> "详情"
                SwipeActionType.COPY_PASSWORD -> "复制密码"
                SwipeActionType.COPY_USERNAME -> "复制账号"
                SwipeActionType.DISABLED -> "禁用"
            },
            onClick = onRightSwipeActionClick
        )
    }
}