package com.aozijx.passly.feature.scanner.presentation.result

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerResultSessionTest {
    @Test
    fun `confirmation waits for persistence and ignores repeated taps`() =
        runTest {
            val session = ScannerResultSession()
            val stored = CompletableDeferred<ScannerResultHandling>()
            val outcomes = mutableListOf<ScannerResultOutcome>()
            var calls = 0
            val first =
                launch {
                    outcomes +=
                        session.confirm {
                            calls++
                            stored.await()
                        }
                }
            runCurrent()
            assertEquals(emptyList<ScannerResultOutcome>(), outcomes)
            assertEquals(
                ScannerResultOutcome.Ignored,
                session.confirm {
                    calls++
                    ScannerResultHandling.Handled
                },
            )
            stored.complete(ScannerResultHandling.Handled)
            first.join()
            assertEquals(listOf(ScannerResultOutcome.Handled), outcomes)
            assertEquals(
                ScannerResultOutcome.Ignored,
                session.confirm {
                    calls++
                    ScannerResultHandling.Handled
                },
            )
            assertEquals(1, calls)
        }

    @Test
    fun `rejected persistence keeps result available for retry`() =
        runTest {
            val session = ScannerResultSession()
            assertEquals(ScannerResultOutcome.Rejected, session.confirm { ScannerResultHandling.Rejected })
            assertEquals(ScannerResultOutcome.Handled, session.confirm { ScannerResultHandling.Handled })
        }

    @Test
    fun `closed result ignores late persistence acknowledgement`() =
        runTest {
            val session = ScannerResultSession()
            val stored = CompletableDeferred<ScannerResultHandling>()
            val result = async { session.confirm { stored.await() } }
            runCurrent()
            session.close()
            stored.complete(ScannerResultHandling.Handled)
            assertEquals(ScannerResultOutcome.Ignored, result.await())
        }

    @Test
    fun `cancelled confirmation publishes nothing and releases attempt`() =
        runTest {
            val session = ScannerResultSession()
            val pending = CompletableDeferred<ScannerResultHandling>()
            val outcomes = mutableListOf<ScannerResultOutcome>()
            val first = launch { outcomes += session.confirm { pending.await() } }
            runCurrent()
            first.cancelAndJoin()
            assertEquals(emptyList<ScannerResultOutcome>(), outcomes)
            assertEquals(ScannerResultOutcome.Handled, session.confirm { ScannerResultHandling.Handled })
        }
}
