package com.aozijx.passly.feature.autofill.accessibility

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityFillLaunchTest {
    @Test
    fun `cancellation before launch body starts still clears captured request and trace`() = runTest {
        var requestClears = 0
        var traceDiscards = 0
        val job = launchAccessibilityFill(
            scope = this,
            token = "request-token",
            discardTrace = { traceDiscards++ },
            clearCaptured = { requestClears++ },
            onFailure = { error("Cancellation must not become an ordinary failure") },
        ) {
            awaitCancellation()
        }

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(1, requestClears)
        assertEquals(1, traceDiscards)
    }

    @Test
    fun `cancelling started launch clears captured request and trace then propagates`() = runTest {
        var requestClears = 0
        var traceDiscards = 0
        val job = launchAccessibilityFill(
            scope = this,
            token = "request-token",
            discardTrace = { traceDiscards++ },
            clearCaptured = { requestClears++ },
            onFailure = { error("Cancellation must not become an ordinary failure") },
        ) {
            awaitCancellation()
        }
        runCurrent()

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(1, requestClears)
        assertEquals(1, traceDiscards)
    }

    @Test
    fun `ordinary launch failure clears captured request and reports failure`() = runTest {
        var requestClears = 0
        var traceDiscards = 0
        var reported: Exception? = null
        val job = launchAccessibilityFill(
            scope = this,
            token = "request-token",
            discardTrace = { traceDiscards++ },
            clearCaptured = { requestClears++ },
            onFailure = { reported = it },
        ) {
            throw IllegalStateException("platform launch failed")
        }
        runCurrent()

        assertEquals(1, requestClears)
        assertEquals(1, traceDiscards)
        assertEquals("platform launch failed", reported?.message)
    }
}
