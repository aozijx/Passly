package com.aozijx.passly.feature.settings.presentation.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSettingsUiMapperTest {
    @Test
    fun mapsSettingsAndPlatformAvailabilityToUiModel() {
        val autofill = AutofillSettings(
                enabled = false,
                presentation = AutofillPresentation.BOTTOM_SHEET,
                savePromptsEnabled = false,
                maxSuggestions = AutofillSettings.MAX_SUGGESTIONS + 5,
            )

        val model = autofill.toAutofillSettingsUiModel(
            supportsCredentialManager = false,
            canEditPreferences = true,
            preferencesRefreshing = false,
            preferencesFailed = false,
            systemService = AutofillSourceState.Ready(true),
        )

        assertFalse(model.enabled)
        assertTrue(model.canEditPreferences)
        assertEquals(AutofillSourceState.Ready(true), model.systemService)
        assertFalse(model.supportsCredentialManager)
        assertFalse(model.savePromptsEnabled)
        assertEquals(AutofillPresentation.BOTTOM_SHEET, model.presentation)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestions)
        assertEquals(AutofillSettings.MIN_SUGGESTIONS, model.minSuggestions)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestionsLimit)
    }

}
