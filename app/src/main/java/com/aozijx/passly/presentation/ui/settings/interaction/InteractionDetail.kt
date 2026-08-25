package com.aozijx.passly.presentation.ui.settings.interaction

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.presentation.ui.vault.list.model.VaultSwipeActionUiModel

internal data class InteractionDetailUiModel(
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: VaultSwipeActionUiModel,
    val swipeRightAction: VaultSwipeActionUiModel,
)

@Composable
internal fun InteractionDetail(
    state: InteractionDetailUiModel,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit,
) {
    SettingsSection {
        Spacer(modifier = Modifier.height(8.dp))

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
