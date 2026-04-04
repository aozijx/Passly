package com.aozijx.passly.service.autofill

import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.service.autofill.engine.AutofillStructureParser

internal data class AutofillMatchScope(
    val normalizedPackage: String?,
    val normalizedDomain: String?
)

internal object AutofillMatchingSupport {
    fun createScope(packageName: String?, webDomain: String?): AutofillMatchScope {
        return AutofillMatchScope(
            normalizedPackage = packageName?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
            normalizedDomain = AutofillStructureParser.normalizeDomain(webDomain)
        )
    }

    fun buildCandidates(
        entries: List<VaultEntry>,
        scope: AutofillMatchScope,
        supportsAutofill: (VaultEntry) -> Boolean
    ): List<AutofillRepository.AutofillCandidate> {
        return entries.asSequence()
            .mapNotNull { entry ->
                if (!supportsAutofill(entry)) return@mapNotNull null

                val matchType = when {
                    isPackageMatch(entry.associatedAppPackage, scope.normalizedPackage) -> AutofillRepository.MatchType.APP
                    isDomainMatch(entry.associatedDomain, scope.normalizedDomain) -> AutofillRepository.MatchType.DOMAIN
                    else -> AutofillRepository.MatchType.UNKNOWN
                }
                if (matchType == AutofillRepository.MatchType.UNKNOWN) return@mapNotNull null

                val rank = if (matchType == AutofillRepository.MatchType.APP) 0 else 1
                AutofillRepository.AutofillCandidate(entry = entry, matchType = matchType, rank = rank)
            }
            .sortedWith(
                compareBy<AutofillRepository.AutofillCandidate> { it.rank }
                    .thenByDescending { it.entry.favorite }
                    .thenByDescending { it.entry.usageCount }
                    .thenByDescending { it.entry.updatedAt ?: it.entry.createdAt ?: 0L }
            )
            .toList()
    }

    fun findExistingByScopeAndUsername(
        entries: List<VaultEntry>,
        scope: AutofillMatchScope,
        usernameValue: String,
        supportsAutofill: (VaultEntry) -> Boolean,
        decryptUsername: (VaultEntry) -> String?
    ): VaultEntry? {
        return entries.find { entry ->
            if (!supportsAutofill(entry)) return@find false
            val scopeMatch = isPackageMatch(entry.associatedAppPackage, scope.normalizedPackage) ||
                isDomainMatch(entry.associatedDomain, scope.normalizedDomain)
            scopeMatch && decryptUsername(entry) == usernameValue
        }
    }

    private fun isPackageMatch(entryPackage: String?, normalizedRequestPackage: String?): Boolean {
        if (normalizedRequestPackage == null) return false
        return entryPackage?.trim()?.lowercase() == normalizedRequestPackage
    }

    private fun isDomainMatch(entryDomain: String?, normalizedRequestDomain: String?): Boolean {
        if (normalizedRequestDomain == null) return false
        val normalizedEntryDomain = AutofillStructureParser.normalizeDomain(entryDomain) ?: return false
        return normalizedEntryDomain == normalizedRequestDomain ||
            normalizedEntryDomain.endsWith(".$normalizedRequestDomain") ||
            normalizedRequestDomain.endsWith(".$normalizedEntryDomain")
    }
}

