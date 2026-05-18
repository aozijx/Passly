package com.aozijx.passly.features.settings.components.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aozijx.passly.features.settings.SettingsContentActions
import com.aozijx.passly.features.settings.SettingsContentState
import com.aozijx.passly.features.settings.components.sections.InteractionHabitsSettingsSection

@Composable
internal fun InteractionDetail(
    state: SettingsContentState,
    actions: SettingsContentActions
) {
    Column(modifier = Modifier.sectionSpacing()) {
        Spacer(modifier = Modifier.height(8.dp))

        InteractionHabitsSettingsSection(
            isSwipeEnabled = state.isSwipeEnabled,
            swipeLeftAction = state.swipeLeftAction,
            swipeRightAction = state.swipeRightAction,
            autofillUiMode = state.autofillUiMode,
            onSwipeEnabledChange = actions.onSwipeEnabledChange,
            onLeftSwipeActionClick = actions.onLeftSwipeActionClick,
            onRightSwipeActionClick = actions.onRightSwipeActionClick,
            onToggleAutofillUiMode = actions.onToggleAutofillUiMode
        )
    }
}