package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel

fun AutofillSettingsUiState.Ready.toAutofillSettingsUiModel(
    supportsCredentialManager: Boolean,
): AutofillSettingsUiModel = AutofillSettingsUiModel(
    enabled = autofill.enabled,
    presentation = autofill.presentation,
    credentialManagerEnabled = autofill.credentialManagerEnabled,
    supportsCredentialManager = supportsCredentialManager,
    requireAuthentication = autofill.requireAuthentication,
    includeOtp = autofill.includeOtp,
    savePromptsEnabled = autofill.savePromptsEnabled,
    allowUnmatchedSuggestions = autofill.allowUnmatchedSuggestions,
    maxSuggestions = autofill.normalizedMaxSuggestions,
    minSuggestions = AutofillSettings.MIN_SUGGESTIONS,
    maxSuggestionsLimit = AutofillSettings.MAX_SUGGESTIONS,
    isSystemServiceEnabled = isSystemAutofillEnabled,
)
