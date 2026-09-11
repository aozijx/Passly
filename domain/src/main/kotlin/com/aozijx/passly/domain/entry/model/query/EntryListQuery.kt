package com.aozijx.passly.domain.entry.model.query

import com.aozijx.passly.domain.entry.model.EntryType

/** Stable, platform-independent entry list query semantics. */
data class EntryListQuery(
    val searchText: String = "",
    val entryTypes: Set<EntryType> = emptySet(),
    val category: String? = null,
    val sort: EntrySort = EntrySort.DEFAULT,
    val hierarchyMode: EntryHierarchyDisplayMode? = null,
) {
    val normalizedSearchText: String = searchText.trim().lowercase()
    val normalizedCategory: String? = category?.trim()?.takeIf(String::isNotEmpty)
}
