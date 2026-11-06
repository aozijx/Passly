package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AutofillSettingsUiMapperTest {
    @Test
    fun mapsSettingsAndPlatformAvailabilityToUiModel() {
        val autofill =
            AutofillSettings(
                enabled = false,
                presentation = AutofillPresentation.BOTTOM_SHEET,
                strictDomainMatching = false,
            )

        val model =
            autofill.toAutofillSettingsUiModel(
                supportsCredentialManager = false,
                canEditPreferences = true,
                preferencesRefreshing = false,
                preferencesFailed = false,
                systemService = AutofillSourceState.Ready(true),
            )

        assertFalse(model.enabled)
        assertEquals(true, model.canEditPreferences)
        assertEquals(AutofillSourceState.Ready(true), model.systemService)
        assertFalse(model.supportsCredentialManager)
        assertFalse(model.strictDomainMatching)
        assertEquals(AutofillPresentation.BOTTOM_SHEET, model.presentation)
    }
}
