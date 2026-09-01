package com.aozijx.passly.app.entry.favicon

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import javax.inject.Inject

internal fun interface FaviconHostResolver {
    fun resolve(host: String): Array<InetAddress>
}

internal data class ValidatedFaviconUrl(
    val uri: URI,
    val host: String,
    val addresses: List<InetAddress>,
)

enum class FaviconUrlFailure {
    INVALID_URL,
    HTTPS_REQUIRED,
    CREDENTIALS_NOT_ALLOWED,
    HOST_NOT_ALLOWED,
    PRIVATE_ADDRESS,
}

class FaviconUrlException(
    val reason: FaviconUrlFailure,
) : IllegalArgumentException(reason.name)

class FaviconUrlPolicy internal constructor(
    private val resolver: FaviconHostResolver,
) {
    @Inject
    constructor() : this(FaviconHostResolver(InetAddress::getAllByName))

    internal fun validate(value: String): ValidatedFaviconUrl {
        val uri = try {
            URI(value.trim())
        } catch (_: Exception) {
            throw FaviconUrlException(FaviconUrlFailure.INVALID_URL)
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            throw FaviconUrlException(FaviconUrlFailure.HTTPS_REQUIRED)
        }
        if (uri.userInfo != null) {
            throw FaviconUrlException(FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED)
        }
        val host = uri.host?.lowercase()?.removeSurrounding("[", "]")?.trimEnd('.')
            ?: throw FaviconUrlException(FaviconUrlFailure.INVALID_URL)
        if (host.isIpLiteral() || host == "localhost" || LOCAL_SUFFIXES.any(host::endsWith)) {
            throw FaviconUrlException(FaviconUrlFailure.HOST_NOT_ALLOWED)
        }
        val addresses = try {
            resolver.resolve(host)
        } catch (_: Exception) {
            throw FaviconUrlException(FaviconUrlFailure.HOST_NOT_ALLOWED)
        }
        if (addresses.isEmpty()) {
            throw FaviconUrlException(FaviconUrlFailure.HOST_NOT_ALLOWED)
        }
        if (addresses.any { !it.isPublicAddress() }) {
            throw FaviconUrlException(FaviconUrlFailure.PRIVATE_ADDRESS)
        }
        return ValidatedFaviconUrl(
            uri = uri,
            host = host,
            addresses = addresses.toList(),
        )
    }

    internal fun resolveRedirect(current: URI, location: String): ValidatedFaviconUrl {
        val target = try {
            current.resolve(location)
        } catch (_: Exception) {
            throw FaviconUrlException(FaviconUrlFailure.INVALID_URL)
        }
        return validate(target.toString())
    }

    private fun String.isIpLiteral(): Boolean {
        if (contains(':')) return true
        val parts = split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }

    private fun InetAddress.isPublicAddress(): Boolean {
        if (
            isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress ||
            isSiteLocalAddress || isMulticastAddress
        ) {
            return false
        }
        val bytes = address
        return when (this) {
            is Inet4Address -> {
                val first = bytes[0].toInt() and 0xff
                val second = bytes[1].toInt() and 0xff
                !(first == 100 && second in 64..127)
            }
            is Inet6Address -> (bytes[0].toInt() and 0xfe) != 0xfc
            else -> false
        }
    }

    private companion object {
        val LOCAL_SUFFIXES = setOf(".local", ".internal", ".lan")
    }
}
