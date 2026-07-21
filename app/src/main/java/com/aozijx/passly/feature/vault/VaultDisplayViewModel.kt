package com.aozijx.passly.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.model.settings.SwipeActionType
import com.aozijx.passly.domain.model.settings.VaultCardStyle
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 彻底优化的数据结构 ---

data class VaultLayoutConfig(
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
)

data class VaultStyleConfig(
    val cardStyle: VaultCardStyle = VaultCardStyle.DEFAULT,
    val perTypeMap: Map<Int, VaultCardStyle> = mapOf(-1 to VaultCardStyle.DEFAULT),
)

data class VaultInteractionConfig(
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
)

data class VaultDisplayUiState(
    val layout: VaultLayoutConfig = VaultLayoutConfig(),
    val style: VaultStyleConfig = VaultStyleConfig(),
    val interaction: VaultInteractionConfig = VaultInteractionConfig()
)

@HiltViewModel
class VaultDisplayViewModel @Inject constructor(
    private val portableSettingsUseCases: PortableSettingsUseCases
) : ViewModel() {

    /**
     * 架构优化：
     * 1. 结构化：将零散的状态归类为逻辑对象（Layout/Style/Interaction）。
     * 2. 简洁性：combine 逻辑现在只是简单的对象构造，没有任何复杂的 copy 逻辑。
     * 3. 性能：UI 层可以针对子对象进行重组优化（配合 Compose 的 remember 处理）。
     */
    val config: StateFlow<VaultDisplayUiState> = combine(
        combine(
            portableSettingsUseCases.isStatusBarAutoHide,
            portableSettingsUseCases.isTopBarCollapsible,
            portableSettingsUseCases.isTabBarCollapsible,
            portableSettingsUseCases.visibleVaultTabs,
            portableSettingsUseCases.tabBarMaxTabsWithoutScroll
        ) { autoHide, top, tab, tabs, max ->
            VaultLayoutConfig(autoHide, top, tab, tabs, max)
        },
        combine(
            portableSettingsUseCases.cardStyle,
            portableSettingsUseCases.cardStyleByEntryType
        ) { style, perType ->
            VaultStyleConfig(style, perType)
        },
        combine(
            portableSettingsUseCases.isSwipeEnabled,
            portableSettingsUseCases.swipeLeftAction,
            portableSettingsUseCases.swipeRightAction
        ) { enabled, left, right ->
            VaultInteractionConfig(enabled, left, right)
        }
    ) { layout, style, interaction ->
        VaultDisplayUiState(layout, style, interaction)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        VaultDisplayUiState()
    )

    // --- Mutations ---

    fun setCardStyle(style: VaultCardStyle) {
        viewModelScope.launch {
            portableSettingsUseCases.setCardStyle(style)
        }
    }

    fun setCardStyleForType(type: Int, style: VaultCardStyle) {
        viewModelScope.launch {
            portableSettingsUseCases.setCardStyleForEntryType(type, style)
        }
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            portableSettingsUseCases.setSwipeEnabled(enabled)
        }
    }

    fun setSwipeAction(isLeft: Boolean, action: SwipeActionType) {
        viewModelScope.launch {
            if (isLeft) portableSettingsUseCases.setSwipeLeftAction(action)
            else portableSettingsUseCases.setSwipeRightAction(action)
        }
    }

    fun updateVisibleTabs(tabs: Set<String>) {
        viewModelScope.launch {
            portableSettingsUseCases.setVisibleVaultTabs(tabs)
        }
    }
}
