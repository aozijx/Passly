package com.aozijx.passly.presentation.feature.settings.ui.interaction

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.shared.gesture.SwipeActionUiModel

internal data class InteractionDetailUiModel(
    val isSwipeEnabled: Boolean,
    val isPullToSearchEnabled: Boolean,
    val swipeLeftAction: SwipeActionUiModel,
    val swipeRightAction: SwipeActionUiModel,
)

@Composable
internal fun InteractionDetail(
    state: InteractionDetailUiModel,
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
            onRightSwipeActionClick = onRightSwipeActionClick
        )
    }
}
