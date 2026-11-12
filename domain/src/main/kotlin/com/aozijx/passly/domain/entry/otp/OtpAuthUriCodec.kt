package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.HotpDefinition
import com.aozijx.passly.domain.entry.model.otp.OtpAuthUriKind
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import com.aozijx.passly.domain.entry.model.otp.SteamDefinition
import com.aozijx.passly.domain.entry.model.otp.TotpDefinition
import com.aozijx.passly.domain.entry.otp.generation.OtpSecretCodec
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.StandardCharsets

sealed interface OtpUriParseResult {
    data class Parsed(
        val config: OtpConfig,
    ) : OtpUriParseResult {
        override fun toString() = "Parsed(***)"
    }

    data object NotOtp : OtpUriParseResult

    data object Malformed : OtpUriParseResult

    data object Unsupported : OtpUriParseResult
}

sealed interface OtpUriFormatResult {
    data class Formatted(
        val content: String,
    ) : OtpUriFormatResult {
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
        val uriKind =
            when (uri.host?.lowercase()) {
                "totp" -> OtpAuthUriKind.TIME_BASED
                "hotp" -> OtpAuthUriKind.COUNTER_BASED
                else -> return OtpUriParseResult.Unsupported
            }
        require(uri.rawAuthority.equals(uri.host, ignoreCase = true))
        val path = uri.rawPath.orEmpty()
        require(path.startsWith('/') && path.length > 1 && '/' !in path.drop(1))
        val parameters = parseQuery(uri.rawQuery.orEmpty())
        if ("encoder" in parameters || "encoding" in parameters) return OtpUriParseResult.Unsupported
        val label = parseLabel(decodeComponent(path.drop(1)), parameters["issuer"])
        val definition = OtpTypeDefinitions.fromAuthUri(uriKind, label.issuer) ?: return OtpUriParseResult.Unsupported
        val algorithm =
            when (parameters["algorithm"]?.uppercase()) {
                null, "SHA1" -> OtpHashAlgorithm.SHA1
                "SHA256" -> OtpHashAlgorithm.SHA256
                "SHA512" -> OtpHashAlgorithm.SHA512
                else -> return OtpUriParseResult.Unsupported
            }
        val digits =
            if ("digits" in parameters) {
                parameters.getValue("digits").toIntOrNull() ?: return OtpUriParseResult.Malformed
            } else {
                when (definition.type) {
                    OtpType.TOTP -> TotpDefinition.DEFAULT_DIGITS
                    OtpType.HOTP -> HotpDefinition.DEFAULT_DIGITS
                    OtpType.STEAM -> SteamDefinition.DIGITS
                }
            }
        val secret =
            OtpSecretCodec.canonicalBase32(
                parameters["secret"] ?: return OtpUriParseResult.Malformed,
                OtpSecretEncoding.BASE32,
            )
        val config =
            try {
                when (definition.type) {
                    OtpType.TOTP -> {
                        if ("counter" in parameters) return OtpUriParseResult.Unsupported
                        val period =
                            if ("period" in parameters) {
                                parameters.getValue("period").toIntOrNull() ?: return OtpUriParseResult.Malformed
                            } else {
                                TotpDefinition.DEFAULT_PERIOD_SECONDS
                            }
                        if (period <= 0) return OtpUriParseResult.Malformed
                        if (algorithm !in TotpDefinition.supportedAlgorithms) return OtpUriParseResult.Unsupported
                        if (digits !in TotpDefinition.allowedDigits) return OtpUriParseResult.Unsupported
                        OtpConfig.Totp(secret, algorithm, digits, period, issuer = label.issuer, accountName = label.account)
                    }

                    OtpType.HOTP -> {
                        if ("period" in parameters) return OtpUriParseResult.Unsupported
                        val counter = parameters["counter"]?.toLongOrNull() ?: return OtpUriParseResult.Malformed
                        if (counter < 0L) return OtpUriParseResult.Malformed
                        if (algorithm !in HotpDefinition.supportedAlgorithms) return OtpUriParseResult.Unsupported
                        if (digits !in HotpDefinition.allowedDigits) return OtpUriParseResult.Unsupported
                        OtpConfig.Hotp(secret, algorithm, digits, counter, issuer = label.issuer, accountName = label.account)
                    }

                    OtpType.STEAM -> {
                        if ("counter" in parameters) return OtpUriParseResult.Unsupported
                        val period =
                            if ("period" in parameters) {
                                parameters.getValue("period").toIntOrNull() ?: return OtpUriParseResult.Malformed
                            } else {
                                SteamDefinition.PERIOD_SECONDS
                            }
                        if (period != SteamDefinition.PERIOD_SECONDS) return OtpUriParseResult.Unsupported
                        if (algorithm !in SteamDefinition.supportedAlgorithms || digits != SteamDefinition.DIGITS) {
                            return OtpUriParseResult.Unsupported
                        }
                        SteamDefinition.configuration(
                            secret = secret,
                            encoding = OtpSecretEncoding.BASE32,
                            issuer = label.issuer,
                            accountName = label.account,
                        )
                    }
                }
            } catch (_: IllegalArgumentException) {
                return OtpUriParseResult.Unsupported
            } catch (_: OtpGenerationError) {
                return OtpUriParseResult.Malformed
            }
        return OtpUriParseResult.Parsed(config)
    }

