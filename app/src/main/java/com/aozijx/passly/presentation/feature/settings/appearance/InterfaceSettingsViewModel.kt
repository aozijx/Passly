package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: InterfaceSettingsRepository,
    private val librarySettingsRepository: LibraryViewSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<InterfaceSettingsUiState> = combine(
        settingsRepository.interfaceSettings,
        librarySettingsRepository.libraryViewSettings,
    ) { prefs, librarySettings ->
        InterfaceSettingsUiState(
            hideSystemBars = prefs.hideSystemBars,
            collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
            collapseQuickFilterBarOnScroll = prefs.collapseQuickFilterBarOnScroll,
            appCornerRadiusDp = prefs.appCornerRadiusDp,
            enabledLibraryQuickFilterKeys =
                librarySettings.visibleQuickFilters?.filterKeys
                    ?: LibraryQuickFilter.defaultVisibleKeys,
            entryHierarchyDisplayMode = librarySettings.entryHierarchyDisplayMode,
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

            is InterfaceSettingsAction.SetAppCornerRadius -> viewModelScope.launch {
                settingsRepository.setAppCornerRadius(action.radiusDp)
            }

            is InterfaceSettingsAction.ToggleVisibleLibraryQuickFilter -> viewModelScope.launch {
                val nextKeys = LibraryQuickFilter.toggleVisibleKey(
                    enabledKeys = uiState.value.enabledLibraryQuickFilterKeys,
                    quickFilter = action.quickFilter
                )
                librarySettingsRepository.setVisibleQuickFilters(nextKeys)
            }

            is InterfaceSettingsAction.SetEntryHierarchyDisplayMode -> viewModelScope.launch {
                librarySettingsRepository.setEntryHierarchyDisplayMode(action.mode)
            }
        }
    }
}
