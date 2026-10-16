package com.aozijx.passly.presentation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLockControllerBoundaryTest {
    @Test
    fun `screen off locking is process scoped instead of activity scoped`() {
        val application = source("com/aozijx/passly/app/PasslyApplication.kt")
        val controller = source("com/aozijx/passly/app/DeviceLockController.kt")

        assertTrue(application.contains("deviceLockController.start()"))
        assertTrue(controller.contains("Intent.ACTION_SCREEN_OFF"))
        assertTrue(controller.contains("ContextCompat.RECEIVER_EXPORTED"))
        assertTrue(controller.contains("goAsync()"))
        assertTrue(controller.contains("DeviceLockTrigger.FLIP"))
        assertTrue(controller.contains("screenOffLockTracker.markScreenOff()"))
        assertTrue(controller.contains("screenOffLockTracker.pendingGeneration()"))
    }

    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
