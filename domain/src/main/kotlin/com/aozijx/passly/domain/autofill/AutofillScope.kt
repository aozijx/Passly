package com.aozijx.passly.domain.autofill

import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/** Owns canonical package/domain matching for every autofill integration. */
object AutofillScope {
    fun normalizeApplicationId(value: String?): String? = value?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)

    /**
     * Normalizes a web domain or URL string into a consistent host format.
     * Web scopes use an exact ASCII host. URL scheme, port and path do not identify a website credential.
     */
    fun normalizeDomain(value: String?): AutofillDomainNormalization {
        val trimmed = value?.trim()?.takeIf(String::isNotEmpty) ?: return AutofillDomainNormalization.Missing
        val cleanUrl = if (URL_SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri =
            try {
                URI(cleanUrl)
            } catch (_: URISyntaxException) {
                return AutofillDomainNormalization.Invalid
            }
        val host = uri.host?.lowercase(Locale.ROOT) ?: return AutofillDomainNormalization.Invalid
        if (uri.rawUserInfo != null) return AutofillDomainNormalization.Invalid

        val canonicalHost =
            if (host.startsWith("[") && host.endsWith("]")) {
                if ('%' in host || !IPV6_LITERAL.matches(host)) return AutofillDomainNormalization.Invalid
                host
            } else {
                val withoutRootDot = host.removeSuffix(".")
                if (!isAsciiDomain(withoutRootDot)) return AutofillDomainNormalization.Invalid
                withoutRootDot
            }
        return AutofillDomainNormalization.Valid(canonicalHost)
    }

    private fun isAsciiDomain(host: String): Boolean =
        host.length in 1..253 &&
            '.' in host &&
            host.split('.').all { label ->
                label.length in 1..63 && DNS_LABEL.matches(label)
            }

    private val URL_SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val DNS_LABEL = Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")
    private val IPV6_LITERAL = Regex("\\[[0-9a-f:.]+]")
}

sealed interface AutofillDomainNormalization {
    data object Missing : AutofillDomainNormalization

    data object Invalid : AutofillDomainNormalization

    data class Valid(
        val host: String,
    ) : AutofillDomainNormalization
}
