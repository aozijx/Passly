package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsAction
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.appearance.InterfaceUiState
import com.aozijx.passly.feature.settings.presentation.common.ObserveSettingsWriteErrors
import com.aozijx.passly.feature.settings.presentation.ui.appearance.InterfaceDetail
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent

@Composable
internal fun InterfaceRoute(onBack: (() -> Unit)?) {
    val viewModel: InterfaceSettingsViewModel = hiltViewModel()
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        onDispose { viewModel.onPageExited(owner) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentReadState = state
    ObserveSettingsWriteErrors(viewModel.errors)

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.INTERFACE.titleRes),
        onBack = onBack,
    ) {
        when (currentReadState) {
            RecoverableReadState.Loading -> {
                Unit
            }

            is RecoverableReadState.Failed -> {
                item { SettingsReadFailureContent(onRetry = { viewModel.onAction(InterfaceSettingsAction.RetryRead) }) }
            }

            is RecoverableReadState.Ready<InterfaceUiState> -> {
                item {
                    InterfaceDetail(
                        state = currentReadState.value,
                        onAction = viewModel::onAction,
                    )
                }
            }
        }
    }
}
