package com.aozijx.passly.domain.autofill.policy

import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.MatchType
import java.net.URI

/**
 * Exact post-decryption scope verification shared by every credential entry point.
 */
object CredentialScopeMatcher {
    fun matchType(
        entry: VaultEntry,
        packageName: String?,
        webDomain: String?,
    ): MatchType {
        val requestedPackage = normalizePackage(packageName)
        if (requestedPackage != null &&
            entry.website?.packageNames.orEmpty().any {
                normalizePackage(it) == requestedPackage
            }
        ) {
            return MatchType.PACKAGE_NAME
        }

        val requestedDomain = normalizeDomain(webDomain)
        if (requestedDomain != null) {
            val entryDomains = buildSet {
                addAll(entry.website?.matchDomains.orEmpty())
                entry.website?.primaryUrl?.let(::add)
            }
        if (entryDomains.any { normalizeDomain(it) == requestedDomain }) return MatchType.WEB_DOMAIN
        }
        return MatchType.UNKNOWN
    }

    fun matches(
        entry: VaultEntry,
        packageName: String?,
        webDomain: String?,
    ): Boolean = matchType(entry, packageName, webDomain) != MatchType.UNKNOWN

    fun normalizePackage(value: String?): String? =
        value?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

    fun normalizeDomain(value: String?): String? {
        val normalized = value
            ?.trim()
            ?.lowercase()
            ?.removeSuffix(".")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching {
            val withScheme = if ("://" in normalized) normalized else "https://$normalized"
            URI(withScheme).host?.lowercase()?.removeSuffix(".")
        }.getOrNull() ?: normalized.substringBefore('/').substringBefore(':')
    }
}
