package com.aozijx.passly.domain.entry.model

/** External identities that may resolve to this entry. */
data class EntryAssociations(
    val primaryUrl: String? = null,
    val domains: Set<String> = emptySet(),
    val applicationIds: Set<String> = emptySet(),
) {
    init {
        require(domains.none(String::isBlank)) { "Associated domains cannot be blank" }
        require(applicationIds.none(String::isBlank)) { "Associated application IDs cannot be blank" }
    }
}
