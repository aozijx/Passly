package com.aozijx.passly.ui.features.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VaultDisplayUiState(
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val cardStyle: VaultCardStyle = VaultCardStyle.DEFAULT,
    val perTypeMap: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.DEFAULT),
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
)

@HiltViewModel
class VaultDisplayViewModel @Inject constructor(
    private val systemSettingsUseCases: SystemSettingsUseCases
) : ViewModel() {

    val config: StateFlow<VaultDisplayUiState> = combine(
        systemSettingsUseCases.isStatusBarAutoHide,
        systemSettingsUseCases.isTopBarCollapsible,
        systemSettingsUseCases.isTabBarCollapsible
    ) { sb, tb, tbb ->
        Triple(sb, tb, tbb)
    }.combine(systemSettingsUseCases.cardStyle) { (sb, tb, tbb), cs ->
        VaultDisplayUiState(
            isStatusBarAutoHide = sb,
            isTopBarCollapsible = tb,
            isTabBarCollapsible = tbb,
            cardStyle = cs,
        )
    }.combine(systemSettingsUseCases.cardStyleByEntryType) { st, ptm ->
        st.copy(perTypeMap = ptm)
    }.combine(systemSettingsUseCases.isSwipeEnabled) { st, se ->
        st.copy(isSwipeEnabled = se)
    }.combine(systemSettingsUseCases.swipeLeftAction) { st, sl ->
        st.copy(swipeLeftAction = sl)
    }.combine(systemSettingsUseCases.swipeRightAction) { st, sr ->
        st.copy(swipeRightAction = sr)
    }.combine(systemSettingsUseCases.visibleVaultTabs) { st, vvt ->
        st.copy(visibleVaultTabs = vvt)
    }.combine(systemSettingsUseCases.tabBarMaxTabsWithoutScroll) { st, tbm ->
        st.copy(tabBarMaxTabsWithoutScroll = tbm)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        VaultDisplayUiState()
    )
}