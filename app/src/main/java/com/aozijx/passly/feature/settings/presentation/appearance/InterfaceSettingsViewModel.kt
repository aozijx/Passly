package com.aozijx.passly.feature.settings.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.domain.settings.port.OtpRuntimePolicyRepository
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
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
        private val otpRuntimePolicyRepository: OtpRuntimePolicyRepository,
    ) : ViewModel() {
        private val pageEffects = PageEffects<UiErrorMessage>()
        val errors = pageEffects.effects

        private val settingsRead =
            RecoverableRead(
                source =
                    combine(
                        settingsRepository.interfaceSettings,
                        librarySettingsRepository.libraryViewSettings,
                        otpRuntimePolicyRepository.otpRuntimePolicy,
                    ) { prefs, librarySettings, otpPolicy ->
                        InterfaceSettingsUiMapper.map(prefs, librarySettings, otpPolicy)
                    },
                scope = viewModelScope,
            )

        val uiState: StateFlow<RecoverableReadState<InterfaceUiState>> = settingsRead.state

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

                is InterfaceSettingsAction.SetCopyNextTotpNearExpiry -> {
                    saveSetting {
                        otpRuntimePolicyRepository.setCopyNextTotpNearExpiry(action.enabled)
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
