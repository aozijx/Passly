package com.aozijx.passly.app.settings

import com.aozijx.passly.app.settings.ReadFailure
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.settings.port.CorruptSettingsDataException
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecoverableReadTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `io failure is visible then cold source is retried`() =
        runTest {
            var starts = 0
            val read =
                RecoverableRead(
                    source =
                        flow {
                            starts++
                            if (starts == 1) throw IOException("unavailable")
                            emit("recovered")
                            awaitCancellation()
                        },
                    scope = backgroundScope,
                    retryDelayMillis = 1_000,
                )
            backgroundScope.launch { read.state.collect {} }
            runCurrent()

            assertEquals(RecoverableReadState.Failed(ReadFailure.IO), read.state.value)
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(RecoverableReadState.Ready("recovered"), read.state.value)
        }

    @Test
    fun `corruption waits for an explicit retry`() =
        runTest {
            var starts = 0
            val read =
                RecoverableRead(
                    source =
                        flow {
                            starts++
                            if (starts == 1) throw CorruptSettingsDataException(IllegalStateException("corrupted"))
                            emit("recovered")
                            awaitCancellation()
                        },
                    scope = backgroundScope,
                    retryDelayMillis = 1_000,
                )
            backgroundScope.launch { read.state.collect {} }
            runCurrent()

            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(1, starts)
            assertEquals(RecoverableReadState.Failed(ReadFailure.CORRUPT_DATA), read.state.value)

            read.retry()
            runCurrent()

            assertEquals(2, starts)
            assertEquals(RecoverableReadState.Ready("recovered"), read.state.value)
        }

    @Test
    fun `explicit retry from another owner wakes shared runtime read`() =
        runTest {
            val retrySignal = SettingsReadRetrySignal()
            var starts = 0
            val runtimeRead =
                RecoverableRead(
                    source =
                        flow {
                            starts++
                            if (starts == 1) throw CorruptSettingsDataException(IllegalStateException("corrupted"))
                            emit("recovered")
                            awaitCancellation()
                        },
                    scope = backgroundScope,
                    sharedRetrySignal = retrySignal,
                )
            val pageRead =
                RecoverableRead(
                    source = flow<String> { awaitCancellation() },
                    scope = backgroundScope,
                    sharedRetrySignal = retrySignal,
                )
            backgroundScope.launch { runtimeRead.state.collect {} }
            backgroundScope.launch { pageRead.state.collect {} }
            runCurrent()

            assertEquals(RecoverableReadState.Failed(ReadFailure.CORRUPT_DATA), runtimeRead.state.value)
            pageRead.retry()
            runCurrent()

            assertEquals(2, starts)
            assertEquals(RecoverableReadState.Ready("recovered"), runtimeRead.state.value)
        }

    @Test
    fun `shared retry during a source attempt is not lost before corruption is reported`() =
        runTest {
            val retrySignal = SettingsReadRetrySignal()
            val releaseFirstAttempt = CompletableDeferred<Unit>()
            var starts = 0
            val runtimeRead =
                RecoverableRead(
                    source =
                        flow {
                            starts++
                            if (starts == 1) {
                                releaseFirstAttempt.await()
                                throw CorruptSettingsDataException(IllegalStateException("corrupted"))
                            }
                            emit("recovered")
                            awaitCancellation()
                        },
                    scope = backgroundScope,
                    sharedRetrySignal = retrySignal,
                )
            val pageRead =
                RecoverableRead(
                    source = flow<String> { awaitCancellation() },
                    scope = backgroundScope,
                    sharedRetrySignal = retrySignal,
                )
            backgroundScope.launch { runtimeRead.state.collect {} }
            backgroundScope.launch { pageRead.state.collect {} }
            runCurrent()

            pageRead.retry()
            releaseFirstAttempt.complete(Unit)
            runCurrent()

            assertEquals(2, starts)
            assertEquals(RecoverableReadState.Ready("recovered"), runtimeRead.state.value)
        }

    @Test
    fun `cancellation is not converted to a failure`() =
        runTest {
            val read =
                RecoverableRead<Unit>(
                    source = flow<Unit> { awaitCancellation() },
                    scope = backgroundScope,
                    retryDelayMillis = 1_000,
                )
            val collector = backgroundScope.launch { read.state.collect {} }
            runCurrent()
            collector.cancel()
            advanceUntilIdle()

            assertEquals(RecoverableReadState.Loading, read.state.value)
        }
}
