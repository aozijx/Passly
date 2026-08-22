package com.aozijx.passly.domain.entry.model.query

enum class EntrySortField {
    TITLE,
    CREATED_AT,
    UPDATED_AT,
    LAST_USED_AT,
    USAGE_FREQUENCY,
    ENTRY_TYPE,
    ID;

    /** Canonical tie-breaker direction shared by in-memory and database sorting. */
    val stableDirection: SortDirection
        get() = when (this) {
            TITLE, ENTRY_TYPE, ID -> SortDirection.ASC
            CREATED_AT, UPDATED_AT, LAST_USED_AT, USAGE_FREQUENCY -> SortDirection.DESC
        }
}

enum class SortDirection { ASC, DESC }

/**
 * Stable ordering requested from an entry query.
 */
data class EntrySort(
    val field: EntrySortField,
    val direction: SortDirection,
    val pinFavorites: Boolean = true,
    val tieBreaker: EntrySortField = EntrySortField.ID
) {
    init {
        require(field != tieBreaker) { "Primary sort field and tie-breaker must differ" }
    }

    companion object {
        val DEFAULT = EntrySort(
            field = EntrySortField.LAST_USED_AT,
            direction = SortDirection.DESC,
            pinFavorites = true,
            tieBreaker = EntrySortField.ID
        )
    }
}
