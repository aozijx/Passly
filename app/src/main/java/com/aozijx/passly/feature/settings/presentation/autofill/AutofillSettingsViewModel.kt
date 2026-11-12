package com.aozijx.passly.feature.settings.presentation.autofill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.app.autofill.AutofillServiceStatusSource
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.domain.settings.port.AutofillSettingsRepository
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
internal class AutofillSettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: AutofillSettingsRepository,
        private val autofillServiceStatusSource: AutofillServiceStatusSource,
    ) : ViewModel() {
        private val pageEffects = PageEffects<AutofillSettingsEffect>()
        val effects = pageEffects.effects

        private val retrySystemService = MutableStateFlow(0)

        private val preferencesRead =
            RecoverableRead(
                source = settingsRepository.autofillSettings,
                scope = viewModelScope,
            )
        private val preferences =
            preferencesRead.state.map(AutofillSettingsUiStateMapper::preferences)
        private val systemServiceStatus = observeSystemService(retrySystemService)

        val uiState: StateFlow<AutofillSettingsUiState> =
            combine(
                preferences,
                systemServiceStatus,
            ) { preferences, systemServiceStatus ->
                AutofillSettingsUiState(preferences, systemServiceStatus)
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000L),
                AutofillSettingsUiState(),
            )

        fun onAction(action: AutofillSettingsAction) {
            when (action) {
                is AutofillSettingsAction.SetEnabled -> {
                    saveSetting {
                        settingsRepository.setAutofillEnabled(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetPresentation -> {
                    saveSetting {
                        settingsRepository.setAutofillPresentation(action.presentation)
                    }
                }

                is AutofillSettingsAction.SetCredentialManagerEnabled -> {
                    saveSetting {
                        settingsRepository.setCredentialManagerEnabled(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetAuthenticationRequired -> {
                    saveSetting {
                        settingsRepository.setAutofillAuthenticationRequired(action.required)
                    }
                }

                is AutofillSettingsAction.SetStrictDomainMatching -> {
                    saveSetting {
                        settingsRepository.setAutofillStrictDomainMatching(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetOtpAutofillEnabled -> {
                    saveSetting {
                        settingsRepository.setOtpAutofillEnabled(action.enabled)
                    }
                }

                AutofillSettingsAction.RetryPreferences -> {
                    preferencesRead.retry()
                }

                AutofillSettingsAction.RetrySystemService -> {
                    retrySystemService.value++
                }

                AutofillSettingsAction.OpenSystemAutofillSettings -> {
                    emitEffect(AutofillSettingsEffect.OpenSystemAutofillSettings)
                }

                AutofillSettingsAction.OpenAccessibilitySettings -> {
                    emitEffect(AutofillSettingsEffect.OpenAccessibilitySettings)
                }
            }
        }

        private fun saveSetting(write: suspend () -> Unit) {
            if (uiState.value.preferences !is AutofillPreferencesState.Ready) return
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                writePreferenceWithFeedback(
                    failureRes = R.string.settings_autofill_save_failed,
                    onFailure = { emitEffect(owner, AutofillSettingsEffect.SaveFailed(it)) },
                    write = write,
                )
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }

        private fun emitEffect(effect: AutofillSettingsEffect) {
            pageEffects.tryEmit(effect)
        }

        private fun emitEffect(
            owner: Any,
            effect: AutofillSettingsEffect,
        ) {
            pageEffects.tryEmit(owner, effect)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeSystemService(retry: MutableStateFlow<Int>) =
            retry
                .flatMapLatest {
                    flow {
                        emit(AutofillSystemServiceObservation.Checking)
                        autofillServiceStatusSource.observeServiceEnabled().collect { isEnabled ->
                            emit(AutofillSystemServiceObservation.EnabledChanged(isEnabled))
                        }
                    }.catch { error ->
                        when (error) {
                            is CancellationException -> throw error
                            is Exception -> emit(AutofillSystemServiceObservation.Unavailable)
                            else -> throw error
                        }
                    }
                }.runningFold(
                    AutofillSystemServiceStatus.Checking,
                    AutofillSettingsUiStateMapper::systemServiceStatus,
                )
    }
