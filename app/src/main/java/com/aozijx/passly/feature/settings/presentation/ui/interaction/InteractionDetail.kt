package com.aozijx.passly.feature.settings.presentation.ui.interaction

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.domain.settings.model.InteractionSettings

@Composable
internal fun InteractionDetail(
    state: InteractionSettings,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onPullToSearchEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit,
) {
    SettingsSection {
        QuickGestureSettingsSection(
            isSwipeEnabled = state.isSwipeEnabled,
            isPullToSearchEnabled = state.isPullToSearchEnabled,
            swipeLeftAction = state.swipeLeftAction,
            swipeRightAction = state.swipeRightAction,
            onSwipeEnabledChange = onSwipeEnabledChange,
            onPullToSearchEnabledChange = onPullToSearchEnabledChange,
            onLeftSwipeActionClick = onLeftSwipeActionClick,
            onRightSwipeActionClick = onRightSwipeActionClick,
        )
    }
}
