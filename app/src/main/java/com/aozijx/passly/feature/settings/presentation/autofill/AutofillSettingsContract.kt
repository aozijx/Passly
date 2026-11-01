package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface AutofillSourceState<out T> {
    val availableValue: T?
        get() = when (this) {
            is Loading -> previous
            is Ready -> value
            is Failed -> previous
        }

    data class Loading<T>(val previous: T? = null) : AutofillSourceState<T>
    data class Ready<T>(val value: T) : AutofillSourceState<T>
    data class Failed<T>(val previous: T?) : AutofillSourceState<T>
}

data class AutofillSettingsUiState(
    val preferences: AutofillSourceState<AutofillSettings> = AutofillSourceState.Loading(),
    val systemService: AutofillSourceState<Boolean> = AutofillSourceState.Loading(),
)

sealed interface AutofillSettingsAction {
    data class SetEnabled(val enabled: Boolean) : AutofillSettingsAction
    data class SetPresentation(
        val presentation: AutofillPresentation,
    ) : AutofillSettingsAction

    data class SetCredentialManagerEnabled(val enabled: Boolean) : AutofillSettingsAction
    data class SetAuthenticationRequired(val required: Boolean) : AutofillSettingsAction
    data class SetOtpEnabled(val enabled: Boolean) : AutofillSettingsAction
    data class SetSavePromptsEnabled(val enabled: Boolean) : AutofillSettingsAction
    data class SetUnmatchedSuggestionsEnabled(val enabled: Boolean) : AutofillSettingsAction
    data class SetMaxSuggestions(val count: Int) : AutofillSettingsAction
    data object RetryPreferences : AutofillSettingsAction
    data object RetrySystemService : AutofillSettingsAction
    data object OpenSystemAutofillSettings : AutofillSettingsAction
    data object OpenAccessibilitySettings : AutofillSettingsAction
}

sealed interface AutofillSettingsEffect {
    data class SaveFailed(val message: UiErrorMessage) : AutofillSettingsEffect
    data object OpenSystemAutofillSettings : AutofillSettingsEffect
    data object OpenAccessibilitySettings : AutofillSettingsEffect
}
