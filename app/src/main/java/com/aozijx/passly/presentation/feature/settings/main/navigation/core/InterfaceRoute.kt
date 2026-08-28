package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsAction
import com.aozijx.passly.presentation.feature.settings.appearance.InterfaceSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.appearance.toInterfaceUiModel
import com.aozijx.passly.presentation.feature.settings.appearance.libraryQuickFilterOptions
import com.aozijx.passly.presentation.feature.settings.appearance.toDomainModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.ui.settings.appearance.InterfaceDetail
import com.aozijx.passly.presentation.ui.settings.appearance.model.EntryHierarchyDisplayModeUiModel
import com.aozijx.passly.presentation.ui.settings.appearance.model.InterfaceEventHandler
import com.aozijx.passly.presentation.ui.settings.appearance.LibraryQuickFiltersSettingsSection

@Composable
internal fun InterfaceRouteContent(
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?
) {
    val viewModel: InterfaceSettingsViewModel = hiltViewModel()
    val state by viewModel.config.collectAsStateWithLifecycle()

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
                    override fun onOuterCornerRadiusChanged(radius: Float) =
                        viewModel.onAction(InterfaceSettingsAction.SetOuterCornerRadius(radius))
                    override fun onInnerCornerRadiusChanged(radius: Float) =
                        viewModel.onAction(InterfaceSettingsAction.SetInnerCornerRadius(radius))
                    override fun onGroupItemSpacingChanged(spacing: Float) =
                        viewModel.onAction(InterfaceSettingsAction.SetGroupItemSpacing(spacing))
                    override fun onGroupContentPaddingChanged(padding: Float) =
                        viewModel.onAction(InterfaceSettingsAction.SetGroupContentPadding(padding))
                    override fun onEntryHierarchyDisplayModeChanged(
                        mode: EntryHierarchyDisplayModeUiModel,
                    ) = viewModel.onAction(
                        InterfaceSettingsAction.SetEntryHierarchyDisplayMode(mode.toDomainModel()),
                    )
                },
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            LibraryQuickFiltersSettingsSection(
                options = libraryQuickFilterOptions(
                    state.enabledLibraryQuickFilterKeys
                ),
                onLibraryQuickFilterToggle = {
                    viewModel.onAction(
                        InterfaceSettingsAction.ToggleVisibleLibraryQuickFilter(
                            it.toDomainModel()
                        )
                    )
                }
            )
        }
    }
}
