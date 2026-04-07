package com.aozijx.passly.domain.model

import com.aozijx.passly.domain.model.core.VaultEntry

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

