package com.aozijx.passly.domain.autofill

import com.google.common.net.InternetDomainName
import java.net.IDN
import java.net.URI
import java.net.URISyntaxException
import java.text.Normalizer
import java.util.Locale

/** Owns canonical package/domain matching for every autofill integration. */
object AutofillScope {
    fun normalizeApplicationId(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)

    /** Matches canonical hosts exactly or by their PSL registrable domain when strict mode is off. */
    fun matchesDomain(
        candidateHost: String,
        requestHost: String,
        strict: Boolean,
    ): Boolean {
        val candidate = (normalizeDomain(candidateHost) as? AutofillDomainNormalization.Valid)?.host ?: return false
        val request = (normalizeDomain(requestHost) as? AutofillDomainNormalization.Valid)?.host ?: return false
        if (candidate == request || strict) return candidate == request

        val candidateRegistrableDomain = candidate.toRegistrableDomain() ?: return false
        val requestRegistrableDomain = request.toRegistrableDomain() ?: return false
        return candidateRegistrableDomain == requestRegistrableDomain
    }

    /** Returns null for IPs, public suffixes, and hosts not recognized by the bundled PSL. */
    fun registrableDomain(host: String): String? {
        val canonicalHost = (normalizeDomain(host) as? AutofillDomainNormalization.Valid)?.host ?: return null
        return canonicalHost.toRegistrableDomain()
    }

    private fun String.toRegistrableDomain(): String? {
        if (startsWith('[') || ':' in this) return null
        return try {
            InternetDomainName.from(this).topPrivateDomain().toString()
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IllegalStateException) {
            null
        }
    }

    /**
     * Normalizes a host or URL to its exact host scope. URL scheme, port, path, query, and
     * fragment are ignored. Invalid hosts return an explicit invalid result.
     */
    fun normalizeDomain(value: String?): AutofillDomainNormalization {
        val input = value?.trim()?.takeIf(String::isNotEmpty) ?: return AutofillDomainNormalization.Missing
        val cleanUrl = if (URL_SCHEME_PATTERN.containsMatchIn(input)) input else "https://$input"
        val uri =
            try {
                URI(cleanUrl)
            } catch (_: URISyntaxException) {
                return AutofillDomainNormalization.Invalid
            }
        val authority = uri.rawAuthority?.takeIf(String::isNotEmpty) ?: return AutofillDomainNormalization.Invalid
        if (uri.rawUserInfo != null || '@' in authority) return AutofillDomainNormalization.Invalid

        val host =
            if (authority.startsWith('[')) {
                val closingBracket = authority.indexOf(']')
                if (closingBracket < 0 || !validPortSuffix(authority.substring(closingBracket + 1))) {
                    return AutofillDomainNormalization.Invalid
                }
                normalizeIpv6(authority.substring(1, closingBracket)) ?: return AutofillDomainNormalization.Invalid
            } else {
                if (authority.count { it == ':' } > 1) return AutofillDomainNormalization.Invalid
                val hostPart = authority.substringBeforeLast(':', authority)
                if (authority.contains(':') && !validPortSuffix(":" + authority.substringAfterLast(':'))) {
                    return AutofillDomainNormalization.Invalid
                }
                normalizeDnsHost(hostPart) ?: return AutofillDomainNormalization.Invalid
            }
        return AutofillDomainNormalization.Valid(host)
    }

    private fun normalizeDnsHost(host: String): String? {
        val asciiHost =
            try {
                IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
            } catch (_: IllegalArgumentException) {
                return null
            }
        if (host.any { it.code > 0x7f } &&
            !IDN.toUnicode(asciiHost).equals(
                normalizeUnicodeHost(host),
                ignoreCase = true,
            )
        ) {
            // Reject lossy IDNA2003 mappings that could merge distinct host names.
            return null
        }

        val canonicalHost = asciiHost.removeSuffix(".")
        if (canonicalHost.isBlank() || canonicalHost.contains("..") || '.' !in canonicalHost) return null
        val labels = canonicalHost.split('.')
        if (labels.any { it.isEmpty() || it.length > MAX_DNS_LABEL_LENGTH || !isValidAceLabel(it) }) return null
        if (canonicalHost.length > MAX_DNS_HOST_LENGTH) return null
        return canonicalHost
    }

