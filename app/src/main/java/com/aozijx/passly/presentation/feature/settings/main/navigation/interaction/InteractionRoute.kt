package com.aozijx.passly.presentation.feature.settings.main.navigation.interaction

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsAction
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.interaction.InteractionDetail
import com.aozijx.passly.presentation.feature.settings.main.interaction.toUiModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InteractionRoute(
    localState: SettingsOverlayState,
    onBack: (() -> Unit)?,
) {
    val viewModel: InteractionSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERACTION.titleRes),
        onBack = onBack
    ) {
        item {
            InteractionDetail(
                state = state.toUiModel(),
                onSwipeEnabledChange = {
                    viewModel.onAction(
                        InteractionSettingsAction.SetSwipeEnabled(it)
                    )
                },
                onLeftSwipeActionClick = localState::openLeftActionDialog,
                onRightSwipeActionClick = localState::openRightActionDialog,
            )
        }
    }
}
