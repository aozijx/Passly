package com.aozijx.passly.presentation.feature.settings.ui.autofill.model

data class AutofillSettingsUiModel(
    val enabled: Boolean,
    val presentation: AutofillPresentationUiModel,
    val credentialManagerEnabled: Boolean,
    val supportsCredentialManager: Boolean,
    val requireAuthentication: Boolean,
    val includeOtp: Boolean,
    val savePromptsEnabled: Boolean,
    val allowUnmatchedSuggestions: Boolean,
    val maxSuggestions: Int,
    val minSuggestions: Int,
    val maxSuggestionsLimit: Int,
    val isSystemServiceEnabled: Boolean,
)

sealed interface AutofillSettingsEvent {
    data object OpenSystemSettings : AutofillSettingsEvent
    data class EnabledChanged(val enabled: Boolean) : AutofillSettingsEvent
    data class PresentationChanged(
        val presentation: AutofillPresentationUiModel,
    ) : AutofillSettingsEvent
    data class CredentialManagerEnabledChanged(val enabled: Boolean) : AutofillSettingsEvent
    data class AuthenticationRequiredChanged(val required: Boolean) : AutofillSettingsEvent
    data class OtpEnabledChanged(val enabled: Boolean) : AutofillSettingsEvent
    data class SavePromptsEnabledChanged(val enabled: Boolean) : AutofillSettingsEvent
    data class UnmatchedSuggestionsEnabledChanged(val enabled: Boolean) : AutofillSettingsEvent
    data class MaxSuggestionsChanged(val maxSuggestions: Int) : AutofillSettingsEvent
}

enum class AutofillPresentationUiModel {
    SYSTEM_INLINE,
    BOTTOM_SHEET,
}
