package com.aozijx.passly.feature.backup.internal.archive.platform

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BackupCreatedDocumentCleanupTest {
    @Test
    fun `cancellation after document creation deletes document and propagates cancellation`() =
        runBlocking {
            val cancelled = CancellationException("export cancelled")
            val deletedUris = mutableListOf<String>()

            val thrown =
                try {
                    withCreatedDocumentCleanupOnFailure(
                        ioDispatcher = Dispatchers.Unconfined,
                        deleteDocument = { uri ->
                            deletedUris += uri
                            true
                        },
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
    fun `cleanup failure is suppressed on original cancellation`() =
        runBlocking {
            val cancelled = CancellationException("export cancelled")
            val cleanupFailure = SecurityException("delete denied")

            val thrown =
                try {
                    withCreatedDocumentCleanupOnFailure(
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

    @Test
    fun `fatal cleanup supersedes cancellation and preserves cancellation`() =
        runBlocking {
            val cancelled = CancellationException("export cancelled")
            val cleanupFailure = AssertionError("fatal delete failure")
            val deletedUris = mutableListOf<String>()

            val thrown =
                try {
                    withCreatedDocumentCleanupOnFailure(
                        ioDispatcher = Dispatchers.Unconfined,
                        deleteDocument = { uri ->
                            deletedUris += uri
                            throw cleanupFailure
                        },
                    ) { registerCreatedDocument ->
                        registerCreatedDocument("content://backup/new-file")
                        throw cancelled
                    }
                    error("Expected cleanup fatal error")
                } catch (error: AssertionError) {
                    error
                }

            assertSame(cleanupFailure, thrown)
            assertEquals(listOf(cancelled), thrown.suppressed.toList())
            assertEquals(listOf("content://backup/new-file"), deletedUris)
        }

    @Test
    fun `fatal operation failure remains primary when cleanup fails ordinarily`() =
        runBlocking {
            val fatal = AssertionError("fatal create failure")
            val cleanupFailure = SecurityException("delete denied")

            val thrown =
                try {
                    withCreatedDocumentCleanupOnFailure(
                        ioDispatcher = Dispatchers.Unconfined,
                        deleteDocument = { throw cleanupFailure },
                    ) { registerCreatedDocument ->
                        registerCreatedDocument("content://backup/new-file")
                        throw fatal
                    }
                    error("Expected fatal error")
                } catch (error: AssertionError) {
                    error
                }

            assertSame(fatal, thrown)
            assertEquals(listOf(cleanupFailure), thrown.suppressed.toList())
        }

    @Test
    fun `ordinary operation failure cleans the created document and remains primary`() =
        runBlocking {
            val operationFailure = IllegalStateException("provider operation failed")
            val deletedUris = mutableListOf<String>()

            val thrown =
                try {
                    withCreatedDocumentCleanupOnFailure(
                        ioDispatcher = Dispatchers.Unconfined,
                        deleteDocument = { uri ->
                            deletedUris += uri
                            true
                        },
                    ) { registerCreatedDocument ->
                        registerCreatedDocument("content://backup/new-file")
                        throw operationFailure
                    }
                    error("Expected operation failure")
                } catch (error: IllegalStateException) {
                    error
                }

            assertSame(operationFailure, thrown)
            assertEquals(listOf("content://backup/new-file"), deletedUris)
        }
}
