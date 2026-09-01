package com.aozijx.passly.app.entry.favicon

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.InetAddress
import java.util.concurrent.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaviconImageDownloaderTest {

    @Test
    fun download_followsValidatedRedirectAndReturnsImageBytes() = runTest {
        val transport = QueueTransport(
            FakeResponse(302, redirectLocation = "/final.png"),
            FakeResponse(200, contentType = "image/png", bytes = byteArrayOf(1, 2, 3)),
        )

        val bytes = downloader(transport).download("https://example.com/start")

        assertArrayEquals(byteArrayOf(1, 2, 3), bytes)
        assertEquals(
            listOf("https://example.com/start", "https://example.com/final.png"),
            transport.opened.map { it.uri.toString() },
        )
        assertTrue(transport.responses.all(FakeResponse::closed))
    }

    @Test
    fun download_rejectsRedirectToPrivateAddressBeforeOpeningIt() = runTest {
        val policy = FaviconUrlPolicy { host ->
            val bytes = if (host == "private.example") byteArrayOf(10, 0, 0, 1) else byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34)
            arrayOf(InetAddress.getByAddress(bytes))
        }
        val transport = QueueTransport(FakeResponse(302, redirectLocation = "https://private.example/icon.png"))

        assertSuspendThrows<FaviconUrlException> {
            FaviconImageDownloader(policy, transport).download("https://public.example/icon.png")
        }
        assertEquals(1, transport.opened.size)
    }

    @Test
    fun download_pinsPolicyApprovedAddressesIntoTransport() = runTest {
        val approvedAddress = InetAddress.getByAddress(
            byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34),
        )
        val policy = FaviconUrlPolicy { arrayOf(approvedAddress) }
        val transport = QueueTransport(
            FakeResponse(200, contentType = "image/webp", bytes = byteArrayOf(1)),
        )

        FaviconImageDownloader(policy, transport).download("https://example.com/icon.webp")

        assertEquals(listOf(approvedAddress), transport.opened.single().addresses)
    }

    @Test
    fun download_rejectsNonImageAndOversizedBodies() = runTest {
        val nonImage = QueueTransport(FakeResponse(200, contentType = "text/html", bytes = byteArrayOf(1)))
        val oversized = QueueTransport(
            FakeResponse(200, contentType = "image/png", bytes = ByteArray(FaviconImageDownloader.MAX_INPUT_BYTES + 1)),
        )

        val typeError = assertSuspendThrows<FaviconDownloadException> {
            downloader(nonImage).download("https://example.com/icon")
        }
        val sizeError = assertSuspendThrows<FaviconDownloadException> {
            downloader(oversized).download("https://example.com/icon")
        }

        assertEquals(FaviconDownloadFailure.NOT_IMAGE, typeError.reason)
        assertEquals(FaviconDownloadFailure.TOO_LARGE, sizeError.reason)
        assertTrue(oversized.responses.single().bodyClosed)
        assertTrue(oversized.responses.single().closed)
    }

    @Test
    fun download_acceptsBodyAtExactSizeLimit() = runTest {
        val expected = ByteArray(FaviconImageDownloader.MAX_INPUT_BYTES) { 7 }

        val actual = downloader(
            QueueTransport(FakeResponse(200, contentType = "image/webp", bytes = expected)),
        ).download("https://example.com/icon")

        assertEquals(FaviconImageDownloader.MAX_INPUT_BYTES, actual.size)
    }

    @Test
    fun download_reportsTypedHttpAndRedirectFailures() = runTest {
        val httpError = assertSuspendThrows<FaviconDownloadException> {
            downloader(QueueTransport(FakeResponse(404))).download("https://example.com/icon")
        }
        val redirectError = assertSuspendThrows<FaviconDownloadException> {
            downloader(QueueTransport(FakeResponse(302))).download("https://example.com/icon")
        }

        assertEquals(FaviconDownloadFailure.HTTP_STATUS, httpError.reason)
        assertEquals(FaviconDownloadFailure.INVALID_REDIRECT, redirectError.reason)
    }

    @Test
    fun download_rejectsRedirectsPastLimit() = runTest {
        val transport = QueueTransport(
            *Array(FaviconImageDownloader.MAX_REDIRECTS + 1) {
                FakeResponse(302, redirectLocation = "/next$it")
            },
        )

        val error = assertSuspendThrows<FaviconDownloadException> {
            downloader(transport).download("https://example.com/start")
        }

        assertEquals(FaviconDownloadFailure.TOO_MANY_REDIRECTS, error.reason)
        assertTrue(transport.responses.all(FakeResponse::closed))
    }

    @Test
    fun download_closesBodyAndResponseWhenReadingIsCancelled() = runTest {
        val response = FakeResponse(
            statusCode = 200,
            contentType = "image/png",
            bodyFactory = { CancellingInputStream() },
        )

        assertSuspendThrows<CancellationException> {
            downloader(QueueTransport(response)).download("https://example.com/icon")
        }

        assertTrue(response.bodyClosed)
        assertTrue(response.closed)
    }

    private fun downloader(transport: FaviconHttpTransport): FaviconImageDownloader {
        val policy = FaviconUrlPolicy {
            arrayOf(InetAddress.getByAddress(byteArrayOf(93.toByte(), 184.toByte(), 216.toByte(), 34)))
        }
        return FaviconImageDownloader(policy, transport)
    }

    private suspend inline fun <reified T : Throwable> assertSuspendThrows(
        block: suspend () -> Unit,
    ): T {
        try {
            block()
        } catch (error: Throwable) {
            assertTrue("Expected ${T::class.java.name}, got ${error::class.java.name}", error is T)
            @Suppress("UNCHECKED_CAST")
            return error as T
        }
        throw AssertionError("Expected ${T::class.java.name} to be thrown")
    }

    private class QueueTransport(vararg response: FakeResponse) : FaviconHttpTransport {
        val responses = response.toList()
        val opened = mutableListOf<ValidatedFaviconUrl>()
        private var index = 0

        override fun open(target: ValidatedFaviconUrl): FaviconHttpResponse {
            opened += target
            return responses[index++]
        }
    }

    private class FakeResponse(
        override val statusCode: Int,
        override val contentType: String? = null,
        override val redirectLocation: String? = null,
        bytes: ByteArray = byteArrayOf(),
        bodyFactory: (() -> InputStream)? = null,
    ) : FaviconHttpResponse {
        private val stream = bodyFactory?.invoke() ?: ByteArrayInputStream(bytes)
        var closed = false
        var bodyClosed = false

        override val body: InputStream = object : InputStream() {
            override fun read(): Int = stream.read()
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = stream.read(buffer, offset, length)
            override fun close() {
                bodyClosed = true
                stream.close()
            }
        }

        override fun close() {
            closed = true
        }
    }

    private class CancellingInputStream : InputStream() {
        override fun read(): Int = throw CancellationException("cancelled")
    }
}
