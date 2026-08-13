package com.aozijx.passly.domain.entry.model

data class EntryProfile(
    val title: String,
    val username: String = "",
    val associations: EntryAssociations = EntryAssociations(),
    val icon: EntryIcon = EntryIcon(),
    val favorite: Boolean = false,
    val tags: Set<String> = emptySet(),
    val expiresAtMs: Long? = null,
) {
    init {
        require(title.isNotBlank()) { "Entry title cannot be blank" }
        require(tags.none(String::isBlank)) { "Entry tags cannot be blank" }
        require(expiresAtMs == null || expiresAtMs >= 0L) { "Expiration time cannot be negative" }
    }
}
