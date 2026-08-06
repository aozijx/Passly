package com.aozijx.passly.feature.vault.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VaultDisplayViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<VaultDisplayUiState> = settingsRepository.settings
        .map { settings ->
            VaultDisplayUiState(
                layout = VaultLayoutConfig(
                    hideSystemBars = settings.interfacePrefs.hideSystemBars,
                    collapseTopBarOnScroll = settings.interfacePrefs.collapseTopBarOnScroll,
                    collapseTabBarOnScroll = settings.interfacePrefs.collapseTabBarOnScroll,
                    tabBarMaxTabsWithoutScroll = settings.vault.maxTabsWithoutScroll
                ),
                style = VaultStyleConfig(
                    entryCardPresentations = settings.vault.entryCardPresentations,
                    entryHierarchyDisplayMode = settings.vault.entryHierarchyDisplayMode
                ),
                interaction = VaultInteractionConfig(
                    isSwipeEnabled = settings.interaction.isSwipeEnabled,
                    swipeLeftAction = settings.interaction.swipeLeftAction,
                    swipeRightAction = settings.interaction.swipeRightAction
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
