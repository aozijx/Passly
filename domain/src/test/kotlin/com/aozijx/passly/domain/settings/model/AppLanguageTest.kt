package com.aozijx.passly.domain.settings.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun localeTags_roundTripSupportedLanguages() {
        assertEquals("zh-CN", AppLanguage.ZH.applicationLocaleTags)
        assertEquals("en", AppLanguage.EN.applicationLocaleTags)

        listOf(AppLanguage.ZH, AppLanguage.EN).forEach { language ->
            assertEquals(language, AppLanguage.fromLanguageTag(language.storageTag))
        }
    }

    @Test
    fun localeTags_mapRegionalVariantByLanguage() {
        assertEquals(AppLanguage.EN, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLanguageTag("system"))
    }
}
