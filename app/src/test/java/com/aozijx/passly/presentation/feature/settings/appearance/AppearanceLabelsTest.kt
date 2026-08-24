package com.aozijx.passly.presentation.feature.settings.appearance

import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode
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

    @Test
    fun appearanceEnumsRoundTripAcrossFeatureUiBoundary() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, mode.toUiModel().toDomainModel())
        }
        FontFamilyMode.entries.forEach { mode ->
            assertEquals(mode, mode.toUiModel().toDomainModel())
        }
    }
}
