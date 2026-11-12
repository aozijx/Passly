package com.aozijx.passly.app.message.runtime

import com.aozijx.passly.app.message.contract.MessageSettingsSnapshot
import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.domain.settings.model.MessageSettings
import com.aozijx.passly.domain.settings.port.MessageSettingsSource
import com.aozijx.passly.testing.sampleMessageSettings
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMessageSettingsSnapshotProviderTest {
    @Test
    fun `first read failure stays unavailable then recovers`() =
        runTest {
            var attempts = 0
            val source =
                object : MessageSettingsSource {
                    override val messages =
                        flow<MessageSettings> {
                            attempts++
                            if (attempts == 1) throw IOException("temporary read failure")
                            emit(sampleMessageSettings(systemNotificationsEnabled = true))
                            kotlinx.coroutines.awaitCancellation()
                        }
                }
            val provider =
                DefaultMessageSettingsSnapshotProvider.create(
                    settingsSource = source,
                    scope = backgroundScope,
                    retryDelayMillis = 100,
                )

            runCurrent()
            assertEquals(MessageSettingsSnapshot.Failed(ReadFailure.IO), provider.current())
            assertEquals(1, attempts)

            advanceTimeBy(100)
            runCurrent()

            assertTrue(attempts >= 2)
            assertTrue((provider.current() as MessageSettingsSnapshot.Ready).value.systemNotificationsEnabled)
        }

    @Test
    fun `snapshot is unavailable until persisted settings emit`() {
        val source =
            object : MessageSettingsSource {
                override val messages = MutableSharedFlow<MessageSettings>()
            }

        val provider = DefaultMessageSettingsSnapshotProvider(source)

        assertSame(MessageSettingsSnapshot.Loading, provider.current())
    }

    @Test
    fun `settings source failure clears stale snapshot and retries subscription`() =
        runTest {
            var attempts = 0
            val source =
                object : MessageSettingsSource {
                    override val messages =
                        flow<MessageSettings> {
                            attempts++
                            if (attempts == 1) {
                                emit(sampleMessageSettings(systemNotificationsEnabled = false))
                                throw IOException("temporary read failure")
                            }
                            emit(sampleMessageSettings(systemNotificationsEnabled = true))
                            kotlinx.coroutines.awaitCancellation()
                        }
                }
            val provider =
                DefaultMessageSettingsSnapshotProvider.create(
                    settingsSource = source,
                    scope = backgroundScope,
                    retryDelayMillis = 100,
                )

            runCurrent()
            assertEquals(MessageSettingsSnapshot.Failed(ReadFailure.IO), provider.current())
            assertEquals(1, attempts)

            advanceTimeBy(100)
            runCurrent()

            assertTrue(attempts >= 2)
            val ready = provider.current() as MessageSettingsSnapshot.Ready
            assertTrue(ready.value.systemNotificationsEnabled)
        }

    @Test
    fun `cancelling process owner does not retry or materialize a failure`() =
        runTest {
            var attempts = 0
            val source =
                object : MessageSettingsSource {
                    override val messages =
                        flow<MessageSettings> {
                            attempts++
                            kotlinx.coroutines.awaitCancellation()
                        }
                }
            val ownerJob = SupervisorJob()
            val scope = CoroutineScope(backgroundScope.coroutineContext + ownerJob)
            val provider =
                DefaultMessageSettingsSnapshotProvider.create(
                    settingsSource = source,
                    scope = scope,
                    retryDelayMillis = 100,
                )

            runCurrent()
            assertEquals(1, attempts)
            ownerJob.cancel()
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(1, attempts)
            assertSame(MessageSettingsSnapshot.Loading, provider.current())
        }
}
