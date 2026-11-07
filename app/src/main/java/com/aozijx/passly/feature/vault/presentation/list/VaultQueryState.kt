package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.feature.vault.model.entryTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/** Stable inputs used to derive the vault's single Paging generation. */
internal data class VaultQueryState(
    val searchText: String,
    val category: String?,
    val sort: EntrySort,
    val entryTypes: Set<EntryType> = emptySet(),
    val hierarchyMode: EntryHierarchyDisplayMode,
) {
    fun toEntryListQuery() =
        EntryListQuery(
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
        ) = VaultQueryState(
            searchText = searchText.trim(),
            category = category?.trim()?.takeIf(String::isNotEmpty),
            sort = sort,
            entryTypes = entryTypes,
            hierarchyMode = hierarchyMode,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<VaultQueryState?>.switchRecoverableQueryGenerations(
    unavailable: Flow<T>,
    load: (EntryListQuery) -> Flow<T>,
): Flow<T> =
    map { state -> state?.toEntryListQuery() }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            query?.let(load) ?: unavailable
        }

@OptIn(FlowPreview::class)
internal fun buildRecoverableVaultQueryStates(
    uiStates: Flow<VaultUiState>,
    librarySettings: Flow<RecoverableReadState<LibraryViewSettings>>,
): Flow<VaultQueryState?> {
    val (effectiveSearch, selection) = uiStates.queryInputs()

    return combine(effectiveSearch, selection, librarySettings.distinctUntilChanged()) { search, currentSelection, read ->
        val settings =
            when (read) {
                is RecoverableReadState.Ready -> read.value
                else -> return@combine null
            }
        VaultQueryState.create(
            searchText = search,
            category = currentSelection.category,
            sort = currentSelection.sort,
            entryTypes = currentSelection.entryTypes,
            hierarchyMode = settings.entryHierarchyDisplayMode,
        )
    }.distinctUntilChanged()
}

@OptIn(FlowPreview::class)
private fun Flow<VaultUiState>.queryInputs(): Pair<Flow<String>, Flow<VaultQuerySelection>> {
    val effectiveSearch =
        map { state -> state.searchQuery.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .distinctUntilChanged()
    val selection =
        mapNotNull { state ->
            val sort = state.selectedSort ?: return@mapNotNull null
            VaultQuerySelection(
                state.selectedCategory,
                sort,
                state.selectedFilters.flatMap { it.entryTypes }.toSet(),
            )
        }.distinctUntilChanged()
    return effectiveSearch to selection
}

private data class VaultQuerySelection(
    val category: String?,
    val sort: EntrySort,
    val entryTypes: Set<EntryType>,
)

private const val SEARCH_DEBOUNCE_MILLIS = 250L
