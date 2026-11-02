package com.aozijx.passly.feature.settings.presentation.autofill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AutofillSettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: InteractionSettingsRepository,
        private val autofillServiceStatusSource: AutofillServiceStatusSource,
    ) : ViewModel() {
        private val pageEffects = PageEffects<AutofillSettingsEffect>()
        val effects = pageEffects.effects

        private val retryPreferences = MutableStateFlow(0)
        private val retrySystemService = MutableStateFlow(0)
        private val lastPreferences = MutableStateFlow<AutofillSettings?>(null)
        private val lastSystemServiceEnabled = MutableStateFlow<Boolean?>(null)

        private val preferences =
            observeSource(
                retry = retryPreferences,
                lastValue = lastPreferences,
                source = { settingsRepository.interaction.map { it.autofill } },
            )
        private val systemService =
            observeSource(
                retry = retrySystemService,
                lastValue = lastSystemServiceEnabled,
                source = { autofillServiceStatusSource.observeServiceEnabled() },
            )

        val uiState: StateFlow<AutofillSettingsUiState> =
            combine(
                preferences,
                systemService,
            ) { preferences, systemService ->
                AutofillSettingsUiState(preferences, systemService)
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

                is AutofillSettingsAction.SetOtpEnabled -> {
                    saveSetting {
                        settingsRepository.setAutofillOtpEnabled(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetSavePromptsEnabled -> {
                    saveSetting {
                        settingsRepository.setAutofillSavePromptsEnabled(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetUnmatchedSuggestionsEnabled -> {
                    saveSetting {
                        settingsRepository.setUnmatchedAutofillSuggestionsEnabled(action.enabled)
                    }
                }

                is AutofillSettingsAction.SetMaxSuggestions -> {
                    saveSetting {
                        settingsRepository.setAutofillMaxSuggestions(action.count)
                    }
                }

                AutofillSettingsAction.RetryPreferences -> {
                    retryPreferences.value++
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
            if (uiState.value.preferences !is AutofillSourceState.Ready) return
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
        private fun <T : Any> observeSource(
            retry: MutableStateFlow<Int>,
            lastValue: MutableStateFlow<T?>,
            source: () -> Flow<T>,
        ): Flow<AutofillSourceState<T>> =
            retry.flatMapLatest {
                flow {
                    emit(AutofillSourceState.Loading(lastValue.value))
                    source().collect { value ->
                        lastValue.value = value
                        emit(AutofillSourceState.Ready(value))
                    }
                }.catch { error ->
                    when (error) {
                        is CancellationException -> throw error
                        is Exception -> emit(AutofillSourceState.Failed(lastValue.value))
                        else -> throw error
                    }
                }
            }
    }
