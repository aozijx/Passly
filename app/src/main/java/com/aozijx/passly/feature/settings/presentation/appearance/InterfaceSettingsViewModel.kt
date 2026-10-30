package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: InterfaceSettingsRepository,
    private val librarySettingsRepository: LibraryViewSettingsRepository,
) : ViewModel() {
    private val errorChannel = Channel<UiErrorMessage>(Channel.BUFFERED)
    val errors = errorChannel.receiveAsFlow()
    private var activePageOwner: Any? = null

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
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_interface_save_failed,
                onFailure = {
                    if (activePageOwner === owner) errorChannel.trySend(it)
                },
                write = write,
            )
        }
    }

    fun onPageEntered(): Any {
        val owner = Any()
        activePageOwner = owner
        return owner
    }

    fun onPageExited(owner: Any) {
        if (activePageOwner === owner) activePageOwner = null
    }
}
