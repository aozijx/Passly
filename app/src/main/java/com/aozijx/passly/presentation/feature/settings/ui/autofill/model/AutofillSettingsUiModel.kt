package com.aozijx.passly.presentation.feature.settings.ui.autofill.model

import com.aozijx.passly.presentation.feature.settings.autofill.AutofillPresentationUiModel

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
