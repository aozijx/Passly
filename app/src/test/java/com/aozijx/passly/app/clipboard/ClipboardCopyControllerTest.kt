package com.aozijx.passly.app.clipboard

import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.core.platform.clipboard.SecureClipboard
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.port.SecuritySettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardCopyControllerTest {

    @Test
    fun `enabled policy schedules configured delay`() = runTest {
        val clipboard = RecordingClipboard()
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = true, delay = 60)),
            secureClipboard = clipboard,
        )

        controller.copySensitive("secret")

        assertEquals("secret" to 60, clipboard.lastCopy)
    }

    @Test
    fun `disabled policy copies without scheduling clear`() = runTest {
        val clipboard = RecordingClipboard()
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = false, delay = 15)),
            secureClipboard = clipboard,
        )

        controller.copySensitive("secret")

        assertEquals("secret" to null, clipboard.lastCopy)
    }

    private fun policy(enabled: Boolean, delay: Int) = SecuritySettings(
        clipboardClearPolicy = ClipboardClearPolicy(enabled, delay),
    )

    private class FakeSecuritySettingsSource(settings: SecuritySettings) : SecuritySettingsSource {
        override val security: Flow<SecuritySettings> = flowOf(settings)
    }

    private class RecordingClipboard : SecureClipboard {
        var lastCopy: Pair<String, Int?>? = null

        override fun copySensitive(text: String, clearAfterSeconds: Int?) {
            lastCopy = text to clearAfterSeconds
        }

        override fun clearOwned(): ClipboardClearResult = ClipboardClearResult.Empty
    }
}
