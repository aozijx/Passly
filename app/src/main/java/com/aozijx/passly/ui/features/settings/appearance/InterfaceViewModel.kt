package com.aozijx.passly.ui.features.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    data class SetPasswordCardStyle(val style: VaultCardStyle) : InterfaceUiAction
    data class SetTotpCardStyle(val style: VaultCardStyle) : InterfaceUiAction
    data class SetVisibleVaultTabs(val tabs: Set<String>) : InterfaceUiAction
    data class SetTabBarMaxTabsWithoutScroll(val maxTabs: Int) : InterfaceUiAction
}

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<InterfaceUiState> = combine(
        systemSettingsUseCases.isStatusBarAutoHide,
        systemSettingsUseCases.isTopBarCollapsible,
        systemSettingsUseCases.isTabBarCollapsible
    ) { sb, tb, tbb ->
        Triple(sb, tb, tbb)
    }.combine(systemSettingsUseCases.cardStyle) { (sb, tb, tbb), cs ->
        InterfaceUiState(
            isStatusBarAutoHide = sb,
            isTopBarCollapsible = tb,
            isTabBarCollapsible = tbb,
            cardStyle = cs,
        )
    }.combine(systemSettingsUseCases.cardStyleByEntryType) { st, ptm ->
        st.copy(perTypeMap = ptm)
    }.combine(systemSettingsUseCases.visibleVaultTabs) { st, vvt ->
        st.copy(visibleVaultTabs = vvt)
    }.combine(systemSettingsUseCases.tabBarMaxTabsWithoutScroll) { st, tbm ->
        st.copy(tabBarMaxTabsWithoutScroll = tbm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        InterfaceUiState()
    )

    fun onAction(action: InterfaceUiAction) {
        when (action) {
            is InterfaceUiAction.SetStatusBarAutoHide -> viewModelScope.launch {
                systemSettingsUseCases.setStatusBarAutoHide(action.enabled)
            }

            is InterfaceUiAction.SetTopBarCollapsible -> viewModelScope.launch {
                systemSettingsUseCases.setTopBarCollapsible(action.enabled)
            }

            is InterfaceUiAction.SetTabBarCollapsible -> viewModelScope.launch {
                systemSettingsUseCases.setTabBarCollapsible(action.enabled)
            }

            is InterfaceUiAction.SetPasswordCardStyle -> viewModelScope.launch {
                systemSettingsUseCases.setCardStyleForEntryType(0, action.style)
            }

            is InterfaceUiAction.SetTotpCardStyle -> viewModelScope.launch {
                systemSettingsUseCases.setCardStyleForEntryType(1, action.style)
            }

            is InterfaceUiAction.SetVisibleVaultTabs -> viewModelScope.launch {
                systemSettingsUseCases.setVisibleVaultTabs(action.tabs)
            }

            is InterfaceUiAction.SetTabBarMaxTabsWithoutScroll -> viewModelScope.launch {
                systemSettingsUseCases.setTabBarMaxTabsWithoutScroll(action.maxTabs)
            }
        }
    }
}