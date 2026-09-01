package com.aozijx.passly.app.entry.favicon

import java.io.Closeable
import java.io.InputStream
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

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
    fun open(target: ValidatedFaviconUrl): FaviconHttpResponse
}

@Singleton
class FaviconImageDownloader internal constructor(
    private val urlPolicy: FaviconUrlPolicy,
    private val transport: FaviconHttpTransport,
) {
    @Inject
    constructor(urlPolicy: FaviconUrlPolicy) : this(urlPolicy, OkHttpTransport)

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
                    current = urlPolicy.resolveRedirect(current.uri, location)
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

        private object OkHttpTransport : FaviconHttpTransport {
            private val client = OkHttpClient.Builder()
                .followRedirects(false)
                .followSslRedirects(false)
                .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()

            override fun open(target: ValidatedFaviconUrl): FaviconHttpResponse {
                val pinnedClient = client.newBuilder()
                    .dns(object : Dns {
                        override fun lookup(hostname: String): List<InetAddress> {
                            if (!hostname.equals(target.host, ignoreCase = true)) {
                                throw UnknownHostException(hostname)
                            }
                            return target.addresses
                        }
                    })
                    .build()
                val request = Request.Builder()
                    .url(target.uri.toURL())
                    .header("Accept", "image/*")
                    .get()
                    .build()
                return OkHttpResponse(pinnedClient.newCall(request).execute())
            }
        }

        private class OkHttpResponse(
            private val response: Response,
        ) : FaviconHttpResponse {
            override val statusCode: Int get() = response.code
            override val contentType: String? get() = response.header("Content-Type")
            override val redirectLocation: String? get() = response.header("Location")
            override val body: InputStream
                get() = requireNotNull(response.body) { "HTTP response body is missing" }.byteStream()

            override fun close() {
                response.close()
            }
        }
    }
}
