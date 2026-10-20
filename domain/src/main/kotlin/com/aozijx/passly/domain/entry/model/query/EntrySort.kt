package com.aozijx.passly.domain.entry.model.query

enum class EntrySortField(val key: String) {
    TITLE("TITLE"),
    CREATED_AT("CREATED_AT"),
    UPDATED_AT("UPDATED_AT"),
    LAST_USED_AT("LAST_USED_AT"),
    USAGE_FREQUENCY("USAGE_FREQUENCY"),
    ENTRY_TYPE("ENTRY_TYPE"),
    ID("ID");

    /** Canonical tie-breaker direction shared by in-memory and database sorting. */
    val stableDirection: SortDirection
        get() = when (this) {
            TITLE, ENTRY_TYPE, ID -> SortDirection.ASC
            CREATED_AT, UPDATED_AT, LAST_USED_AT, USAGE_FREQUENCY -> SortDirection.DESC
        }

    companion object {
        fun parseStoredKey(key: String?): EntrySortFieldParseResult {
            if (key.isNullOrBlank()) return EntrySortFieldParseResult.Missing
            val value = entries.firstOrNull { it.key == key }
            return if (value == null) {
                EntrySortFieldParseResult.Unsupported(key)
            } else {
                EntrySortFieldParseResult.Supported(value)
            }
        }
    }
}

sealed interface EntrySortFieldParseResult {
    data class Supported(val value: EntrySortField) : EntrySortFieldParseResult
    data object Missing : EntrySortFieldParseResult
    data class Unsupported(val storedValue: String) : EntrySortFieldParseResult
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

    fun toggled(): EntrySort = copy(
        direction = if (direction == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC,
    )

    companion object {
        fun presets(): List<EntrySort> = listOf(
            EntrySort(EntrySortField.CREATED_AT, SortDirection.DESC),
            EntrySort(EntrySortField.TITLE, SortDirection.ASC),
            EntrySort(EntrySortField.UPDATED_AT, SortDirection.DESC),
            EntrySort(EntrySortField.LAST_USED_AT, SortDirection.DESC),
            EntrySort(EntrySortField.USAGE_FREQUENCY, SortDirection.DESC),
        )
    }
}
