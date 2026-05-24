package com.aozijx.passly.features.settings.interaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.features.settings.shell.ClickableSettingItem
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SwitchSettingItem

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
    SettingsCard {
        SwitchSettingItem(
            icon = Icons.Default.Swipe,
            title = "列表快捷手势",
            subtitle = "支持条目左右滑动触发快捷操作",
            checked = isSwipeEnabled,
            onCheckedChange = onSwipeEnabledChange
        )

        AnimatedVisibility(
            visible = isSwipeEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                HorizontalDivider(
                    Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp
                )
                ClickableSettingItem(
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
                HorizontalDivider(
                    Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp
                )
                ClickableSettingItem(
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
    }
}