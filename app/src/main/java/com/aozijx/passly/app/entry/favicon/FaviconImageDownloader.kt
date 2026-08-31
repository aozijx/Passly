package com.aozijx.passly.app.entry.favicon

import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FaviconDownloadFailure {
    INVALID_REDIRECT,
    TOO_MANY_REDIRECTS,
    HTTP_STATUS,
    NOT_IMAGE,
    EMPTY_IMAGE,
    TOO_LARGE,
    NETWORK,
}

class FaviconDownloadException(
    val reason: FaviconDownloadFailure,
) : IllegalStateException(reason.name)

internal interface FaviconHttpResponse : Closeable {
    val statusCode: Int
    val contentType: String?
    val redirectLocation: String?
    val body: InputStream
}

internal fun interface FaviconHttpTransport {
    fun open(uri: URI): FaviconHttpResponse
}

@Singleton
class FaviconImageDownloader internal constructor(
    private val urlPolicy: FaviconUrlPolicy,
    private val transport: FaviconHttpTransport,
) {
    @Inject
    constructor(urlPolicy: FaviconUrlPolicy) : this(urlPolicy, HttpUrlConnectionTransport)

    suspend fun download(value: String): ByteArray = withContext(Dispatchers.IO) {
        try {
            downloadBlocking(value)
        } catch (error: CancellationException) {
            throw error
        } catch (error: FaviconUrlException) {
            throw error
        } catch (error: FaviconDownloadException) {
            throw error
        } catch (_: Exception) {
            throw FaviconDownloadException(FaviconDownloadFailure.NETWORK)
        }
    }

    private fun downloadBlocking(value: String): ByteArray {
        var current = urlPolicy.validate(value)
        repeat(MAX_REDIRECTS + 1) { redirectIndex ->
            transport.open(current).use { response ->
                if (response.statusCode in 300..399) {
                    if (redirectIndex >= MAX_REDIRECTS) {
                        throw FaviconDownloadException(FaviconDownloadFailure.TOO_MANY_REDIRECTS)
                    }
                    val location = response.redirectLocation
                        ?: throw FaviconDownloadException(FaviconDownloadFailure.INVALID_REDIRECT)
                    current = urlPolicy.resolveRedirect(current, location)
                } else {
                    if (response.statusCode !in 200..299) {
                        throw FaviconDownloadException(FaviconDownloadFailure.HTTP_STATUS)
                    }
                    if (!response.contentType.orEmpty().substringBefore(';').trim()
                            .startsWith("image/", ignoreCase = true)
                    ) {
                        throw FaviconDownloadException(FaviconDownloadFailure.NOT_IMAGE)
                    }
                    return response.body.use { input -> input.readFaviconBytesBounded() }
                }
            }
        }
        throw FaviconDownloadException(FaviconDownloadFailure.TOO_MANY_REDIRECTS)
    }

    private fun InputStream.readFaviconBytesBounded(): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_INPUT_BYTES) {
                throw FaviconDownloadException(FaviconDownloadFailure.TOO_LARGE)
            }
            output.write(buffer, 0, count)
        }
        if (total == 0) {
            throw FaviconDownloadException(FaviconDownloadFailure.EMPTY_IMAGE)
        }
        return output.toByteArray()
    }

    companion object {
        const val MAX_INPUT_BYTES = 10 * 1024 * 1024
        const val MAX_REDIRECTS = 5
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000

        private object HttpUrlConnectionTransport : FaviconHttpTransport {
            override fun open(uri: URI): FaviconHttpResponse {
                val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "image/*")
                }
                return HttpUrlConnectionResponse(connection)
            }
        }

        private class HttpUrlConnectionResponse(
            private val connection: HttpURLConnection,
        ) : FaviconHttpResponse {
            override val statusCode: Int get() = connection.responseCode
            override val contentType: String? get() = connection.contentType
            override val redirectLocation: String? get() = connection.getHeaderField("Location")
            override val body: InputStream get() = connection.inputStream

            override fun close() {
                connection.disconnect()
            }
        }
    }
}
