package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Shared query inputs used to derive each quick-filter page's Paging generation. */
internal data class VaultQueryState(
    val searchText: String,
    val category: String?,
    val sort: EntrySort,
    val hierarchyMode: EntryHierarchyDisplayMode,
    val reloadVersion: Long,
) {
    fun toEntryListQuery(quickFilter: LibraryQuickFilter) = EntryListQuery(
        searchText = searchText,
        filter = quickFilter.entryFilter,
        category = category,
        sort = sort,
        hierarchyMode = hierarchyMode.takeIf { quickFilter == LibraryQuickFilter.ALL },
    )

    companion object {
        fun create(
            searchText: String,
            category: String?,
            sort: EntrySort,
            hierarchyMode: EntryHierarchyDisplayMode,
            reloadVersion: Long,
        ) = VaultQueryState(
            searchText = searchText.trim(),
            category = category?.trim()?.takeIf(String::isNotEmpty),
            sort = sort,
            hierarchyMode = hierarchyMode,
            reloadVersion = reloadVersion,
        )
    }
}

private data class VaultQueryGeneration(
    val query: EntryListQuery,
    val reloadVersion: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<VaultQueryState>.switchQueryGenerations(
    quickFilter: LibraryQuickFilter,
    load: (EntryListQuery) -> Flow<T>,
): Flow<T> = map { state ->
    VaultQueryGeneration(
        query = state.toEntryListQuery(quickFilter),
        reloadVersion = state.reloadVersion,
    )
}.distinctUntilChanged().flatMapLatest { generation -> load(generation.query) }

@OptIn(FlowPreview::class)
internal fun buildVaultQueryStates(
    uiStates: Flow<VaultUiState>,
    hierarchyModes: Flow<EntryHierarchyDisplayMode>,
    reloadVersions: Flow<Long>,
): Flow<VaultQueryState> {
    val effectiveSearch = uiStates
        .map { state -> state.searchQuery.trim() }
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
    val selection = uiStates
        .map { state -> VaultQuerySelection(state.selectedCategory, state.selectedSort) }
        .distinctUntilChanged()

    return combine(
        effectiveSearch,
        selection,
        hierarchyModes.distinctUntilChanged(),
        reloadVersions,
    ) { search, currentSelection, hierarchy, reloadVersion ->
        VaultQueryState.create(
            searchText = search,
            category = currentSelection.category,
            sort = currentSelection.sort,
            hierarchyMode = hierarchy,
            reloadVersion = reloadVersion,
        )
    }.distinctUntilChanged()
}

private data class VaultQuerySelection(
    val category: String?,
    val sort: EntrySort,
)

private const val SEARCH_DEBOUNCE_MILLIS = 250L
