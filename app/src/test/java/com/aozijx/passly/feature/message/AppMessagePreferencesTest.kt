package com.aozijx.passly.feature.message

import com.aozijx.passly.core.message.AppMessageCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMessagePreferencesTest {
    @Test
    fun masterSwitchDisablesEveryMessageCategory() {
        val preferences = AppMessagePreferences(showGeneral = false)

        AppMessageCategory.entries.forEach { category ->
            assertFalse(preferences.allows(category))
        }
    }

    @Test
    fun categorySwitchOnlyDisablesItsOwnMessages() {
        val preferences = AppMessagePreferences(showIconDownloads = false)

        assertTrue(preferences.allows(AppMessageCategory.GENERAL))
        assertFalse(preferences.allows(AppMessageCategory.ICON_DOWNLOAD))
        assertTrue(preferences.allows(AppMessageCategory.CLIPBOARD_CLEAR))
        assertTrue(preferences.allows(AppMessageCategory.APP_CLOSE))
    }
}
