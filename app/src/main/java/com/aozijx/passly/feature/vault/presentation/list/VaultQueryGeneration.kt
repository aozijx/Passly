package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
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

internal sealed interface VaultQueryGeneration {
    data object Unavailable : VaultQueryGeneration

    data class Available(
        val query: EntryListQuery,
    ) : VaultQueryGeneration
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
                is VaultQueryGeneration.Available -> load(generation.query)
            }
        }

@OptIn(FlowPreview::class)
internal fun buildRecoverableVaultQueryGenerations(
    uiStates: Flow<VaultUiState>,
    librarySettings: Flow<RecoverableReadState<LibraryViewSettings>>,
): Flow<VaultQueryGeneration> {
    val (effectiveSearch, selection) = uiStates.queryInputs()

    return combine(effectiveSearch, selection, librarySettings.distinctUntilChanged()) { search, currentSelection, read ->
        val settings =
            when (read) {
                is RecoverableReadState.Ready -> read.value

                RecoverableReadState.Loading,
                is RecoverableReadState.Failed,
                -> return@combine VaultQueryGeneration.Unavailable
            }
        val entryTypes = currentSelection.entryTypes
        VaultQueryGeneration.Available(
            EntryListQuery(
                searchText = search,
                category = currentSelection.categorySelection.queryCategory,
                sort = settings.sort,
                entryTypes = entryTypes,
                hierarchyMode = settings.entryHierarchyDisplayMode.takeIf { entryTypes.isEmpty() },
            ),
        )
    }.distinctUntilChanged()
}

private val VaultCategorySelection.queryCategory: String?
    get() =
        when (this) {
            VaultCategorySelection.All -> null
            is VaultCategorySelection.Selected -> name.trim()
        }

@OptIn(FlowPreview::class)
private fun Flow<VaultUiState>.queryInputs(): Pair<Flow<String>, Flow<VaultQuerySelection>> {
    val effectiveSearch =
        map { state -> state.search.query.trim() }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .distinctUntilChanged()
    val selection =
        map { state ->
            VaultQuerySelection(
                state.categorySelection,
                state.selectedFilters.flatMap { it.entryTypes }.toSet(),
            )
        }.distinctUntilChanged()
    return effectiveSearch to selection
}

private data class VaultQuerySelection(
    val categorySelection: VaultCategorySelection,
    val entryTypes: Set<EntryType>,
)

private const val SEARCH_DEBOUNCE_MILLIS = 250L
