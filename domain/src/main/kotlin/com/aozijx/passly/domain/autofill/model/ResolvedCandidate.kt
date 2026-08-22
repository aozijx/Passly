package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.MatchType

/**
 * A candidate entry for autofill, wrapping a domain [EntryListItem].
 *
 * It adds autofill-specific metadata generated during the matching process
 * and temporary secrets (like decrypted passwords) needed for the fill operation.
 */
data class ResolvedCandidate(
    val entry: EntryListItem,
    /** Metadata indicating how this entry was matched to the current page. */
    val matchedBy: MatchType? = null,
    val matchedPackage: String? = null,
    val matchedDomain: String? = null,
    /** The secret password, populated only in Phase 2 of autofill (after unlock/selection). */
    val password: String = "",
    /** Mapping of specific field IDs to their intended roles. */
    val roleMap: Map<String, FieldRole> = emptyMap()
) {
    /**
     * Returns a simplified model for basic credential filling.
     * Only returns non-null if either username or password is available.
     */
    fun fillableCredentials(): FillableCredentials? {
        if (entry.username.isBlank() && password.isBlank()) return null
        return FillableCredentials(username = entry.username, password = password)
    }
}

/**
 * Basic username/password pair for direct filling.
 */
data class FillableCredentials(
    val username: String,
    val password: String,
)
