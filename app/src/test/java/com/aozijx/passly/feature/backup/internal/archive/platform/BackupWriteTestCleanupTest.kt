package com.aozijx.passly.feature.backup.internal.archive.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupWriteTestCleanupTest {
    @Test
    fun `delete false fails a successful write check`() {
        assertThrows(IllegalStateException::class.java) {
            enforceWriteTestCleanup(
                primaryFailure = null,
                cleanupResult = Result.success(false),
            )
        }
    }

    @Test
    fun `delete exception fails a successful write check`() {
        val cleanupFailure = SecurityException("delete denied")

        val thrown = assertThrows(SecurityException::class.java) {
            enforceWriteTestCleanup(
                primaryFailure = null,
                cleanupResult = Result.failure(cleanupFailure),
            )
        }

        assertEquals(cleanupFailure, thrown)
    }

    @Test
    fun `write failure remains primary when cleanup also fails`() {
        val writeFailure = IllegalStateException("write failed")
        val cleanupFailure = SecurityException("delete denied")

        enforceWriteTestCleanup(
            primaryFailure = writeFailure,
            cleanupResult = Result.failure(cleanupFailure),
        )

        assertEquals(listOf(cleanupFailure), writeFailure.suppressed.toList())
    }
}
