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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardCopyControllerTest {

    @Test
    fun `enabled policy schedules configured delay`() = runTest {
        val clipboard = RecordingClipboard()
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = true, delay = 60)),
            secureClipboard = clipboard,
        )

        controller.writeSensitive("secret")

        assertEquals("secret" to 60, clipboard.lastCopy)
    }

    @Test
    fun `disabled policy copies without scheduling clear`() = runTest {
        val clipboard = RecordingClipboard()
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = false, delay = 15)),
            secureClipboard = clipboard,
        )

        controller.writeSensitive("secret")

        assertEquals("secret" to null, clipboard.lastCopy)
    }

    @Test
    fun `owned clipboard clear reports success`() {
        val clipboard = RecordingClipboard(ClipboardClearResult.Cleared)
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = true, delay = 30)),
            secureClipboard = clipboard,
        )

        assertTrue(controller.clearOwned())
    }

    @Test
    fun `unowned clipboard clear reports no change`() {
        val clipboard = RecordingClipboard(ClipboardClearResult.NotOwned)
        val controller = ClipboardCopyController(
            settingsSource = FakeSecuritySettingsSource(policy(enabled = true, delay = 30)),
            secureClipboard = clipboard,
        )

        assertFalse(controller.clearOwned())
    }
    private fun policy(enabled: Boolean, delay: Int) = SecuritySettings(
        clipboardClearPolicy = ClipboardClearPolicy(enabled, delay),
    )

    private class FakeSecuritySettingsSource(settings: SecuritySettings) : SecuritySettingsSource {
        override val security: Flow<SecuritySettings> = flowOf(settings)
    }

    private class RecordingClipboard(
        private val clearResult: ClipboardClearResult = ClipboardClearResult.Empty,
    ) : SecureClipboard {
        var lastCopy: Pair<String, Int?>? = null

        override fun copySensitive(text: String, clearAfterSeconds: Int?) {
            lastCopy = text to clearAfterSeconds
        }

        override fun clearOwned(): ClipboardClearResult = clearResult
    }
}
