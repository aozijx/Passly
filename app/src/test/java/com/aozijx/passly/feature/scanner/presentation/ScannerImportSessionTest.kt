package com.aozijx.passly.feature.scanner.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerImportSessionTest {
    @Test
    fun `confirmation waits for persistence and ignores repeated taps`() = runTest {
        val session = ScannerImportSession()
        val stored = CompletableDeferred<ScannerImportHandlingResult>()
        val outcomes = mutableListOf<ScannerImportOutcome>()
        var calls = 0
        val first = launch {
            outcomes += session.confirm { calls++; stored.await() }
        }
        runCurrent()
        assertEquals(emptyList<ScannerImportOutcome>(), outcomes)
        assertEquals(ScannerImportOutcome.Ignored, session.confirm { calls++; ScannerImportHandlingResult.Applied })
        stored.complete(ScannerImportHandlingResult.Applied)
        first.join()
        assertEquals(listOf(ScannerImportOutcome.Applied), outcomes)
        assertEquals(ScannerImportOutcome.Ignored, session.confirm { calls++; ScannerImportHandlingResult.Applied })
        assertEquals(1, calls)
    }

    @Test
    fun `rejected persistence keeps result available for retry`() = runTest {
        val session = ScannerImportSession()
        assertEquals(ScannerImportOutcome.Rejected, session.confirm { ScannerImportHandlingResult.Rejected })
        assertEquals(ScannerImportOutcome.Applied, session.confirm { ScannerImportHandlingResult.Applied })
    }

    @Test
    fun `closed result ignores late persistence acknowledgement`() = runTest {
        val session = ScannerImportSession()
        val stored = CompletableDeferred<ScannerImportHandlingResult>()
        val result = async { session.confirm { stored.await() } }
        runCurrent()
        session.close()
        stored.complete(ScannerImportHandlingResult.Applied)
        assertEquals(ScannerImportOutcome.Ignored, result.await())
    }

    @Test
    fun `cancelled confirmation publishes nothing and releases attempt`() = runTest {
        val session = ScannerImportSession()
        val pending = CompletableDeferred<ScannerImportHandlingResult>()
        val outcomes = mutableListOf<ScannerImportOutcome>()
        val first = launch { outcomes += session.confirm { pending.await() } }
        runCurrent()
        first.cancelAndJoin()
        assertEquals(emptyList<ScannerImportOutcome>(), outcomes)
        assertEquals(ScannerImportOutcome.Applied, session.confirm { ScannerImportHandlingResult.Applied })
    }
}
