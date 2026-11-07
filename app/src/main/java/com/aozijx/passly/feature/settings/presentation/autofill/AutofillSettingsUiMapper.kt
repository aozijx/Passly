package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.feature.settings.presentation.ui.autofill.model.AutofillSettingsUiModel

fun AutofillSettings.toAutofillSettingsUiModel(
    supportsCredentialManager: Boolean,
    systemService: AutofillSourceState<Boolean>,
): AutofillSettingsUiModel =
    AutofillSettingsUiModel(
        enabled = enabled,
        presentation = presentation,
        credentialManagerEnabled = credentialManagerEnabled,
        supportsCredentialManager = supportsCredentialManager,
        requireAuthentication = requireAuthentication,
        strictDomainMatching = strictDomainMatching,
        systemService = systemService,
    )
