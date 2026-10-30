package com.aozijx.passly.feature.settings.presentation.autofill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.feature.autofill.platform.AutofillServiceStatusSource
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    private val settingsRepository: InteractionSettingsRepository,
    private val autofillServiceStatusSource: AutofillServiceStatusSource,
) : ViewModel() {

    private data class PageEffect(val owner: Any, val effect: AutofillSettingsEffect)

    private val _effects = Channel<PageEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
        .filter { activePageOwner === it.owner }
        .map { it.effect }
    private var activePageOwner: Any? = null

    val uiState: StateFlow<AutofillSettingsUiState> = combine(
        settingsRepository.interaction,
        autofillServiceStatusSource.observeServiceEnabled(),
    ) { interaction, systemAutofillEnabled ->
        AutofillSettingsUiState.Ready(
            autofill = interaction.autofill,
            isSystemAutofillEnabled = systemAutofillEnabled,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AutofillSettingsUiState.Loading,
        )

    fun onAction(action: AutofillSettingsAction) {
        when (action) {
            is AutofillSettingsAction.SetEnabled -> saveSetting {
                settingsRepository.setAutofillEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetPresentation -> saveSetting {
                settingsRepository.setAutofillPresentation(action.presentation)
            }
            is AutofillSettingsAction.SetCredentialManagerEnabled -> saveSetting {
                settingsRepository.setCredentialManagerEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetAuthenticationRequired -> saveSetting {
                settingsRepository.setAutofillAuthenticationRequired(action.required)
            }
            is AutofillSettingsAction.SetOtpEnabled -> saveSetting {
                settingsRepository.setAutofillOtpEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetSavePromptsEnabled -> saveSetting {
                settingsRepository.setAutofillSavePromptsEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetUnmatchedSuggestionsEnabled -> saveSetting {
                settingsRepository.setUnmatchedAutofillSuggestionsEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetMaxSuggestions -> saveSetting {
                settingsRepository.setAutofillMaxSuggestions(action.count)
            }
            AutofillSettingsAction.OpenSystemAutofillSettings ->
                emitEffect(AutofillSettingsEffect.OpenSystemAutofillSettings)
            AutofillSettingsAction.OpenAccessibilitySettings ->
                emitEffect(AutofillSettingsEffect.OpenAccessibilitySettings)
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        val owner = activePageOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.settings_autofill_save_failed,
                onFailure = { emitEffect(owner, AutofillSettingsEffect.SaveFailed(it)) },
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

    private fun emitEffect(effect: AutofillSettingsEffect) {
        activePageOwner?.let { emitEffect(it, effect) }
    }

    private fun emitEffect(owner: Any, effect: AutofillSettingsEffect) {
        if (activePageOwner === owner) _effects.trySend(PageEffect(owner, effect))
    }
}
