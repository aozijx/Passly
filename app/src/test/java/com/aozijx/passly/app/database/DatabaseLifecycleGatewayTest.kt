package com.aozijx.passly.app.database

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.port.DatabaseController
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
    fun resetReturnsReadyOrFailureInsteadOfNullableThrowable() = runTest {
        val controller = FakeController()
        val gateway = DatabaseLifecycleGateway(controller)

        assertTrue(gateway.reset() is AppResult.Success)
        controller.resetError = IllegalStateException("reset failed")
        assertTrue(gateway.reset() is AppResult.Failure)
    }

    private class FakeController : DatabaseController {
        var preWarmError: Throwable? = null
        var retryError: Throwable? = null
        var resetError: Throwable? = null

        override suspend fun preWarm() = preWarmError
        override suspend fun retry() = retryError
        override suspend fun reset() = resetError
        override suspend fun close() = Unit
    }
}
