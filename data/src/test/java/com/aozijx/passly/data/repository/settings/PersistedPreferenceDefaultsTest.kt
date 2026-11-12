package com.aozijx.passly.data.repository.settings

import com.aozijx.passly.data.preferences.proto.AppPreferences
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.MessageLevel
import com.aozijx.passly.domain.settings.model.MessageTopic
import com.aozijx.passly.domain.settings.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistedPreferenceDefaultsTest {
    @Test
    fun `appearance defaults come from proto`() {
        val settings = readAppearance(AppPreferences.getDefaultInstance().appearance)

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.isDynamicColor)
        assertEquals("default", settings.themeKey)
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertEquals(FontFamilyMode.APP_BUNDLED, settings.fontFamily)
    }

    @Test
    fun `interface defaults come from proto`() {
        val settings = readInterface(AppPreferences.getDefaultInstance().interfacePrefs)

        assertEquals(false, settings.hideSystemBars)
        assertEquals(false, settings.collapseTopBarOnScroll)
        assertTrue(settings.collapseQuickFilterBarOnScroll)
        assertEquals(16f, settings.appCornerRadiusDp)
    }

    @Test
    fun `message defaults come from proto and include every topic`() {
        val settings = decodeMessageSettings(AppPreferences.getDefaultInstance().message)

        assertTrue(settings.optionalMessagesEnabled)
        assertTrue(settings.systemNotificationsEnabled)
        assertEquals(MessageTopic.entries.toSet(), settings.topicSettings.keys)
        assertEquals(MessageLevel.INFO, settings.topic(MessageTopic.CLIPBOARD).minimumLevel)
        assertEquals(MessageLevel.WARNING, settings.topic(MessageTopic.SECURITY).minimumLevel)
        assertEquals(MessageLevel.ERROR, settings.topic(MessageTopic.DATABASE).minimumLevel)
    }

    @Test
    fun `nested autofill defaults come from proto`() {
        val settings = readAutofill(AppPreferences.getDefaultInstance().interaction.autofill)

        assertEquals(AutofillPresentation.FLOATING, settings.presentation)
    }
}
