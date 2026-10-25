package com.aozijx.passly.presentation.feature.settings.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSettingsUiMapperTest {
    @Test
    fun mapsSettingsAndPlatformAvailabilityToUiModel() {
        val state = AutofillSettingsUiState.Ready(
            autofill = AutofillSettings(
                enabled = false,
                presentation = AutofillPresentation.BOTTOM_SHEET,
                savePromptsEnabled = false,
                maxSuggestions = AutofillSettings.MAX_SUGGESTIONS + 5,
            ),
            isSystemAutofillEnabled = true,
        )

        val model = state.toAutofillSettingsUiModel(supportsCredentialManager = false)

        assertFalse(model.enabled)
        assertTrue(model.isSystemServiceEnabled)
        assertFalse(model.supportsCredentialManager)
        assertFalse(model.savePromptsEnabled)
        assertEquals(AutofillPresentation.BOTTOM_SHEET, model.presentation)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestions)
        assertEquals(AutofillSettings.MIN_SUGGESTIONS, model.minSuggestions)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestionsLimit)
    }

}
