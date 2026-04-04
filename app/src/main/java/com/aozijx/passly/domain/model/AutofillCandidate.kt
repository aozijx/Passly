package com.aozijx.passly.domain.model

enum class AutofillMatchType {
    APP,
    DOMAIN,
    UNKNOWN
}

data class AutofillCandidate(
    val entry: VaultEntry,
    val matchType: AutofillMatchType,
    val rank: Int
)

