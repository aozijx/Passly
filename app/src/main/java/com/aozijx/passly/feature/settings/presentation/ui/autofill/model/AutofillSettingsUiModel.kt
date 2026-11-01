package com.aozijx.passly.feature.settings.presentation.ui.autofill.model

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSourceState

data class AutofillSettingsUiModel(
    val enabled: Boolean,
    val presentation: AutofillPresentation,
    val credentialManagerEnabled: Boolean,
    val supportsCredentialManager: Boolean,
    val requireAuthentication: Boolean,
    val includeOtp: Boolean,
    val savePromptsEnabled: Boolean,
    val allowUnmatchedSuggestions: Boolean,
    val maxSuggestions: Int,
    val minSuggestions: Int,
    val maxSuggestionsLimit: Int,
    val canEditPreferences: Boolean,
    val preferencesRefreshing: Boolean,
    val preferencesFailed: Boolean,
    val systemService: AutofillSourceState<Boolean>,
)
