package com.aozijx.passly.feature.autofill.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillServiceCleanupTest {
    @Test
    fun `cleanup completes after service scope is cancelled and callback is suppressed`() = runTest {
        val ownerJob = Job()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(ownerJob + dispatcher)
        val cleanupStarted = CompletableDeferred<Unit>()
        val continueCleanup = CompletableDeferred<Unit>()
        var cleanupFinished = false
        var callbackInvoked = false

        val cleanupJob = launchAutofillServiceCleanup(
            scope = scope,
            dispatcher = dispatcher,
            cleanup = {
                cleanupStarted.complete(Unit)
                continueCleanup.await()
                cleanupFinished = true
            },
            afterCleanup = { callbackInvoked = true },
        )

        runCurrent()
        assertTrue(cleanupStarted.isCompleted)
        ownerJob.cancel()
        continueCleanup.complete(Unit)
        runCurrent()

        assertTrue(cleanupFinished)
        assertFalse(callbackInvoked)
        assertTrue(cleanupJob.isCancelled)
    }
}
