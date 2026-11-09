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

internal sealed interface VaultQueryGeneration {
    data object Unavailable : VaultQueryGeneration

    data class Available(val query: VaultQueryState) : VaultQueryGeneration
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<VaultQueryGeneration>.switchRecoverableQueryGenerations(
    unavailable: Flow<T>,
    load: (EntryListQuery) -> Flow<T>,
): Flow<T> =
    distinctUntilChanged()
        .flatMapLatest { generation ->
            when (generation) {
                VaultQueryGeneration.Unavailable -> unavailable
                is VaultQueryGeneration.Available -> load(generation.query.toEntryListQuery())
            }
        }

@OptIn(FlowPreview::class)
internal fun buildRecoverableVaultQueryStates(
    uiStates: Flow<VaultUiState>,
    librarySettings: Flow<RecoverableReadState<LibraryViewSettings>>,
): Flow<VaultQueryGeneration> {
    val (effectiveSearch, selection) = uiStates.queryInputs()

    return combine(effectiveSearch, selection, librarySettings.distinctUntilChanged()) { search, currentSelection, read ->
        val settings =
            when (read) {
                is RecoverableReadState.Ready -> read.value
                else -> return@combine VaultQueryGeneration.Unavailable
            }
        VaultQueryGeneration.Available(
            VaultQueryState.create(
                searchText = search,
                category = currentSelection.category,
                sort = settings.sort,
                entryTypes = currentSelection.entryTypes,
                hierarchyMode = settings.entryHierarchyDisplayMode,
            ),
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
        map { state ->
            VaultQuerySelection(
                state.selectedCategory,
                state.selectedFilters.flatMap { it.entryTypes }.toSet(),
            )
        }.distinctUntilChanged()
    return effectiveSearch to selection
}

private data class VaultQuerySelection(
    val category: String?,
    val entryTypes: Set<EntryType>,
)

private const val SEARCH_DEBOUNCE_MILLIS = 250L
