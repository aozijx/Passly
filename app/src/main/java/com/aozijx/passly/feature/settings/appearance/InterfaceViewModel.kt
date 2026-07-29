package com.aozijx.passly.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InterfaceUiState(
    val hideSystemBars: Boolean = false,
    val collapseTopBarOnScroll: Boolean = false,
    val collapseTabBarOnScroll: Boolean = false,
    val outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    val innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    val groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    val groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP,
    val visibleVaultTabs: Set<String>? = null,
    val tabBarMaxTabsWithoutScroll: Int = 4,
    val entryHierarchyDisplayMode: EntryHierarchyDisplayMode =
        EntryHierarchyDisplayMode.COLLAPSED,
)

sealed interface InterfaceUiAction {
    data class SetHideSystemBars(val enabled: Boolean) : InterfaceUiAction
    data class SetTopBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetTabBarCollapsible(val enabled: Boolean) : InterfaceUiAction
    data class SetOuterCornerRadius(val radiusDp: Float) : InterfaceUiAction
    data class SetInnerCornerRadius(val radiusDp: Float) : InterfaceUiAction
    data class SetGroupItemSpacing(val spacingDp: Float) : InterfaceUiAction
    data class SetGroupContentPadding(val paddingDp: Float) : InterfaceUiAction
    data class SetVisibleVaultTabs(val tabs: Set<String>) : InterfaceUiAction
    data class SetMaxTabsWithoutScroll(val maxTabs: Int) : InterfaceUiAction
    data class SetEntryHierarchyDisplayMode(
        val mode: EntryHierarchyDisplayMode
    ) : InterfaceUiAction
}

@HiltViewModel
class InterfaceViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository
) : ViewModel() {

    val config: StateFlow<InterfaceUiState> = settingsRepository.settings
        .map { settings ->
            val prefs = settings.interfacePrefs
            InterfaceUiState(
                hideSystemBars = prefs.hideSystemBars,
                collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
                collapseTabBarOnScroll = prefs.collapseTabBarOnScroll,
                outerCornerRadiusDp = prefs.outerCornerRadiusDp,
                innerCornerRadiusDp = prefs.innerCornerRadiusDp,
                groupItemSpacingDp = prefs.groupItemSpacingDp,
                groupContentPaddingDp = prefs.groupContentPaddingDp,
                visibleVaultTabs = settings.vault.visibleTabs?.tabKeys,
                tabBarMaxTabsWithoutScroll = settings.vault.maxTabsWithoutScroll,
                entryHierarchyDisplayMode = settings.vault.entryHierarchyDisplayMode
            )
        }
        .stateIn(
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

            is InterfaceUiAction.SetOuterCornerRadius -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetOuterCornerRadius(action.radiusDp))
            }

            is InterfaceUiAction.SetInnerCornerRadius -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetInnerCornerRadius(action.radiusDp))
            }

            is InterfaceUiAction.SetGroupItemSpacing -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetGroupItemSpacing(action.spacingDp))
            }

            is InterfaceUiAction.SetGroupContentPadding -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetGroupContentPadding(action.paddingDp))
            }

            is InterfaceUiAction.SetVisibleVaultTabs -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetVisibleVaultTabs(action.tabs))
            }

            is InterfaceUiAction.SetMaxTabsWithoutScroll -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetMaxTabsWithoutScroll(action.maxTabs))
            }

            is InterfaceUiAction.SetEntryHierarchyDisplayMode -> viewModelScope.launch {
                settingsRepository.update(
                    SettingsCommand.SetEntryHierarchyDisplayMode(action.mode)
                )
            }
        }
    }
}
