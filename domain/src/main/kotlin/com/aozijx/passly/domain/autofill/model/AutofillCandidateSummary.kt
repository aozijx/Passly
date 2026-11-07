package com.aozijx.passly.domain.autofill.model

import com.aozijx.passly.domain.entry.model.query.CredentialMatch
import com.aozijx.passly.domain.entry.model.query.EntryListItem

const val AUTOFILL_CANDIDATE_LIMIT = 10

data class AutofillCandidateQuery(
    val packageName: String?,
    val webDomain: String?,
    val limit: Int = AUTOFILL_CANDIDATE_LIMIT,
    val strictDomainMatching: Boolean = true,
)

/** Metadata-only candidate. Protected field values cannot be represented here. */
data class AutofillCandidateSummary(
    val entry: EntryListItem,
    val match: CredentialMatch,
)
