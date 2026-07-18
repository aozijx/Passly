package com.aozijx.passly.feature.settings.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.runtime.Composable
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.ui.components.group.RoundedGroup
import com.aozijx.passly.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.ui.components.settings.SettingsSectionTitle

@Composable
fun SwipeGestureSettingsSection(
    isSwipeEnabled: Boolean,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit
) {
    SettingsSectionTitle(text = "快捷手势")
    RoundedGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "interaction.swipe_enabled",
                icon = Icons.Default.Swipe,
                title = "列表快捷手势",
                subtitle = "支持条目左右滑动触发快捷操作",
                checked = isSwipeEnabled,
                onCheckedChange = onSwipeEnabledChange
            ),
            navigationSettingsGroupItem(
                key = "interaction.swipe_left",
                visible = isSwipeEnabled,
                title = "左滑快捷动作",
                value = swipeLeftAction.label,
                onClick = onLeftSwipeActionClick
            ),
            navigationSettingsGroupItem(
                key = "interaction.swipe_right",
                visible = isSwipeEnabled,
                title = "右滑快捷动作",
                value = swipeRightAction.label,
                onClick = onRightSwipeActionClick
            )
        )
    )
}

private val SwipeActionType.label: String
    get() = when (this) {
        SwipeActionType.DELETE -> "删除"
        SwipeActionType.DETAIL -> "详情"
        SwipeActionType.COPY_PASSWORD -> "复制密码"
        SwipeActionType.COPY_USERNAME -> "复制账号"
        SwipeActionType.DISABLED -> "禁用"
    }
