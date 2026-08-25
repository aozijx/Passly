package com.aozijx.passly.app.clipboard

import com.aozijx.passly.core.platform.clipboard.ClipboardClearResult
import com.aozijx.passly.core.platform.clipboard.SecureClipboard
import com.aozijx.passly.domain.settings.model.AppSettingsSnapshot
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.BackupSettings
import com.aozijx.passly.domain.settings.model.ClipboardClearPolicy
import com.aozijx.passly.domain.settings.model.InteractionSettings
import com.aozijx.passly.domain.settings.model.InterfaceSettings
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.model.SecuritySettings
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
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
            settingsRepository = FakeSettingsRepository(policy(enabled = true, delay = 60)),
            secureClipboard = clipboard,
        )

        controller.copySensitive("secret")

        assertEquals("secret" to 60, clipboard.lastCopy)
    }

    @Test
    fun `disabled policy copies without scheduling clear`() = runTest {
        val clipboard = RecordingClipboard()
        val controller = ClipboardCopyController(
            settingsRepository = FakeSettingsRepository(policy(enabled = false, delay = 15)),
            secureClipboard = clipboard,
        )

        controller.copySensitive("secret")

        assertEquals("secret" to null, clipboard.lastCopy)
    }

    private fun policy(enabled: Boolean, delay: Int) = AppSettingsSnapshot(
        appearance = AppearanceSettings(),
        interfacePrefs = InterfaceSettings(),
        security = SecuritySettings(
            clipboardClearPolicy = ClipboardClearPolicy(enabled, delay)
        ),
        interaction = InteractionSettings(),
        messages = MessageSettings(),
        vault = LibraryViewSettings(),
        backup = BackupSettings(),
    )

    private class FakeSettingsRepository(snapshot: AppSettingsSnapshot) : AppSettingsRepository {
        override val settings: Flow<AppSettingsSnapshot> = flowOf(snapshot)
        override val lockTimeout: Flow<Long> = flowOf(snapshot.security.lockTimeout)
        override val isLockOnBackground: Flow<Boolean> =
            flowOf(snapshot.security.isLockOnBackground)

        override suspend fun update(command: SettingsCommand) = Unit
    }

    private class RecordingClipboard : SecureClipboard {
        var lastCopy: Pair<String, Int?>? = null

        override fun copySensitive(text: String, clearAfterSeconds: Int?) {
            lastCopy = text to clearAfterSeconds
        }

        override fun clearOwned(): ClipboardClearResult = ClipboardClearResult.Empty
    }
}
