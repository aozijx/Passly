package com.aozijx.passly.app.database

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.data.local.database.port.DatabaseResetController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseResetGatewayAdapterTest {
    @Test
    fun resetMapsNullableFailureToAppResult() = runTest {
        val controller = RecordingDatabaseResetController()
        val gateway = DatabaseResetGatewayAdapter(controller)

        assertTrue(gateway.reset() is AppResult.Success)
        controller.error = IllegalStateException("reset failed")
        assertTrue(gateway.reset() is AppResult.Failure)
    }

    private class RecordingDatabaseResetController : DatabaseResetController {
        var error: Throwable? = null
        override suspend fun reset() = error
    }
}