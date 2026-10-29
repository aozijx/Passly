package com.aozijx.passly.feature.autofill.legacy.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyInteractionExpiryTest {
    @Test
    fun `disconnect expiry finishes when service scope is cancelled`() = runTest {
        val ownerJob = Job()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(ownerJob + dispatcher)
        val cleanupStarted = CompletableDeferred<Unit>()
        val continueCleanup = CompletableDeferred<Unit>()
        var cleanupFinished = false

        launchLegacyInteractionExpiry(scope, dispatcher) {
            cleanupStarted.complete(Unit)
            continueCleanup.await()
            cleanupFinished = true
        }

        ownerJob.cancel()
        continueCleanup.complete(Unit)
        runCurrent()

        assertTrue(cleanupStarted.isCompleted)
        assertTrue(cleanupFinished)
    }
}
