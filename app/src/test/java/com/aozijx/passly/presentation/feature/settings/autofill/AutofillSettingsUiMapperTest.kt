package com.aozijx.passly.presentation.feature.settings.autofill

import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.AutofillSettings
import com.aozijx.passly.presentation.ui.settings.autofill.model.AutofillPresentationUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillSettingsUiMapperTest {
    @Test
    fun mapsSettingsAndPlatformAvailabilityToUiModel() {
        val state = AutofillSettingsUiState(
            autofill = AutofillSettings(
                enabled = false,
                presentation = AutofillPresentation.BOTTOM_SHEET,
                maxSuggestions = AutofillSettings.MAX_SUGGESTIONS + 5,
            ),
            isSystemAutofillEnabled = true,
        )

        val model = state.toAutofillSettingsUiModel(supportsCredentialManager = false)

        assertFalse(model.enabled)
        assertTrue(model.isSystemServiceEnabled)
        assertFalse(model.supportsCredentialManager)
        assertEquals(AutofillPresentationUiModel.BOTTOM_SHEET, model.presentation)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestions)
        assertEquals(AutofillSettings.MIN_SUGGESTIONS, model.minSuggestions)
        assertEquals(AutofillSettings.MAX_SUGGESTIONS, model.maxSuggestionsLimit)
    }

    @Test
    fun presentationRoundTripsAcrossFeatureUiBoundary() {
        AutofillPresentation.entries.forEach { presentation ->
            assertEquals(presentation, presentation.toUiModel().toDomainModel())
        }
    }
}
