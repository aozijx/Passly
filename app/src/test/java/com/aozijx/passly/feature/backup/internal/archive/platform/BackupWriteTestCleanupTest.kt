package com.aozijx.passly.feature.backup.internal.archive.platform

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupWriteTestCleanupTest {
    @Test
    fun `false or failed delete makes a successful write check fail`() {
        assertThrows(IllegalStateException::class.java) {
            enforceWriteTestCleanup(
                primaryFailure = null,
                cleanupResult = Result.success(false),
            )
        }
        val cleanupFailure = SecurityException("delete denied")

        val thrown =
            assertThrows(SecurityException::class.java) {
                enforceWriteTestCleanup(
                    primaryFailure = null,
                    cleanupResult = Result.failure(cleanupFailure),
                )
            }

        assertSame(cleanupFailure, thrown)
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

    @Test
    fun `cleanup fatal supersedes ordinary write failure and preserves it`() {
        val writeFailure = IllegalStateException("write failed")
        val cleanupFailure = AssertionError("fatal delete failure")

        val thrown =
            assertThrows(AssertionError::class.java) {
                enforceWriteTestCleanup(
                    primaryFailure = writeFailure,
                    cleanupResult = Result.failure(cleanupFailure),
                )
            }

        assertSame(cleanupFailure, thrown)
        assertEquals(listOf(writeFailure), thrown.suppressed.toList())
    }

    @Test
    fun `cleanup fatal supersedes cancellation and preserves it`() {
        val writeCancellation = CancellationException("write cancelled")
        val cleanupFailure = AssertionError("fatal delete failure")

        val thrown =
            assertThrows(AssertionError::class.java) {
                enforceWriteTestCleanup(
                    primaryFailure = writeCancellation,
                    cleanupResult = Result.failure(cleanupFailure),
                )
            }

        assertSame(cleanupFailure, thrown)
        assertEquals(listOf(writeCancellation), thrown.suppressed.toList())
    }

    @Test
    fun `cleanup cancellation supersedes ordinary write failure and preserves it`() {
        val writeFailure = IllegalStateException("write failed")
        val cleanupCancellation = CancellationException("cleanup cancelled")

        val thrown =
            assertThrows(CancellationException::class.java) {
                enforceWriteTestCleanup(
                    primaryFailure = writeFailure,
                    cleanupResult = Result.failure(cleanupCancellation),
                )
            }

        assertSame(cleanupCancellation, thrown)
        assertEquals(listOf(writeFailure), thrown.suppressed.toList())
    }
}
