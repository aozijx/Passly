package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.settings.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearanceLabelsTest {
    @Test
    fun languageKeysRoundTripAcrossFeatureUiBoundary() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, appLanguageFromKey(language.name))
        }
    }

    @Test
    fun unknownLanguageKeyIsRejected() {
        assertNull(appLanguageFromKey("UNKNOWN_LANGUAGE"))
    }
}
