package com.aozijx.passly.feature.autofill.accessibility

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityFillLaunchTest {
    @Test
    fun `cancelling launch clears captured request and trace then propagates`() = runTest {
        var requestClears = 0
        var traceDiscards = 0
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            launchAccessibilityFill(
                token = "request-token",
                discardTrace = { traceDiscards++ },
                clearCaptured = { requestClears++ },
                onFailure = { error("Cancellation must not become an ordinary failure") },
            ) {
                awaitCancellation()
            }
        }

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

        launchAccessibilityFill(
            token = "request-token",
            discardTrace = { traceDiscards++ },
            clearCaptured = { requestClears++ },
            onFailure = { reported = it },
        ) {
            throw IllegalStateException("platform launch failed")
        }

        assertEquals(1, requestClears)
        assertEquals(1, traceDiscards)
        assertEquals("platform launch failed", reported?.message)
    }
}
