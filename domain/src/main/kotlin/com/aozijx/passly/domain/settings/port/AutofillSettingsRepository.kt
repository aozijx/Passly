package com.aozijx.passly.domain.settings.port

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import kotlinx.coroutines.flow.Flow

interface AutofillSettingsSource {
    val autofillSettings: Flow<AutofillSettings>
}

interface AutofillSettingsRepository : AutofillSettingsSource {
    suspend fun setAutofillEnabled(enabled: Boolean)

    suspend fun setAutofillPresentation(presentation: AutofillPresentation)

    suspend fun setCredentialManagerEnabled(enabled: Boolean)

    suspend fun setAutofillAuthenticationRequired(required: Boolean)

    suspend fun setAutofillStrictDomainMatching(enabled: Boolean)
}
