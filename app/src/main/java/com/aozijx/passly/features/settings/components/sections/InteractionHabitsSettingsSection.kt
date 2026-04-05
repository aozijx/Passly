package com.aozijx.passly.features.settings.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.features.settings.components.common.ClickableSettingItem
import com.aozijx.passly.features.settings.components.common.SettingsCard
import com.aozijx.passly.features.settings.components.common.SettingsGroupTitle
import com.aozijx.passly.features.settings.components.common.SwitchSettingItem

@Composable
fun InteractionHabitsSettingsSection(
    isSwipeEnabled: Boolean,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    autofillUiMode: AutofillUiMode,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit,
    onToggleAutofillUiMode: () -> Unit
) {
    SettingsGroupTitle(text = "交互习惯")
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
                    value = swipeLeftAction.displayName,
                    onClick = onLeftSwipeActionClick
                )
                HorizontalDivider(
                    Modifier.padding(start = 56.dp, end = 16.dp), thickness = 0.5.dp
                )
                ClickableSettingItem(
                    title = "右滑快捷动作",
                    value = swipeRightAction.displayName,
                    onClick = onRightSwipeActionClick
                )
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        ClickableSettingItem(
            icon = Icons.Default.ViewDay,
            title = "自动填充展示",
            value = autofillUiMode.displayName,
            onClick = onToggleAutofillUiMode
        )
    }
}