    fun format(
        config: OtpConfig,
        title: String,
    ): OtpUriFormatResult =
        try {
            val definition = OtpTypeDefinitions[config.type]
            require(OtpConfigValidation.validate(config).isEmpty())
            val issuer =
                config.issuer?.takeIf(String::isNotBlank)
                    ?: definition.issuerAlias
            require(OtpTypeDefinitions.fromAuthUri(definition.authUriKind, issuer)?.type == definition.type)
            val label =
                config.accountName?.takeIf(String::isNotBlank)?.let { Label(issuer, it) }
                    ?: parseLabel(title, issuer)
            require(OtpTypeDefinitions.fromAuthUri(definition.authUriKind, label.issuer)?.type == definition.type)
            val secret = config.secret ?: throw OtpGenerationError.InvalidSecret
            val (algorithm, digits) =
                when (config) {
                    is OtpConfig.Totp -> config.algorithm to config.digits
                    is OtpConfig.Hotp -> config.algorithm to config.digits
                }
            val parameters =
                linkedMapOf(
                    "secret" to OtpSecretCodec.canonicalBase32(secret, config.encoding),
                    "algorithm" to
                        when (algorithm) {
                            OtpHashAlgorithm.SHA1 -> "SHA1"
                            OtpHashAlgorithm.SHA256 -> "SHA256"
                            OtpHashAlgorithm.SHA512 -> "SHA512"
                        },
                    "digits" to digits.toString(),
                )
            when (config) {
                is OtpConfig.Totp -> parameters["period"] = config.periodSeconds.toString()
                is OtpConfig.Hotp -> parameters["counter"] = config.counter.toString()
            }
            label.issuer?.let { parameters["issuer"] = it }
            val text = label.issuer?.let { "$it:${label.account}" } ?: label.account
            val query = parameters.entries.joinToString("&") { (key, value) -> "$key=${encodeComponent(value)}" }
            OtpUriFormatResult.Formatted("otpauth://${definition.authUriKind.schemeAuthority}/${encodeComponent(text)}?$query")
        } catch (_: IllegalArgumentException) {
            OtpUriFormatResult.InvalidConfiguration
        } catch (_: OtpGenerationError) {
            OtpUriFormatResult.InvalidConfiguration
        }

    private data class Label(
        val issuer: String?,
        val account: String,
    ) {
        init {
            require(issuer == null || (issuer.isNotBlank() && ':' !in issuer))
            require(account.isNotBlank() && ':' !in account)
        }
    }

    private fun parseLabel(
        value: String,
        issuerParameter: String?,
    ): Label {
        val colon = value.indexOf(':')
        val prefix = if (colon >= 0) value.take(colon) else null
        require(prefix == null || issuerParameter == null || prefix == issuerParameter)
        return Label(
            issuerParameter ?: prefix,
            if (colon >= 0) value.substring(colon + 1).trimStart(' ') else value,
        )
    }

    private fun parseQuery(value: String): Map<String, String> =
        buildMap {
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

    private fun encodeComponent(value: String): String =
        URLEncoder
            .encode(value, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

    private fun decodeComponent(value: String): String =
        buildString {
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
