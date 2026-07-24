package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.VaultCardStyle
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
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val cardStyle: VaultCardStyle = VaultCardStyle.DEFAULT,
    val perTypeMap: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.DEFAULT),
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
)

sealed interface InterfaceUiAction {
    data class SetStatusBarAutoHide(val enabled: Boolean) : InterfaceUiAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetTabBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetLoginCardStyle(val style: VaultCardStyle) : InterfaceUiAction
    data class SetVisibleVaultTabs(val tabs: Set<String>) : InterfaceUiAction
    data class SetTabBarMaxTabsWithoutScroll(val maxTabs: Int) : InterfaceUiAction
}

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<InterfaceUiState> = combine(
        settingsRepository.settings.map { it.appearance.isStatusBarAutoHide },
        settingsRepository.settings.map { it.appearance.isTopBarCollapsible },
        settingsRepository.settings.map { it.appearance.isTabBarCollapsible }
    ) { sb, tb, tbb ->
        Triple(sb, tb, tbb)
    }.combine(settingsRepository.settings.map { it.vault.cardStyle }) { (sb, tb, tbb), cs ->
        InterfaceUiState(
            isStatusBarAutoHide = sb,
            isTopBarCollapsible = tb,
            isTabBarCollapsible = tbb,
            cardStyle = cs,
        )
    }.combine(settingsRepository.settings.map { it.vault.cardStyleByEntryType }) { st, ptm ->
        st.copy(perTypeMap = ptm)
    }.combine(settingsRepository.settings.map { it.vault.visibleVaultTabs }) { st, vvt ->
        st.copy(visibleVaultTabs = vvt)
    }
        .combine(settingsRepository.settings.map { it.interaction.tabBarMaxTabsWithoutScroll }) { st, tbm ->
        st.copy(tabBarMaxTabsWithoutScroll = tbm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        InterfaceUiState()
    )

    fun onAction(action: InterfaceUiAction) {
        when (action) {
            is InterfaceUiAction.SetStatusBarAutoHide -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetStatusBarAutoHide(action.enabled))
            }

            is InterfaceUiAction.SetTopBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTopBarCollapsible(action.enabled))
            }

            is InterfaceUiAction.SetTabBarCollapsible -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTabBarCollapsible(action.enabled))
            }

            is InterfaceUiAction.SetLoginCardStyle -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetCardStyleForEntryType(0, action.style))
            }

            is InterfaceUiAction.SetVisibleVaultTabs -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetVisibleVaultTabs(action.tabs))
            }

            is InterfaceUiAction.SetTabBarMaxTabsWithoutScroll -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetTabBarMaxTabsWithoutScroll(action.maxTabs))
            }
        }
    }
}
