package com.aozijx.passly.presentation.feature.settings.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillSettingsUiModel

fun AutofillSettingsUiState.toAutofillSettingsUiModel(
    supportsCredentialManager: Boolean,
): AutofillSettingsUiModel = AutofillSettingsUiModel(
    enabled = autofill.enabled,
    presentation = autofill.presentation.toUiModel(),
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

fun AutofillPresentation.toUiModel(): AutofillPresentationUiModel = when (this) {
    AutofillPresentation.SYSTEM_INLINE -> AutofillPresentationUiModel.SYSTEM_INLINE
    AutofillPresentation.BOTTOM_SHEET -> AutofillPresentationUiModel.BOTTOM_SHEET
}

fun AutofillPresentationUiModel.toDomainModel(): AutofillPresentation = when (this) {
    AutofillPresentationUiModel.SYSTEM_INLINE -> AutofillPresentation.SYSTEM_INLINE
    AutofillPresentationUiModel.BOTTOM_SHEET -> AutofillPresentation.BOTTOM_SHEET
}
