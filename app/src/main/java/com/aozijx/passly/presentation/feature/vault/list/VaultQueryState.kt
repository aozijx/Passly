package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.model.entryTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Stable inputs used to derive the vault's single Paging generation. */
internal data class VaultQueryState(
    val searchText: String,
    val category: String?,
    val sort: EntrySort,
    val entryTypes: Set<EntryType> = emptySet(),
    val hierarchyMode: EntryHierarchyDisplayMode,
    val reloadVersion: Long,
) {
    fun toEntryListQuery() = EntryListQuery(
        searchText = searchText,
        entryTypes = entryTypes,
        category = category,
        sort = sort,
        hierarchyMode = hierarchyMode.takeIf { entryTypes.isEmpty() },
    )

    companion object {
        fun create(
            searchText: String,
            category: String?,
            sort: EntrySort,
            entryTypes: Set<EntryType> = emptySet(),
            hierarchyMode: EntryHierarchyDisplayMode,
            reloadVersion: Long,
        ) = VaultQueryState(
            searchText = searchText.trim(),
            category = category?.trim()?.takeIf(String::isNotEmpty),
            sort = sort,
            entryTypes = entryTypes,
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
    load: (EntryListQuery) -> Flow<T>,
): Flow<T> = map { state ->
    VaultQueryGeneration(
        query = state.toEntryListQuery(),
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
        .map { state ->
            VaultQuerySelection(
                state.selectedCategory,
                state.selectedSort,
                state.selectedFilters.flatMap { it.entryTypes }.toSet(),
            )
        }
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
            entryTypes = currentSelection.entryTypes,
            hierarchyMode = hierarchy,
            reloadVersion = reloadVersion,
        )
    }.distinctUntilChanged()
}

private data class VaultQuerySelection(
    val category: String?,
    val sort: EntrySort,
    val entryTypes: Set<EntryType>,
)

private const val SEARCH_DEBOUNCE_MILLIS = 250L
