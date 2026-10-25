package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsUiState
import com.aozijx.passly.feature.settings.presentation.appearance.toInterfaceUiModel
import com.aozijx.passly.feature.settings.presentation.ui.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup

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
        when (val readyState = state) {
            InterfaceSettingsUiState.Loading -> Unit
            is InterfaceSettingsUiState.Ready -> item {
                InterfaceDetail(
                    state = readyState.toInterfaceUiModel(),
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}
