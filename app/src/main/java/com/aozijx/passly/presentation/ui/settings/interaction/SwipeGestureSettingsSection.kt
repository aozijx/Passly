package com.aozijx.passly.presentation.ui.settings.interaction

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.switchSettingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel

@Composable
internal fun SwipeGestureSettingsSection(
    isSwipeEnabled: Boolean,
    swipeLeftAction: VaultSwipeActionUiModel,
    swipeRightAction: VaultSwipeActionUiModel,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit
) {
    SettingsSectionTitle(text = stringResource(R.string.settings_swipe_gesture_section))
    RoundedGroup(
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
                title = stringResource(R.string.settings_swipe_gesture_left_action),
                value = swipeLeftAction.localizedLabel(),
                onClick = onLeftSwipeActionClick
            ),
            navigationSettingsGroupItem(
                key = "interaction.swipe_right",
                visible = isSwipeEnabled,
                title = stringResource(R.string.settings_swipe_gesture_right_action),
                value = swipeRightAction.localizedLabel(),
                onClick = onRightSwipeActionClick
            )
        )
    )
}

@Composable
internal fun VaultSwipeActionUiModel.localizedLabel(): String = stringResource(
    when (this) {
        VaultSwipeActionUiModel.DELETE -> R.string.settings_swipe_action_delete
        VaultSwipeActionUiModel.DETAIL -> R.string.settings_swipe_action_detail
        VaultSwipeActionUiModel.COPY_PASSWORD -> R.string.settings_swipe_action_copy_password
        VaultSwipeActionUiModel.COPY_USERNAME -> R.string.settings_swipe_action_copy_username
    }
)
