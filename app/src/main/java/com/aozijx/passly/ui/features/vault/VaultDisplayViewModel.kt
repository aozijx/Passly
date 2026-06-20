package com.aozijx.passly.ui.features.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.AppDefaults
import com.aozijx.passly.domain.model.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VaultDisplayUiState(
    val isStatusBarAutoHide: Boolean = AppDefaults.DISPLAY_STATUS_BAR_AUTO_HIDE,
    val isTopBarCollapsible: Boolean = AppDefaults.DISPLAY_TOP_BAR_COLLAPSIBLE,
    val isTabBarCollapsible: Boolean = AppDefaults.DISPLAY_TAB_BAR_COLLAPSIBLE,
    val cardStyle: VaultCardStyle = AppDefaults.CardStyle.GLOBAL_DEFAULT_STYLE,
    val perTypeMap: Map<Int, VaultCardStyle> = mapOf(-1 to AppDefaults.CardStyle.GLOBAL_DEFAULT_STYLE),
    val isSwipeEnabled: Boolean = AppDefaults.VAULT_SWIPE_ENABLED,
    val swipeLeftAction: SwipeActionType = AppDefaults.VAULT_SWIPE_LEFT_ACTION,
    val swipeRightAction: SwipeActionType = AppDefaults.VAULT_SWIPE_RIGHT_ACTION,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = AppDefaults.VAULT_TAB_BAR_MAX_TABS_WITHOUT_SCROLL,
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