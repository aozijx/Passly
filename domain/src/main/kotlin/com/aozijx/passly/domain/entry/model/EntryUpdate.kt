package com.aozijx.passly.domain.entry.model

data class EntryUpdate(
    val profile: EntryProfile? = null,
    val secret: EntrySecret? = null,
) {
    init {
        require(profile != null || secret != null) { "Entry update cannot be empty" }
    }
}