    private fun isValidAceLabel(label: String): Boolean {
        if (!label.startsWith(ACE_PREFIX)) return true
        val unicode = IDN.toUnicode(label)
        if (unicode.equals(label, ignoreCase = true)) return false
        return try {
            IDN.toASCII(unicode, IDN.USE_STD3_ASCII_RULES).equals(label, ignoreCase = true)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun normalizeUnicodeHost(host: String): String {
        val normalized = Normalizer.normalize(host.lowercase(Locale.ROOT), Normalizer.Form.NFC)
        return normalized
            .replace('\u3002', '.')
            .replace('\uff0e', '.')
            .replace('\uff61', '.')
    }

    private fun validPortSuffix(suffix: String): Boolean =
        suffix.isEmpty() ||
            (
                suffix.startsWith(':') &&
                    suffix.drop(1).isNotEmpty() &&
                    suffix.drop(1).all { it in '0'..'9' } &&
                    suffix.drop(1).toIntOrNull()?.let { it in 0..MAX_PORT } == true
            )

    /** Parses IPv6 without DNS lookup and serializes it with RFC 5952 zero compression. */
    private fun normalizeIpv6(literal: String): String? {
        if (literal.isEmpty() || '%' in literal) return null
        val expandedInput = expandIpv4Tail(literal) ?: return null
        val compressionIndex = expandedInput.indexOf("::")
        if (compressionIndex != expandedInput.lastIndexOf("::")) return null

        val left: List<String>
        val right: List<String>
        val compressed = compressionIndex >= 0
        if (compressed) {
            left = expandedInput.substring(0, compressionIndex).splitGroups()
            right = expandedInput.substring(compressionIndex + 2).splitGroups()
            if (left == INVALID_GROUPS || right == INVALID_GROUPS) return null
        } else {
            left = expandedInput.splitGroups()
            right = emptyList()
            if (left == INVALID_GROUPS) return null
        }

        val parsedGroups =
            (left + right).map { group ->
                if (group.isEmpty() || group.length > 4 || group.any { it !in HEX_DIGITS }) return null
                group.toInt(16)
            }
        val missingGroups = IPV6_GROUP_COUNT - parsedGroups.size
        if (if (compressed) missingGroups < 1 else missingGroups != 0) return null
        val groups =
            if (compressed) {
                left.map { it.toInt(16) } + List(missingGroups) { 0 } + right.map { it.toInt(16) }
            } else {
                parsedGroups
            }

        var bestStart = -1
        var bestLength = 1
        var index = 0
        while (index < groups.size) {
            if (groups[index] != 0) {
                index++
                continue
            }
            val start = index
            while (index < groups.size && groups[index] == 0) index++
            val length = index - start
            if (length > bestLength) {
                bestStart = start
                bestLength = length
            }
        }

        val canonical =
            if (groups.take(5).all { it == 0 } && groups[5] == IPV4_MAPPED_PREFIX) {
                val octets =
                    listOf(
                        groups[6] shr 8,
                        groups[6] and 0xff,
                        groups[7] shr 8,
                        groups[7] and 0xff,
                    )
                "::ffff:${octets.joinToString(".")}"
            } else if (bestStart < 0) {
                groups.joinToString(":") { it.toString(16) }
            } else {
                val before = groups.take(bestStart).joinToString(":") { it.toString(16) }
                val after = groups.drop(bestStart + bestLength).joinToString(":") { it.toString(16) }
                "$before::$after"
            }
        return "[$canonical]"
    }

    private fun expandIpv4Tail(literal: String): String? {
        if ('.' !in literal) return literal
        val separator = literal.lastIndexOf(':')
        if (separator < 0) return null
        val octets = literal.substring(separator + 1).split('.')
        if (octets.size != 4) return null
        val values =
            octets.map { part ->
                if (part.isEmpty() || (part.length > 1 && part.startsWith('0')) || !part.all { it in '0'..'9' }) {
                    return null
                }
                part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
            }
        val first = (values[0] shl 8) or values[1]
        val second = (values[2] shl 8) or values[3]
        return literal.substring(0, separator + 1) + first.toString(16) + ":" + second.toString(16)
    }

    private fun String.splitGroups(): List<String> =
        if (isEmpty()) emptyList() else split(':').takeIf { it.none(String::isEmpty) } ?: INVALID_GROUPS

    private const val ACE_PREFIX = "xn--"
    private const val HEX_DIGITS = "0123456789abcdefABCDEF"
    private const val IPV6_GROUP_COUNT = 8
    private const val IPV4_MAPPED_PREFIX = 0xffff
    private const val MAX_DNS_LABEL_LENGTH = 63
    private const val MAX_DNS_HOST_LENGTH = 253
    private const val MAX_PORT = 65535
    private val URL_SCHEME_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val INVALID_GROUPS = listOf("")
}

sealed interface AutofillDomainNormalization {
    data object Missing : AutofillDomainNormalization

    data object Invalid : AutofillDomainNormalization

    data class Valid(
        val host: String,
    ) : AutofillDomainNormalization
}
