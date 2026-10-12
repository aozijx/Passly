package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.EntryListItem

data class AutofillCandidateQuery(
    val packageName: String?,
    val webDomain: String?,
    val allowUnmatched: Boolean,
    val limit: Int,
)

/** Metadata-only candidate. Protected field values cannot be represented here. */
data class AutofillCandidateSummary(
    val entry: EntryListItem,
    val match: CredentialMatch,
)
