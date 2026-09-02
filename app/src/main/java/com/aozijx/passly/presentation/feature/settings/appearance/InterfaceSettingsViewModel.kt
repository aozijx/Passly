package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: InterfaceSettingsRepository
) : ViewModel() {

    val uiState: StateFlow<InterfaceSettingsUiState> = settingsRepository.interfaceSettings
        .map { settings ->
            val prefs = settings.preferences
            InterfaceSettingsUiState(
                hideSystemBars = prefs.hideSystemBars,
                collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
                collapseQuickFilterBarOnScroll = prefs.collapseQuickFilterBarOnScroll,
                outerCornerRadiusDp = prefs.outerCornerRadiusDp,
                innerCornerRadiusDp = prefs.innerCornerRadiusDp,
                groupItemSpacingDp = prefs.groupItemSpacingDp,
                groupContentPaddingDp = prefs.groupContentPaddingDp,
                enabledLibraryQuickFilterKeys =
                    settings.visibleLibraryQuickFilterKeys
                        ?: LibraryQuickFilter.defaultVisibleKeys,
                entryHierarchyDisplayMode = settings.entryHierarchyDisplayMode
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
                settingsRepository.setHideSystemBars(action.enabled)
            }

            is InterfaceSettingsAction.SetTopBarCollapsible -> viewModelScope.launch {
                settingsRepository.setTopBarCollapsible(action.enabled)
            }

            is InterfaceSettingsAction.SetQuickFilterBarCollapsible -> viewModelScope.launch {
                settingsRepository.setQuickFilterBarCollapsible(action.enabled)
            }

            is InterfaceSettingsAction.SetOuterCornerRadius -> viewModelScope.launch {
                settingsRepository.setOuterCornerRadius(action.radiusDp)
            }

            is InterfaceSettingsAction.SetInnerCornerRadius -> viewModelScope.launch {
                settingsRepository.setInnerCornerRadius(action.radiusDp)
            }

            is InterfaceSettingsAction.SetGroupItemSpacing -> viewModelScope.launch {
                settingsRepository.setGroupItemSpacing(action.spacingDp)
            }

            is InterfaceSettingsAction.SetGroupContentPadding -> viewModelScope.launch {
                settingsRepository.setGroupContentPadding(action.paddingDp)
            }

            is InterfaceSettingsAction.ToggleVisibleLibraryQuickFilter -> viewModelScope.launch {
                val nextKeys = LibraryQuickFilter.toggleVisibleKey(
                    enabledKeys = uiState.value.enabledLibraryQuickFilterKeys,
                    quickFilter = action.quickFilter
                )
                settingsRepository.setVisibleLibraryQuickFilters(nextKeys)
            }

            is InterfaceSettingsAction.SetEntryHierarchyDisplayMode -> viewModelScope.launch {
                settingsRepository.setEntryHierarchyDisplayMode(action.mode)
            }
        }
    }
}
