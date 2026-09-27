package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.toAction
import com.aozijx.passly.presentation.feature.settings.appearance.toInterfaceUiModel
import com.aozijx.passly.presentation.feature.settings.ui.appearance.InterfaceDetail
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup

@Composable
internal fun InterfaceRoute(
    onBack: (() -> Unit)?
) {
    val viewModel: InterfaceSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERFACE.titleRes),
        onBack = onBack
    ) {
        item {
            InterfaceDetail(
                state = state.toInterfaceUiModel(),
                onEvent = { event ->
                    viewModel.onAction(event.toAction())
                },
            )
        }
    }
}
