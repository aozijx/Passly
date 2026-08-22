package com.aozijx.passly.domain.entry.model.query

/** Stable, platform-independent entry list query semantics. */
data class EntryListQuery(
    val searchText: String = "",
    val filter: EntryFilter = EntryFilter.ALL,
    val category: String? = null,
    val sort: EntrySort = EntrySort.DEFAULT,
    val hierarchyMode: EntryHierarchyDisplayMode? = null,
) {
    val normalizedSearchText: String = searchText.trim().lowercase()
    val normalizedCategory: String? = category?.trim()?.takeIf(String::isNotEmpty)
}
