package com.aozijx.passly.app.database

import com.aozijx.passly.core.error.model.FileIOError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.port.DatabaseResetController
import com.aozijx.passly.domain.access.port.DatabaseResetSessionCoordinator
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseResetGatewayAdapterTest {
    @Test
    fun resetMapsNullableFailureToAppResult() =
        runTest {
            val controller = RecordingDatabaseResetController()
            var exclusiveResetCalls = 0
            val gateway =
                DatabaseResetGatewayAdapter(
                    controller,
                    DatabaseResetSessionCoordinator { operation ->
                        exclusiveResetCalls++
                        operation()
                    },
                )

            assertEquals(AppResult.Success(Unit), gateway.reset())
            assertEquals(1, exclusiveResetCalls)
            assertEquals(1, controller.resetCalls)

            controller.error = IOException("reset failed")
            val result = gateway.reset()
            assertTrue(result is AppResult.Failure)
            val failure = result as AppResult.Failure
            assertTrue(failure.error is FileIOError)
            assertEquals("IOException", failure.error.throwableType)
            assertEquals(2, exclusiveResetCalls)
            assertEquals(2, controller.resetCalls)
        }

    private class RecordingDatabaseResetController : DatabaseResetController {
        var error: Throwable? = null
        var resetCalls = 0

        override suspend fun reset(): Throwable? {
            resetCalls++
            return error
        }
    }
}
