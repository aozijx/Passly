package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import com.aozijx.passly.feature.settings.presentation.ui.appearance.model.InterfaceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class InterfaceSettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: InterfaceSettingsRepository,
        private val librarySettingsRepository: LibraryViewSettingsRepository,
    ) : ViewModel() {
        private val pageEffects = PageEffects<UiErrorMessage>()
        val errors = pageEffects.effects

        private val settingsRead =
            RecoverableRead(
                source =
                    combine(
                        settingsRepository.interfaceSettings,
                        librarySettingsRepository.libraryViewSettings,
                    ) { prefs, librarySettings ->
                        InterfaceUiModel(
                            hideSystemBars = prefs.hideSystemBars,
                            collapseTopBarOnScroll = prefs.collapseTopBarOnScroll,
                            collapseQuickFilterBarOnScroll = prefs.collapseQuickFilterBarOnScroll,
                            appCornerRadiusDp = prefs.appCornerRadiusDp,
                            appCornerRadiusRange =
                                AppCornerRadiusConstraints.MIN_DP..AppCornerRadiusConstraints.MAX_DP,
                            entryHierarchyDisplayMode = librarySettings.entryHierarchyDisplayMode,
                            entryCardPresentations = librarySettings.entryCardPresentations,
                        )
                    },
                scope = viewModelScope,
            )

        val uiState: StateFlow<RecoverableReadState<InterfaceUiModel>> = settingsRead.state

        fun onAction(action: InterfaceSettingsAction) {
            when (action) {
                InterfaceSettingsAction.RetryRead -> {
                    settingsRead.retry()
                }

                is InterfaceSettingsAction.SetHideSystemBars -> {
                    saveSetting {
                        settingsRepository.setHideSystemBars(action.enabled)
                    }
                }

                is InterfaceSettingsAction.SetTopBarCollapsible -> {
                    saveSetting {
                        settingsRepository.setTopBarCollapsible(action.enabled)
                    }
                }

                is InterfaceSettingsAction.SetQuickFilterBarCollapsible -> {
                    saveSetting {
                        settingsRepository.setQuickFilterBarCollapsible(action.enabled)
                    }
                }

                is InterfaceSettingsAction.SetAppCornerRadius -> {
                    saveSetting {
                        settingsRepository.setAppCornerRadius(action.radiusDp)
                    }
                }

                is InterfaceSettingsAction.SetEntryHierarchyDisplayMode -> {
                    saveSetting {
                        librarySettingsRepository.setEntryHierarchyDisplayMode(action.mode)
                    }
                }

                is InterfaceSettingsAction.SetEntryCardPresentation -> {
                    saveSetting {
                        librarySettingsRepository.setEntryCardPresentation(action.presentation)
                    }
                }
            }
        }

        private fun saveSetting(write: suspend () -> Unit) {
            if (uiState.value !is RecoverableReadState.Ready) return
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

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }
    }
