package com.aozijx.passly.feature.settings.interaction

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.components.settings.SettingsSection

@Composable
internal fun InteractionDetail(
    state: InteractionUiState,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onLeftSwipeActionClick: () -> Unit,
    onRightSwipeActionClick: () -> Unit,
    onAutofillAction: (InteractionUiAction) -> Unit,
    onOpenAutofillSettings: () -> Unit
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

        Spacer(modifier = Modifier.height(24.dp))

        AutofillSettingsSection(
            settings = state.autofill,
            isSystemServiceEnabled = state.isSystemAutofillEnabled,
            onOpenAutofillSettings = onOpenAutofillSettings,
            onAction = onAutofillAction,
        )
    }
}
