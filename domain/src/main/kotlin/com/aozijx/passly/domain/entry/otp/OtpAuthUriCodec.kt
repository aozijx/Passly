package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets

sealed interface OtpUriParseResult {
    data class Parsed(val config: OtpConfig) : OtpUriParseResult {
        override fun toString() = "Parsed(***)"
    }
    data object NotOtp : OtpUriParseResult
    data object Malformed : OtpUriParseResult
    data object Unsupported : OtpUriParseResult
}

sealed interface OtpUriFormatResult {
    data class Formatted(val content: String) : OtpUriFormatResult {
        override fun toString() = "Formatted(***)"
    }
    data object InvalidConfiguration : OtpUriFormatResult
}

object OtpAuthUriCodec {
    private val PARAMETERS = setOf("secret", "issuer", "algorithm", "digits", "period", "counter", "encoder", "encoding")

    fun parse(value: String): OtpUriParseResult {
        if (!value.startsWith("otpauth:", ignoreCase = true)) return OtpUriParseResult.NotOtp
        return try {
            parseUri(URI(value))
        } catch (_: URISyntaxException) {
            OtpUriParseResult.Malformed
        } catch (_: CharacterCodingException) {
            OtpUriParseResult.Malformed
        } catch (_: IllegalArgumentException) {
            OtpUriParseResult.Malformed
        } catch (_: OtpGenerationError) {
            OtpUriParseResult.Malformed
        }
    }

    private fun parseUri(uri: URI): OtpUriParseResult {
        require(!uri.isOpaque && !uri.rawAuthority.isNullOrBlank())
        require(uri.rawFragment == null && uri.rawUserInfo == null && uri.port == -1)
        val type = when (uri.host?.lowercase()) {
            "totp" -> OtpType.TOTP
            "hotp" -> OtpType.HOTP
            else -> return OtpUriParseResult.Unsupported
        }
        require(uri.rawAuthority.equals(uri.host, ignoreCase = true))
        val path = uri.rawPath.orEmpty()
        require(path.startsWith('/') && path.length > 1 && '/' !in path.drop(1))
        val parameters = parseQuery(uri.rawQuery.orEmpty())
        if ("encoder" in parameters || "encoding" in parameters) return OtpUriParseResult.Unsupported
        val label = parseLabel(decodeComponent(path.drop(1)), parameters["issuer"])
        val resolvedType = if (type == OtpType.TOTP && label.issuer.equals("Steam", ignoreCase = true)) {
            OtpType.STEAM
        } else type
        val algorithm = when (parameters["algorithm"]?.uppercase()) {
            null, "SHA1" -> OtpHashAlgorithm.SHA1
            "SHA256" -> OtpHashAlgorithm.SHA256
            "SHA512" -> OtpHashAlgorithm.SHA512
            else -> return OtpUriParseResult.Unsupported
        }
        val digits = if ("digits" in parameters) {
            parameters.getValue("digits").toIntOrNull() ?: return OtpUriParseResult.Malformed
        } else if (resolvedType == OtpType.STEAM) 5 else 6
        if (resolvedType == OtpType.STEAM && digits != 5 || resolvedType != OtpType.STEAM && digits !in 6..8) {
            return OtpUriParseResult.Unsupported
        }
        val period = if ("period" in parameters) {
            parameters.getValue("period").toIntOrNull()?.takeIf { it > 0 }
                ?: return OtpUriParseResult.Malformed
        } else 30
        val counter = if ("counter" in parameters) {
            parameters.getValue("counter").toLongOrNull()?.takeIf { it >= 0 }
                ?: return OtpUriParseResult.Malformed
        } else null
        if (type == OtpType.HOTP && counter == null) return OtpUriParseResult.Malformed
        if (resolvedType == OtpType.STEAM && (period != 30 || algorithm != OtpHashAlgorithm.SHA1)) {
            return OtpUriParseResult.Unsupported
        }
        val secret = parameters["secret"] ?: return OtpUriParseResult.Malformed
        return OtpUriParseResult.Parsed(OtpConfig(
            type = resolvedType,
            secret = canonicalSecret(secret, OtpSecretEncoding.BASE32),
            algorithm = algorithm,
            digits = digits,
            periodSeconds = if (type == OtpType.HOTP) null else period,
            counter = if (type == OtpType.HOTP) counter else null,
            issuer = label.issuer,
            accountName = label.account,
        ))
    }

