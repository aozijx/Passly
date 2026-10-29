package com.aozijx.passly.feature.backup.internal.archive.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupCreatedDocumentCleanupTest {
    @Test
    fun `cancellation after document creation deletes document and propagates cancellation`() = runBlocking {
        val cancelled = CancellationException("export cancelled")
        val deletedUris = mutableListOf<String>()

        val thrown = try {
            withCreatedDocumentCleanupOnCancellation(
                ioDispatcher = Dispatchers.Unconfined,
                deleteDocument = { uri -> deletedUris += uri; true },
            ) { registerCreatedDocument ->
                registerCreatedDocument("content://backup/new-file")
                throw cancelled
            }
            error("Expected cancellation")
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancelled, thrown)
        assertEquals(listOf("content://backup/new-file"), deletedUris)
    }

    @Test
    fun `cleanup failure is suppressed on original cancellation`() = runBlocking {
        val cancelled = CancellationException("export cancelled")
        val cleanupFailure = SecurityException("delete denied")

        val thrown = try {
            withCreatedDocumentCleanupOnCancellation(
                ioDispatcher = Dispatchers.Unconfined,
                deleteDocument = { throw cleanupFailure },
            ) { registerCreatedDocument ->
                registerCreatedDocument("content://backup/new-file")
                throw cancelled
            }
            error("Expected cancellation")
        } catch (error: CancellationException) {
            error
        }

        assertSame(cancelled, thrown)
        assertEquals(listOf(cleanupFailure), thrown.suppressed.toList())
    }
}
