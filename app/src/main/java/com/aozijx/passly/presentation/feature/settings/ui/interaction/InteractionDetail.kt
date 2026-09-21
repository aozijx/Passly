package com.aozijx.passly.presentation.feature.settings.ui.interaction

import androidx.compose.runtime.Composable
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.shared.gesture.SwipeActionUiModel

internal data class InteractionDetailUiModel(
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionUiModel,
    val swipeRightAction: SwipeActionUiModel,
)

@Composable
internal fun InteractionDetail(
    state: InteractionDetailUiModel,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit,
) {
    SettingsSection {
        SwipeGestureSettingsSection(
            isSwipeEnabled = state.isSwipeEnabled,
            swipeLeftAction = state.swipeLeftAction,
            swipeRightAction = state.swipeRightAction,
            onSwipeEnabledChange = onSwipeEnabledChange,
            onLeftSwipeActionClick = onLeftSwipeActionClick,
            onRightSwipeActionClick = onRightSwipeActionClick
        )
    }
}