    fun format(config: OtpConfig, title: String): OtpUriFormatResult = try {
        val issuer = config.issuer?.takeIf(String::isNotBlank)
            ?: if (config.type == OtpType.STEAM) "Steam" else null
        val label = config.accountName?.takeIf(String::isNotBlank)?.let { Label(issuer, it) }
            ?: parseLabel(title, issuer)
        if (config.type == OtpType.STEAM) {
            require(label.issuer.equals("Steam", ignoreCase = true) && config.algorithm == OtpHashAlgorithm.SHA1)
            require(config.digits == 5 && config.periodSeconds == 30)
        } else {
            require(config.digits in 6..8)
            require(config.type != OtpType.TOTP || !label.issuer.equals("Steam", ignoreCase = true))
        }
        val secret = config.secret ?: throw OtpGenerationError.InvalidSecret
        val parameters = linkedMapOf(
            "secret" to canonicalSecret(secret, config.encoding),
            "algorithm" to when (config.algorithm) {
                OtpHashAlgorithm.SHA1 -> "SHA1"
                OtpHashAlgorithm.SHA256 -> "SHA256"
                OtpHashAlgorithm.SHA512 -> "SHA512"
            },
            "digits" to config.digits.toString(),
        )
        when (config.type) {
            OtpType.TOTP, OtpType.STEAM -> parameters["period"] = requireNotNull(config.periodSeconds).toString()
            OtpType.HOTP -> parameters["counter"] = requireNotNull(config.counter).toString()
        }
        label.issuer?.let { parameters["issuer"] = it }
        val host = when (config.type) {
            OtpType.TOTP, OtpType.STEAM -> "totp"
            OtpType.HOTP -> "hotp"
        }
        val text = label.issuer?.let { "$it:${label.account}" } ?: label.account
        val query = parameters.entries.joinToString("&") { (key, value) -> "$key=${encodeComponent(value)}" }
        OtpUriFormatResult.Formatted("otpauth://$host/${encodeComponent(text)}?$query")
    } catch (_: IllegalArgumentException) {
        OtpUriFormatResult.InvalidConfiguration
    } catch (_: OtpGenerationError) {
        OtpUriFormatResult.InvalidConfiguration
    }

    private data class Label(val issuer: String?, val account: String) {
        init {
            require(issuer == null || issuer.isNotBlank() && ':' !in issuer)
            require(account.isNotBlank() && ':' !in account)
        }
    }

    private fun parseLabel(value: String, issuerParameter: String?): Label {
        val colon = value.indexOf(':')
        val prefix = if (colon >= 0) value.take(colon) else null
        require(prefix == null || issuerParameter == null || prefix == issuerParameter)
        return Label(
            issuerParameter ?: prefix,
            if (colon >= 0) value.substring(colon + 1).trimStart(' ') else value,
        )
    }

    private fun parseQuery(value: String): Map<String, String> = buildMap {
        value.split('&').filter(String::isNotEmpty).forEach { part ->
            val equals = part.indexOf('=')
            require(equals > 0)
            val key = decodeComponent(part.take(equals))
            val decoded = decodeComponent(part.substring(equals + 1))
            if (key in PARAMETERS) {
                require(key !in this)
                put(key, decoded)
            }
        }
    }

    private fun canonicalSecret(value: String, encoding: OtpSecretEncoding): String {
        val source = if (encoding == OtpSecretEncoding.BASE32) value.replace(" ", "") else value
        val decoded = OtpSecretCodec.decode(source, encoding)
        return try {
            OtpSecretCodec.encodeBase32(decoded)
        } finally {
            decoded.fill(0)
        }
    }

    private fun encodeComponent(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

    private fun decodeComponent(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            if (value[index] != '%') {
                append(value[index++])
                continue
            }
            var end = index
            while (end + 2 < value.length && value[end] == '%') end += 3
            require(end > index)
            val bytes = ByteArray((end - index) / 3)
            try {
                bytes.indices.forEach { offset ->
                    val start = index + offset * 3
                    bytes[offset] = ((value[start + 1].digitToInt(16) shl 4) or value[start + 2].digitToInt(16)).toByte()
                }
                val chars = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
                try {
                    append(chars)
                } finally {
                    for (offset in 0 until chars.limit()) chars.put(offset, '\u0000')
                }
            } finally {
                bytes.fill(0)
            }
            index = end
        }
    }
}
