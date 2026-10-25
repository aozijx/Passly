package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings

sealed interface AutofillSettingsUiState {
    data object Loading : AutofillSettingsUiState

    data class Ready(
        val autofill: AutofillSettings,
        val isSystemAutofillEnabled: Boolean,
    ) : AutofillSettingsUiState
}

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
    data object OpenSystemAutofillSettings : AutofillSettingsAction
    data object OpenAccessibilitySettings : AutofillSettingsAction
}

sealed interface AutofillSettingsEffect {
    data object OpenSystemAutofillSettings : AutofillSettingsEffect
    data object OpenAccessibilitySettings : AutofillSettingsEffect
}
