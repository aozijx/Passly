package com.aozijx.passly.app.entry.favicon

import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import com.aozijx.passly.core.platform.media.FaviconProcessingException
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FaviconProcessingFailureMappingTest {
    @Test
    fun `implementation failures map to stable media contract`() {
        assertEquals(
            FaviconProcessingFailure.INVALID_URL,
            FaviconUrlException(FaviconUrlFailure.HTTPS_REQUIRED)
                .toFaviconProcessingException()
                .failure,
        )
        assertEquals(
            FaviconProcessingFailure.URL_NOT_ALLOWED,
            FaviconUrlException(FaviconUrlFailure.PRIVATE_ADDRESS)
                .toFaviconProcessingException()
                .failure,
        )
        assertEquals(
            FaviconProcessingFailure.IMAGE_TOO_LARGE,
            FaviconDownloadException(FaviconDownloadFailure.TOO_LARGE)
                .toFaviconProcessingException()
                .failure,
        )
        assertEquals(
            FaviconProcessingFailure.INVALID_IMAGE,
            IllegalStateException("decode").toFaviconProcessingException().failure,
        )
    }

    @Test
    fun `result boundary maps exceptions and propagates cancellation and fatal errors`() =
        runTest {
            val ordinaryFailure = IllegalStateException("image processing failed")
            val result = resultOfPreservingCancellation { throw ordinaryFailure }
            val mappedFailure = requireNotNull(result.exceptionOrNull())
            assertEquals(FaviconProcessingException::class, mappedFailure::class)
            assertEquals(FaviconProcessingFailure.INVALID_IMAGE, (mappedFailure as FaviconProcessingException).failure)
            assertSame(ordinaryFailure, mappedFailure.cause)

            val cancelled = CancellationException("cancelled")
            val propagatedCancellation =
                assertSuspendThrows<CancellationException> {
                    resultOfPreservingCancellation { throw cancelled }
                }
            assertSame(cancelled, propagatedCancellation)

            val fatal = AssertionError("fatal image processing error")
            val propagatedFatal =
                assertSuspendThrows<AssertionError> {
                    resultOfPreservingCancellation { throw fatal }
                }
            assertSame(fatal, propagatedFatal)
        }

    @Test
    fun `transform output is deleted before the original fatal error propagates`() {
        val root = Files.createTempDirectory("passly-favicon-output").toFile()
        try {
            val output = File(root, "transform.webp").apply { writeText("partial output") }
            val fatal = AssertionError("fatal transform failure")

            val propagated =
                assertThrows(AssertionError::class.java) {
                    withFaviconOutputCleanup(output) { throw fatal }
                }

            assertSame(fatal, propagated)
            assertFalse(output.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `transform cleanup failure is suppressed behind the original fatal error`() {
        val root = Files.createTempDirectory("passly-favicon-output").toFile()
        try {
            val backingFile = File(root, "transform.webp").apply { writeText("partial output") }
            val cleanupFailure = SecurityException("delete denied")
            val output = DeleteFailingFile(backingFile, cleanupFailure)
            val fatal = AssertionError("fatal transform failure")

            val propagated =
                assertThrows(AssertionError::class.java) {
                    withFaviconOutputCleanup(output) { throw fatal }
                }

            assertSame(fatal, propagated)
            assertSame(cleanupFailure, propagated.suppressed.single())
            assertTrue(backingFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `fatal cleanup error outranks an ordinary transform error`() {
        val root = Files.createTempDirectory("passly-favicon-output").toFile()
        try {
            val backingFile = File(root, "transform.webp").apply { writeText("partial output") }
            val fatalCleanup = AssertionError("fatal delete failure")
            val output = DeleteFailingFile(backingFile, fatalCleanup)
            val ordinaryFailure = IllegalStateException("transform failed")

            val propagated =
                assertThrows(AssertionError::class.java) {
                    withFaviconOutputCleanup(output) { throw ordinaryFailure }
                }

            assertSame(fatalCleanup, propagated)
            assertSame(ordinaryFailure, propagated.suppressed.single())
        } finally {
            root.deleteRecursively()
        }
    }

    private suspend inline fun <reified T : Throwable> assertSuspendThrows(crossinline block: suspend () -> Unit): T {
        try {
            block()
        } catch (error: Throwable) {
            assertTrue("Expected ${T::class.java.name}, got ${error::class.java.name}", error is T)
            @Suppress("UNCHECKED_CAST")
            return error as T
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }

    private class DeleteFailingFile(
        file: File,
        private val failure: Throwable,
    ) : File(file.path) {
        override fun delete(): Boolean = throw failure
    }
}
