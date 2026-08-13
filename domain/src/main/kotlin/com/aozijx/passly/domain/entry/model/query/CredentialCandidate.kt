package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.Entry

enum class LookupField { TITLE, USERNAME, EMAIL, DOMAIN, URL, APPLICATION_ID }

data class LookupFieldValue(
    val field: LookupField,
    val text: String,
) {
    init {
        require(text.isNotBlank()) { "Lookup field text cannot be blank" }
    }
}

enum class MatchType(val score: Int) {
    APPLICATION_ID(100),
    VERIFIED_ASSOCIATION(90),
    WEB_DOMAIN(80),
    TITLE(60),
    URL(40),
    UNKNOWN(0),
}

data class CredentialMatch(
    val type: MatchType,
    val applicationId: String? = null,
    val domain: String? = null,
) {
    val score: Int get() = type.score
}

data class CredentialCandidate(
    val entry: Entry,
    val match: CredentialMatch,
)
