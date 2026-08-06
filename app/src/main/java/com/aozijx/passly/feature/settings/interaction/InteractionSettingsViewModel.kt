package com.aozijx.passly.feature.settings.interaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.autofill.usecase.AutofillUseCases
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionSettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: AppSettingsRepository,
    private val autofillUseCases: AutofillUseCases,
) : AndroidViewModel(application) {

    val config: StateFlow<InteractionSettingsUiState> = combine(
        settingsRepository.settings,
        autofillUseCases.observeStatus(),
    ) { settings, systemAutofillEnabled ->
        InteractionSettingsUiState(
            isSwipeEnabled = settings.interaction.isSwipeEnabled,
            swipeLeftAction = settings.interaction.swipeLeftAction,
            swipeRightAction = settings.interaction.swipeRightAction,
            autofill = settings.interaction.autofill,
            isSystemAutofillEnabled = systemAutofillEnabled,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            InteractionSettingsUiState(),
        )

    fun onAction(action: InteractionSettingsAction) {
        val command = when (action) {
            is InteractionSettingsAction.SetSwipeEnabled ->
                SettingsCommand.SetSwipeEnabled(action.enabled)

            is InteractionSettingsAction.SetSwipeLeftAction ->
                SettingsCommand.SetSwipeLeftAction(action.action)

            is InteractionSettingsAction.SetSwipeRightAction ->
                SettingsCommand.SetSwipeRightAction(action.action)

            is InteractionSettingsAction.SetAutofillEnabled ->
                SettingsCommand.SetAutofillEnabled(action.enabled)

            is InteractionSettingsAction.SetAutofillPresentation ->
                SettingsCommand.SetAutofillPresentation(action.presentation)

            is InteractionSettingsAction.SetCredentialManagerEnabled ->
                SettingsCommand.SetCredentialManagerEnabled(action.enabled)

            is InteractionSettingsAction.SetAutofillAuthenticationRequired ->
                SettingsCommand.SetAutofillAuthenticationRequired(action.required)

            is InteractionSettingsAction.SetAutofillOtpEnabled ->
                SettingsCommand.SetAutofillOtpEnabled(action.enabled)

            is InteractionSettingsAction.SetAutofillSavePromptsEnabled ->
                SettingsCommand.SetAutofillSavePromptsEnabled(action.enabled)

            is InteractionSettingsAction.SetUnmatchedAutofillSuggestionsEnabled ->
                SettingsCommand.SetUnmatchedAutofillSuggestionsEnabled(action.enabled)

            is InteractionSettingsAction.SetAutofillMaxSuggestions ->
                SettingsCommand.SetAutofillMaxSuggestions(action.count)
        }
        viewModelScope.launch { settingsRepository.update(command) }
    }

    fun openAutofillSettings() {
        autofillUseCases.openSettings()
    }
}
