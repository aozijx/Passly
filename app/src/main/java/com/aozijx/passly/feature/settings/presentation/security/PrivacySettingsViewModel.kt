package com.aozijx.passly.feature.settings.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.clipboard.port.OwnedClipboardCleaner
import com.aozijx.passly.domain.settings.port.SecuritySettingsRepository
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PrivacySettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SecuritySettingsRepository,
        private val clipboardCleaner: OwnedClipboardCleaner,
    ) : ViewModel() {
        private val pageEffects = PageEffects<PrivacySettingsEffect>()
        val effects = pageEffects.effects

        private val settingsRead =
            RecoverableRead(
                source =
                    settingsRepository.security
                        .map { security ->
                            PrivacySettingsUiState.Ready(
                                isSecureContentEnabled = security.isSecureContentEnabled,
                                isFlipToLockEnabled = security.isFlipToLockEnabled,
                                isFlipExitAndClearStackEnabled = security.isFlipExitAndClearStackEnabled,
                                reauthenticateSensitiveCopies = security.reauthenticateSensitiveCopies,
                                clipboardClearEnabled = security.clipboardClearPolicy.enabled,
                                clipboardClearDelaySeconds = security.clipboardClearPolicy.delaySeconds,
                            )
                        },
                scope = viewModelScope,
            )

        val uiState: StateFlow<PrivacySettingsUiState> =
            settingsRead.state
                .map { state ->
                    when (state) {
                        RecoverableReadState.Loading -> PrivacySettingsUiState.Loading
                        is RecoverableReadState.Ready -> state.value
                        is RecoverableReadState.Failed -> PrivacySettingsUiState.Failed(state.failure)
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000L),
                    PrivacySettingsUiState.Loading,
                )

        fun onAction(action: PrivacySettingsAction) {
            when (action) {
                PrivacySettingsAction.RetryRead -> {
                    settingsRead.retry()
                }

                is PrivacySettingsAction.SetSecureContentEnabled -> {
                    saveSetting {
                        settingsRepository.setSecureContentEnabled(action.enabled)
                    }
                }

                is PrivacySettingsAction.SetFlipToLockEnabled -> {
                    saveSetting {
                        settingsRepository.setFlipToLockEnabled(action.enabled)
                    }
                }

                is PrivacySettingsAction.SetFlipExitAndClearStackEnabled -> {
                    saveSetting {
                        settingsRepository.setFlipExitAndClearStackEnabled(action.enabled)
                    }
                }

                is PrivacySettingsAction.SetSensitiveCopyReauthentication -> {
                    saveSetting {
                        settingsRepository.setReauthenticateSensitiveCopies(action.enabled)
                    }
                }

                is PrivacySettingsAction.SetClipboardClearEnabled -> {
                    saveSetting {
                        settingsRepository.setClipboardClearEnabled(action.enabled)
                    }
                }

                is PrivacySettingsAction.SetClipboardClearDelaySeconds -> {
                    saveSetting {
                        settingsRepository.setClipboardClearDelaySeconds(action.seconds)
                    }
                }

                PrivacySettingsAction.ClearClipboardNow -> {
                    val owner = pageEffects.currentOwner ?: return
                    val effect =
                        if (clipboardCleaner.clearOwned()) {
                            PrivacySettingsEffect.ClipboardCleared
                        } else {
                            PrivacySettingsEffect.ClipboardNotCleared
                        }
                    emitEffect(owner, effect)
                }
            }
        }

        private fun saveSetting(write: suspend () -> Unit) {
            if (uiState.value !is PrivacySettingsUiState.Ready) return
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                writePreferenceWithFeedback(
                    failureRes = R.string.settings_privacy_save_failed,
                    onFailure = { emitEffect(owner, PrivacySettingsEffect.SaveFailed(it)) },
                    write = write,
                )
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }

        private fun emitEffect(
            owner: Any,
            effect: PrivacySettingsEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }
    }
