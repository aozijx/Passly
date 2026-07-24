package com.aozijx.passly.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.SwipeActionType
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
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    /**
     * 架构优化：
     * 1. 结构化：将零散的状态归类为逻辑对象（Layout/Style/Interaction）。
     * 2. 简洁性：combine 逻辑现在只是简单的对象构造，没有任何复杂的 copy 逻辑。
     * 3. 性能：UI 层可以针对子对象进行重组优化（配合 Compose 的 remember 处理）。
     */
    val config: StateFlow<VaultDisplayUiState> = combine(
        combine(
            settingsRepository.settings.map { it.appearance.isStatusBarAutoHide },
            settingsRepository.settings.map { it.appearance.isTopBarCollapsible },
            settingsRepository.settings.map { it.appearance.isTabBarCollapsible },
            settingsRepository.settings.map { it.vault.visibleVaultTabs },
            settingsRepository.settings.map { it.interaction.tabBarMaxTabsWithoutScroll }
        ) { autoHide, top, tab, tabs, max ->
            VaultLayoutConfig(autoHide, top, tab, tabs, max)
        },
        combine(
            settingsRepository.settings.map { it.vault.cardStyle },
            settingsRepository.settings.map { it.vault.cardStyleByEntryType }
        ) { style, perType ->
            VaultStyleConfig(style, perType)
        },
        combine(
            settingsRepository.settings.map { it.interaction.isSwipeEnabled },
            settingsRepository.settings.map { it.interaction.swipeLeftAction },
            settingsRepository.settings.map { it.interaction.swipeRightAction }
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
            settingsRepository.update(SettingsCommand.SetCardStyle(style))
        }
    }

    fun setCardStyleForType(type: Int, style: VaultCardStyle) {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetCardStyleForEntryType(type, style))
        }
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetSwipeEnabled(enabled))
        }
    }

    fun setSwipeAction(isLeft: Boolean, action: SwipeActionType) {
        viewModelScope.launch {
            if (isLeft) settingsRepository.update(SettingsCommand.SetSwipeLeftAction(action))
            else settingsRepository.update(SettingsCommand.SetSwipeRightAction(action))
        }
    }

    fun updateVisibleTabs(tabs: Set<String>) {
        viewModelScope.launch {
            settingsRepository.update(SettingsCommand.SetVisibleVaultTabs(tabs))
        }
    }
}
