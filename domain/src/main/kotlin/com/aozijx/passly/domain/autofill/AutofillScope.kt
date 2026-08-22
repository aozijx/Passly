package com.aozijx.passly.domain.autofill

import com.aozijx.passly.domain.entry.model.Entry
import java.net.URI

/** Owns canonical package/domain matching for every autofill integration. */
object AutofillScope {

    fun normalizeApplicationId(value: String?): String? =
        value?.trim()?.lowercase()?.takeIf(String::isNotBlank)

    /**
     * Normalizes a web domain or URL string into a consistent host format.
     * e.g., "https://www.google.com/login" -> "google.com"
     */
    fun normalizeDomain(value: String?): String? {
        val trimmed = value?.trim()?.lowercase() ?: return null
        if (trimmed.isBlank()) return null

        val cleanUrl = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        return URI(cleanUrl).host
            ?.removePrefix("www.")
            ?.removeSuffix(".")
            ?.takeIf { it.isNotBlank() && it.contains(".") }
    }

    fun matches(entry: Entry, packageName: String?, webDomain: String?): Boolean {
        val applicationId = normalizeApplicationId(packageName)
        if (applicationId != null && entry.profile.associations.applicationIds.any {
                normalizeApplicationId(it) == applicationId
            }) {
            return true
        }

        val domain = normalizeDomain(webDomain) ?: return false
        val entryDomains = buildSet {
            addAll(entry.profile.associations.domains)
            entry.profile.associations.primaryUrl?.let(::add)
        }
        return entryDomains.any { normalizeDomain(it) == domain }
    }
}
