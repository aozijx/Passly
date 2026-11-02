package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel

fun AutofillSettings.toAutofillSettingsUiModel(
    supportsCredentialManager: Boolean,
    canEditPreferences: Boolean,
    preferencesRefreshing: Boolean,
    preferencesFailed: Boolean,
    systemService: AutofillSourceState<Boolean>,
): AutofillSettingsUiModel =
    AutofillSettingsUiModel(
        enabled = enabled,
        presentation = presentation,
        credentialManagerEnabled = credentialManagerEnabled,
        supportsCredentialManager = supportsCredentialManager,
        requireAuthentication = requireAuthentication,
        includeOtp = includeOtp,
        savePromptsEnabled = savePromptsEnabled,
        allowUnmatchedSuggestions = allowUnmatchedSuggestions,
        maxSuggestions = normalizedMaxSuggestions,
        minSuggestions = AutofillSettings.MIN_SUGGESTIONS,
        maxSuggestionsLimit = AutofillSettings.MAX_SUGGESTIONS,
        canEditPreferences = canEditPreferences,
        preferencesRefreshing = preferencesRefreshing,
        preferencesFailed = preferencesFailed,
        systemService = systemService,
    )
