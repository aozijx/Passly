package com.aozijx.passly.presentation.feature.settings.autofill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.feature.autofill.shared.ObserveAutofillStatusUseCase
import com.aozijx.passly.feature.autofill.shared.OpenAutofillSettingsUseCase
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    observeAutofillStatus: ObserveAutofillStatusUseCase,
    private val openAutofillSettings: OpenAutofillSettingsUseCase,
) : ViewModel() {

    val uiState: StateFlow<AutofillSettingsUiState> = combine(
        settingsRepository.settings,
        observeAutofillStatus(),
    ) { settings, systemAutofillEnabled ->
        AutofillSettingsUiState(
            autofill = settings.interaction.autofill,
            isSystemAutofillEnabled = systemAutofillEnabled,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            AutofillSettingsUiState(),
        )

    fun onAction(action: AutofillSettingsAction) {
        val command = when (action) {
            is AutofillSettingsAction.SetEnabled ->
                SettingsCommand.SetAutofillEnabled(action.enabled)

            is AutofillSettingsAction.SetPresentation ->
                SettingsCommand.SetAutofillPresentation(action.presentation)

            is AutofillSettingsAction.SetCredentialManagerEnabled ->
                SettingsCommand.SetCredentialManagerEnabled(action.enabled)

            is AutofillSettingsAction.SetAuthenticationRequired ->
                SettingsCommand.SetAutofillAuthenticationRequired(action.required)

            is AutofillSettingsAction.SetOtpEnabled ->
                SettingsCommand.SetAutofillOtpEnabled(action.enabled)

            is AutofillSettingsAction.SetSavePromptsEnabled ->
                SettingsCommand.SetAutofillSavePromptsEnabled(action.enabled)

            is AutofillSettingsAction.SetUnmatchedSuggestionsEnabled ->
                SettingsCommand.SetUnmatchedAutofillSuggestionsEnabled(action.enabled)

            is AutofillSettingsAction.SetMaxSuggestions ->
                SettingsCommand.SetAutofillMaxSuggestions(action.count)

            AutofillSettingsAction.OpenSystemAutofillSettings -> null
        }
        if (command == null) {
            openAutofillSettings()
        } else {
            viewModelScope.launch { settingsRepository.update(command) }
        }
    }
}
