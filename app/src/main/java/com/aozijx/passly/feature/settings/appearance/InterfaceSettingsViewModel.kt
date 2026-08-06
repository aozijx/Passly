package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.vault.model.VaultTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<InterfaceSettingsUiState> = settingsRepository.settings
        .map { settings ->
            val prefs = settings.interfacePrefs
            InterfaceSettingsUiState(
                hideSystemBars = prefs.hideSystemBars,
                collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
                collapseTabBarOnScroll = prefs.collapseTabBarOnScroll,
                outerCornerRadiusDp = prefs.outerCornerRadiusDp,
                innerCornerRadiusDp = prefs.innerCornerRadiusDp,
                groupItemSpacingDp = prefs.groupItemSpacingDp,
                groupContentPaddingDp = prefs.groupContentPaddingDp,
                enabledVaultTabKeys =
                    settings.vault.visibleTabs?.tabKeys ?: VaultTab.defaultVisibleKeys,
                entryHierarchyDisplayMode = settings.vault.entryHierarchyDisplayMode
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InterfaceSettingsUiState()
        )

    fun onAction(action: InterfaceSettingsAction) {
        when (action) {
            is InterfaceSettingsAction.SetHideSystemBars -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetHideSystemBars(action.enabled))
            }

            is InterfaceSettingsAction.SetTopBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTopBarCollapsible(action.enabled))
            }

            is InterfaceSettingsAction.SetTabBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTabBarCollapsible(action.enabled))
            }

            is InterfaceSettingsAction.SetOuterCornerRadius -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetOuterCornerRadius(action.radiusDp))
            }

            is InterfaceSettingsAction.SetInnerCornerRadius -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetInnerCornerRadius(action.radiusDp))
            }

            is InterfaceSettingsAction.SetGroupItemSpacing -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetGroupItemSpacing(action.spacingDp))
            }

            is InterfaceSettingsAction.SetGroupContentPadding -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetGroupContentPadding(action.paddingDp))
            }

            is InterfaceSettingsAction.ToggleVisibleVaultTab -> viewModelScope.launch {
                val nextKeys = VaultTab.toggleVisibleKey(
                    enabledKeys = config.value.enabledVaultTabKeys,
                    tab = action.tab
                )
                settingsRepository.update(SettingsCommand.SetVisibleVaultTabs(nextKeys))
            }

            is InterfaceSettingsAction.SetEntryHierarchyDisplayMode -> viewModelScope.launch {
                settingsRepository.update(
                    SettingsCommand.SetEntryHierarchyDisplayMode(action.mode)
                )
            }
        }
    }
}
