package com.aozijx.passly.feature.settings.interaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.autofill.usecase.AutofillUseCases
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.domain.settings.model.SwipeActionType
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InteractionUiState(
    val isSwipeEnabled: Boolean = false,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL,
    val autofill: AutofillSettings = AutofillSettings(),
    val isSystemAutofillEnabled: Boolean = false,
)

sealed interface InteractionUiAction {
    data class SetSwipeEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetSwipeLeftAction(val action: SwipeActionType) : InteractionUiAction
    data class SetSwipeRightAction(val action: SwipeActionType) : InteractionUiAction
    data class SetAutofillEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetAutofillPresentation(
        val presentation: AutofillPresentation
    ) : InteractionUiAction

    data class SetCredentialManagerEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetAutofillAuthenticationRequired(val required: Boolean) : InteractionUiAction
    data class SetAutofillOtpEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetAutofillSavePromptsEnabled(val enabled: Boolean) : InteractionUiAction
    data class SetUnmatchedAutofillSuggestionsEnabled(
        val enabled: Boolean
    ) : InteractionUiAction

    data class SetAutofillMaxSuggestions(val count: Int) : InteractionUiAction
}

@HiltViewModel
class InteractionViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: AppSettingsRepository,
    private val autofillUseCases: AutofillUseCases,
) : AndroidViewModel(application) {

    val config: StateFlow<InteractionUiState> = combine(
        settingsRepository.settings,
        autofillUseCases.observeStatus(),
    ) { settings, systemAutofillEnabled ->
        InteractionUiState(
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
            InteractionUiState(),
        )

    fun onAction(action: InteractionUiAction) {
        val command = when (action) {
            is InteractionUiAction.SetSwipeEnabled ->
                SettingsCommand.SetSwipeEnabled(action.enabled)

            is InteractionUiAction.SetSwipeLeftAction ->
                SettingsCommand.SetSwipeLeftAction(action.action)

            is InteractionUiAction.SetSwipeRightAction ->
                SettingsCommand.SetSwipeRightAction(action.action)

            is InteractionUiAction.SetAutofillEnabled ->
                SettingsCommand.SetAutofillEnabled(action.enabled)

            is InteractionUiAction.SetAutofillPresentation ->
                SettingsCommand.SetAutofillPresentation(action.presentation)

            is InteractionUiAction.SetCredentialManagerEnabled ->
                SettingsCommand.SetCredentialManagerEnabled(action.enabled)

            is InteractionUiAction.SetAutofillAuthenticationRequired ->
                SettingsCommand.SetAutofillAuthenticationRequired(action.required)

            is InteractionUiAction.SetAutofillOtpEnabled ->
                SettingsCommand.SetAutofillOtpEnabled(action.enabled)

            is InteractionUiAction.SetAutofillSavePromptsEnabled ->
                SettingsCommand.SetAutofillSavePromptsEnabled(action.enabled)

            is InteractionUiAction.SetUnmatchedAutofillSuggestionsEnabled ->
                SettingsCommand.SetUnmatchedAutofillSuggestionsEnabled(action.enabled)

            is InteractionUiAction.SetAutofillMaxSuggestions ->
                SettingsCommand.SetAutofillMaxSuggestions(action.count)
        }
        viewModelScope.launch { settingsRepository.update(command) }
    }

    fun openAutofillSettings() {
        autofillUseCases.openSettings()
    }
}
