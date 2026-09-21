package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.feature.settings.appearance.toInterfaceUiModel
import com.aozijx.passly.presentation.feature.settings.ui.appearance.InterfaceDetail
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.feature.settings.ui.appearance.model.InterfaceEventHandler
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
                eventHandler = object : InterfaceEventHandler {
                    override fun onStatusBarAutoHideChanged(enabled: Boolean) =
                        viewModel.onAction(InterfaceSettingsAction.SetHideSystemBars(enabled))
                    override fun onTopBarCollapsibleChanged(enabled: Boolean) =
                        viewModel.onAction(InterfaceSettingsAction.SetTopBarCollapsible(enabled))
                    override fun onQuickFilterBarCollapsibleChanged(enabled: Boolean) =
                        viewModel.onAction(
                            InterfaceSettingsAction.SetQuickFilterBarCollapsible(enabled),
                        )
                    override fun onAppCornerRadiusChanged(radiusDp: Float) =
                        viewModel.onAction(InterfaceSettingsAction.SetAppCornerRadius(radiusDp))
                    override fun onEntryHierarchyDisplayModeChanged(
                        mode: EntryHierarchyDisplayModeUiModel,
                    ) = viewModel.onAction(
                        InterfaceSettingsAction.SetEntryHierarchyDisplayMode(mode.toDomainModel()),
                    )
                },
            )
        }
    }
}
