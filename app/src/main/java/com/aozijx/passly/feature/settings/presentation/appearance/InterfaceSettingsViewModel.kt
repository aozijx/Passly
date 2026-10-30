package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.presentation.shared.event.PageEffects
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
    private val pageEffects = PageEffects<UiErrorMessage>()
    val errors = pageEffects.effects

    val uiState: StateFlow<InterfaceSettingsUiState> = combine(
        settingsRepository.interfaceSettings,
        librarySettingsRepository.libraryViewSettings,
    ) { prefs, librarySettings ->
        InterfaceSettingsUiState.Ready(
            hideSystemBars = prefs.hideSystemBars,
            collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
            collapseQuickFilterBarOnScroll = prefs.collapseQuickFilterBarOnScroll,
            appCornerRadiusDp = prefs.appCornerRadiusDp,
            entryHierarchyDisplayMode = librarySettings.entryHierarchyDisplayMode,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InterfaceSettingsUiState.Loading
        )

    fun onAction(action: InterfaceSettingsAction) {
        when (action) {
            is InterfaceSettingsAction.SetHideSystemBars -> saveSetting {
                settingsRepository.setHideSystemBars(action.enabled)
            }

            is InterfaceSettingsAction.SetTopBarCollapsible -> saveSetting {
                settingsRepository.setTopBarCollapsible(action.enabled)
            }

            is InterfaceSettingsAction.SetQuickFilterBarCollapsible -> saveSetting {
                settingsRepository.setQuickFilterBarCollapsible(action.enabled)
            }

            is InterfaceSettingsAction.SetAppCornerRadius -> saveSetting {
                settingsRepository.setAppCornerRadius(action.radiusDp)
            }

            is InterfaceSettingsAction.SetEntryHierarchyDisplayMode -> saveSetting {
                librarySettingsRepository.setEntryHierarchyDisplayMode(action.mode)
            }
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_interface_save_failed,
                onFailure = {
                    pageEffects.tryEmit(owner, it)
                },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }
}
