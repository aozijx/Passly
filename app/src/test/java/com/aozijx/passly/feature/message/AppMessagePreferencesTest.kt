package com.aozijx.passly.feature.message

import com.aozijx.passly.core.message.AppMessage
import com.aozijx.passly.core.message.AppMessageCategory
import com.aozijx.passly.core.message.AppMessagePresentation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMessagePreferencesTest {
    @Test
    fun statusBarSwitchDoesNotDisableToastMessages() {
        val preferences = AppMessagePreferences(statusBarNotificationsEnabled = false)
        val statusBarMessage = AppMessage(
            text = "download",
            category = AppMessageCategory.ICON_DOWNLOAD,
            presentation = AppMessagePresentation.STATUS_BAR
        )
        val toastMessage = AppMessage(
            text = "clipboard",
            category = AppMessageCategory.CLIPBOARD_CLEAR
        )

        assertFalse(preferences.allows(statusBarMessage))
        assertTrue(preferences.allows(toastMessage))
    }

    @Test
    fun categorySwitchOnlyDisablesItsOwnMessages() {
        val preferences = AppMessagePreferences(iconDownloadNotificationsEnabled = false)

        assertFalse(
            preferences.allows(
                AppMessage(
                    "icon",
                    AppMessageCategory.ICON_DOWNLOAD,
                    presentation = AppMessagePresentation.STATUS_BAR
                )
            )
        )
        assertTrue(preferences.allows(AppMessage("general")))
        assertTrue(
            preferences.allows(AppMessage("clipboard", AppMessageCategory.CLIPBOARD_CLEAR))
        )
        assertTrue(preferences.allows(AppMessage("close", AppMessageCategory.APP_CLOSE)))
    }

    @Test
    fun securityToastSwitchesOnlyControlTheirOwnCategory() {
        val preferences = AppMessagePreferences(
            clipboardClearToastsEnabled = false,
            appCloseToastsEnabled = true
        )

        assertFalse(
            preferences.allows(AppMessage("clipboard", AppMessageCategory.CLIPBOARD_CLEAR))
        )
        assertTrue(preferences.allows(AppMessage("close", AppMessageCategory.APP_CLOSE)))
    }
}
