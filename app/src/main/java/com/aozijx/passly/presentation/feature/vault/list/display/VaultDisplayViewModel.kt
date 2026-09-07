package com.aozijx.passly.presentation.feature.vault.list.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VaultDisplayViewModel @Inject constructor(
    interfaceSettingsRepository: InterfaceSettingsRepository,
    librarySettingsSource: LibraryViewSettingsSource,
    interactionSettingsSource: InteractionSettingsSource,
) : ViewModel() {

    val config: StateFlow<VaultDisplayUiState> = combine(
        interfaceSettingsRepository.interfaceSettings,
        librarySettingsSource.libraryViewSettings,
        interactionSettingsSource.interaction,
    ) { interfaceSettings, librarySettings, interactionSettings ->
            VaultDisplayUiState(
                layout = VaultLayoutConfig(
                    hideSystemBars = interfaceSettings.hideSystemBars,
                    collapseTopBarOnScroll = interfaceSettings.collapseTopBarOnScroll,
                    collapseQuickFilterBarOnScroll = interfaceSettings.collapseQuickFilterBarOnScroll
                ),
                style = VaultStyleConfig(
                    entryCardPresentations = librarySettings.entryCardPresentations,
                    entryHierarchyDisplayMode = librarySettings.entryHierarchyDisplayMode
                ),
                interaction = VaultInteractionConfig(
                    isSwipeEnabled = interactionSettings.isSwipeEnabled,
                    swipeLeftAction = interactionSettings.swipeLeftAction,
                    swipeRightAction = interactionSettings.swipeRightAction
                )
            )
        }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            VaultDisplayUiState()
        )
}
