package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageSettingsSnapshotProviderTest {

    @Test
    fun `snapshot is unavailable until persisted settings emit`() {
        val source = object : MessageSettingsSource {
            override val messages = MutableSharedFlow<MessageSettings>()
        }

        val provider = DefaultMessageSettingsSnapshotProvider(source)

        assertSame(MessageSettingsSnapshot.Loading, provider.current())
    }

    @Test
    fun `settings source failure clears stale snapshot and retries subscription`() = runTest {
        var attempts = 0
        val source = object : MessageSettingsSource {
            override val messages = flow {
                attempts++
                if (attempts == 1) {
                    emit(MessageSettings(systemNotificationsEnabled = false))
                    throw IOException("temporary read failure")
                }
                emit(MessageSettings(systemNotificationsEnabled = true))
                kotlinx.coroutines.awaitCancellation()
            }
        }
        val provider = DefaultMessageSettingsSnapshotProvider.create(
            settingsSource = source,
            scope = backgroundScope,
            retryDelayMillis = 100,
        )

        runCurrent()
        assertSame(MessageSettingsSnapshot.Loading, provider.current())
        assertEquals(1, attempts)

        advanceTimeBy(100)
        runCurrent()

        assertTrue(attempts >= 2)
        val ready = provider.current() as MessageSettingsSnapshot.Ready
        assertTrue(ready.value.systemNotificationsEnabled)
    }
}
