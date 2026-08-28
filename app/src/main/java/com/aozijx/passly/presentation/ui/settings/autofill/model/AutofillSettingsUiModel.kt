package com.aozijx.passly.presentation.ui.settings.autofill.model

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

interface AutofillSettingsEventHandler {
    fun onOpenSystemSettings()
    fun onEnabledChanged(enabled: Boolean)
    fun onPresentationChanged(presentation: AutofillPresentationUiModel)
    fun onCredentialManagerEnabledChanged(enabled: Boolean)
    fun onAuthenticationRequiredChanged(required: Boolean)
    fun onOtpEnabledChanged(enabled: Boolean)
    fun onSavePromptsEnabledChanged(enabled: Boolean)
    fun onUnmatchedSuggestionsEnabledChanged(enabled: Boolean)
    fun onMaxSuggestionsChanged(maxSuggestions: Int)
}

enum class AutofillPresentationUiModel {
    SYSTEM_INLINE,
    BOTTOM_SHEET,
}
