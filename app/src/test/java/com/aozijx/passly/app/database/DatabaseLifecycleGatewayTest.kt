package com.aozijx.passly.app.database

import com.aozijx.passly.data.local.database.port.DatabaseController
import com.aozijx.passly.data.local.database.port.DatabaseQuarantineResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseLifecycleGatewayTest {
    @Test
    fun initializeMapsControllerReadinessToExplicitResult() = runTest {
        val controller = FakeController()
        val gateway = DatabaseLifecycleGateway(controller)

        assertEquals(DatabaseLifecycleResult.Ready, gateway.initialize())

        val failure = IllegalStateException("open failed")
        controller.preWarmError = failure
        val result = gateway.initialize()
        assertSame(failure, (result as DatabaseLifecycleResult.Failure).cause)
    }

    @Test
    fun quarantinePreservesRecoveryIdAndCapturesThrownFailure() = runTest {
        val controller = FakeController().apply {
            quarantineResult = DatabaseQuarantineResult(recoveryId = "recovery-1")
        }
        val gateway = DatabaseLifecycleGateway(controller)

        assertEquals(
            DatabaseLifecycleResult.Reinitialized("recovery-1"),
            gateway.quarantineAndReinitialize(),
        )

        val failure = IllegalArgumentException("quarantine failed")
        controller.quarantineFailure = failure
        val result = gateway.quarantineAndReinitialize()
        assertSame(failure, (result as DatabaseLifecycleResult.Failure).cause)
    }

    @Test
    fun clearReturnsReadyOrFailureInsteadOfNullableThrowable() = runTest {
        val controller = FakeController()
        val gateway = DatabaseLifecycleGateway(controller)

        assertEquals(DatabaseLifecycleResult.Ready, gateway.clearAndReinitialize())
        controller.clearError = IllegalStateException("clear failed")
        assertTrue(gateway.clearAndReinitialize() is DatabaseLifecycleResult.Failure)
    }

    private class FakeController : DatabaseController {
        var preWarmError: Throwable? = null
        var retryError: Throwable? = null
        var clearError: Throwable? = null
        var quarantineResult = DatabaseQuarantineResult()
        var quarantineFailure: Throwable? = null

        override suspend fun preWarm() = preWarmError
        override suspend fun retry() = retryError
        override suspend fun clearAndReinitialize() = clearError
        override suspend fun quarantineAndReinitialize(): DatabaseQuarantineResult {
            quarantineFailure?.let { throw it }
            return quarantineResult
        }
        override suspend fun close() = Unit
    }
}
