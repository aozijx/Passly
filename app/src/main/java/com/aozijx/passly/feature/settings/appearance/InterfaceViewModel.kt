package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InterfaceUiState(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
)

sealed interface InterfaceUiAction {
    data class SetHideSystemBars(val enabled: Boolean) : InterfaceUiAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetTabBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetVisibleVaultTabs(val tabs: Set<String>) : InterfaceUiAction
    data class SetMaxTabsWithoutScroll(val maxTabs: Int) : InterfaceUiAction
}

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<InterfaceUiState> = combine(
        settingsRepository.settings.map { it.interfacePrefs.hideSystemBars },
        settingsRepository.settings.map { it.interfacePrefs.collapseTopBarOnScroll },
        settingsRepository.settings.map { it.interfacePrefs.collapseTabBarOnScroll }
    ) { hsb, ctp, ctb ->
        Triple(hsb, ctp, ctb)
    }.combine(settingsRepository.settings.map { it.vault.visibleTabs }) { (hsb, ctp, ctb), vt ->
        InterfaceUiState(
            hideSystemBars = hsb,
            collapseTopBarOnScroll = ctp,
            collapseTabBarOnScroll = ctb,
            visibleVaultTabs = vt?.tabKeys,
        )
    }
        .combine(settingsRepository.settings.map { it.vault.maxTabsWithoutScroll }) { st, tbm ->
        st.copy(tabBarMaxTabsWithoutScroll = tbm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        InterfaceUiState()
    )

    fun onAction(action: InterfaceUiAction) {
        when (action) {
            is InterfaceUiAction.SetHideSystemBars -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetHideSystemBars(action.enabled))
            }

            is InterfaceUiAction.SetTopBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTopBarCollapsible(action.enabled))
            }

            is InterfaceUiAction.SetTabBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTabBarCollapsible(action.enabled))
            }

            is InterfaceUiAction.SetVisibleVaultTabs -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetVisibleVaultTabs(action.tabs))
            }

            is InterfaceUiAction.SetMaxTabsWithoutScroll -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetMaxTabsWithoutScroll(action.maxTabs))
            }
        }
    }
}
