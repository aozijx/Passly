package com.aozijx.passly.feature.settings.presentation.ui.autofill.model

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.settings.presentation.autofill.AutofillSourceState

data class AutofillSettingsUiModel(
    val enabled: Boolean,
    val presentation: AutofillPresentation,
    val credentialManagerEnabled: Boolean,
    val supportsCredentialManager: Boolean,
    val requireAuthentication: Boolean,
    val strictDomainMatching: Boolean,
    val systemService: AutofillSourceState<Boolean>,
)
