package com.aozijx.passly.feature.settings.presentation.ui.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.components.group.SegmentedSettingsGroup
import com.aozijx.passly.presentation.shared.components.group.navigationSettingsGroupItem
import com.aozijx.passly.presentation.shared.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.domain.settings.model.SwipeActionType

@Composable
internal fun QuickGestureSettingsSection(
    isSwipeEnabled: Boolean,
    isPullToSearchEnabled: Boolean,
    swipeLeftAction: SwipeActionType,
    swipeRightAction: SwipeActionType,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onPullToSearchEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_swipe_gesture_section))
    SegmentedSettingsGroup(
        items = listOf(
            switchSettingsGroupItem(
                key = "interaction.swipe_enabled",
                icon = Icons.Default.Swipe,
                title = stringResource(R.string.settings_swipe_gesture_enabled),
                subtitle = stringResource(R.string.settings_swipe_gesture_enabled_description),
                checked = isSwipeEnabled,
                onCheckedChange = onSwipeEnabledChange
            ),
            navigationSettingsGroupItem(
                key = "interaction.swipe_left",
                visible = isSwipeEnabled,
                reserveLeadingIconSpace = true,
                title = stringResource(R.string.settings_swipe_gesture_left_action),
                value = swipeLeftAction.localizedLabel(),
                onClick = onLeftSwipeActionClick
            ),
            navigationSettingsGroupItem(
                key = "interaction.swipe_right",
                visible = isSwipeEnabled,
                reserveLeadingIconSpace = true,
                title = stringResource(R.string.settings_swipe_gesture_right_action),
                value = swipeRightAction.localizedLabel(),
                onClick = onRightSwipeActionClick
            ),
            switchSettingsGroupItem(
                key = "interaction.pull_to_search_enabled",
                icon = Icons.Default.Search,
                title = stringResource(R.string.settings_pull_to_search_enabled),
                subtitle = stringResource(R.string.settings_pull_to_search_enabled_description),
                checked = isPullToSearchEnabled,
                onCheckedChange = onPullToSearchEnabledChange,
            )
        )
    )
}

@Composable
internal fun SwipeActionType.localizedLabel(): String = stringResource(
    when (this) {
        SwipeActionType.DISABLED -> R.string.settings_swipe_action_disabled
        SwipeActionType.DELETE -> R.string.settings_swipe_action_delete
        SwipeActionType.COPY_PASSWORD -> R.string.settings_swipe_action_copy_password
        SwipeActionType.COPY_USERNAME -> R.string.settings_swipe_action_copy_username
    }
)
