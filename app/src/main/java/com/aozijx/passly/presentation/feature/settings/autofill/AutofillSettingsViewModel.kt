package com.aozijx.passly.presentation.feature.settings.autofill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.autofill.platform.AutofillPlatformGateway
import com.aozijx.passly.domain.settings.port.InteractionSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    private val settingsRepository: InteractionSettingsRepository,
    private val autofillPlatformGateway: AutofillPlatformGateway,
) : ViewModel() {

    val uiState: StateFlow<AutofillSettingsUiState> = combine(
        settingsRepository.interaction,
        autofillPlatformGateway.observeServiceEnabled(),
    ) { interaction, systemAutofillEnabled ->
        AutofillSettingsUiState(
            autofill = interaction.autofill,
            isSystemAutofillEnabled = systemAutofillEnabled,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AutofillSettingsUiState(),
        )

    fun onAction(action: AutofillSettingsAction) {
        when (action) {
            is AutofillSettingsAction.SetEnabled -> viewModelScope.launch {
                settingsRepository.setAutofillEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetPresentation -> viewModelScope.launch {
                settingsRepository.setAutofillPresentation(action.presentation)
            }
            is AutofillSettingsAction.SetCredentialManagerEnabled -> viewModelScope.launch {
                settingsRepository.setCredentialManagerEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetAuthenticationRequired -> viewModelScope.launch {
                settingsRepository.setAutofillAuthenticationRequired(action.required)
            }
            is AutofillSettingsAction.SetOtpEnabled -> viewModelScope.launch {
                settingsRepository.setAutofillOtpEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetSavePromptsEnabled -> viewModelScope.launch {
                settingsRepository.setAutofillSavePromptsEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetUnmatchedSuggestionsEnabled -> viewModelScope.launch {
                settingsRepository.setUnmatchedAutofillSuggestionsEnabled(action.enabled)
            }
            is AutofillSettingsAction.SetMaxSuggestions -> viewModelScope.launch {
                settingsRepository.setAutofillMaxSuggestions(action.count)
            }
            AutofillSettingsAction.OpenSystemAutofillSettings -> autofillPlatformGateway.openSystemSettings()
        }
    }
}
