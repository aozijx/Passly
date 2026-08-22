package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.policy.EntryListProjector
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.model.query.EntryListQuery
import com.aozijx.passly.domain.entry.model.query.EntryFilter
import com.aozijx.passly.feature.vault.contract.VaultUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class VaultListState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val items: List<EntryListItem> = emptyList()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val uiState: StateFlow<VaultUiState>,
    private val refreshTrigger: Flow<Long>
) {
    private val _isLoading = MutableStateFlow(true)

    private val searchQuery = uiState
        .map { it.searchQuery }
        .distinctUntilChanged()
    private val selectedSort = uiState
        .map { it.selectedSort }
        .distinctUntilChanged()
    private val selectedCategory = uiState
        .map { it.selectedCategory }
        .distinctUntilChanged()

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: StateFlow<String> = searchQuery
        .map(String::trim)
        .debounce(250)
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), "")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawItems: StateFlow<List<EntryListItem>> = refreshTrigger
        .flatMapLatest { entryListQueryRepository.activeSummaries }
        .onEach { _isLoading.value = false }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<String>> = rawItems
        .map(EntryListProjector::categories)
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sortedItems: StateFlow<List<EntryListItem>> = combine(
        rawItems,
        debouncedSearchQuery,
        selectedSort,
        selectedCategory.map { category -> category?.trim()?.takeIf(String::isNotEmpty) }
    ) { items, query, sort, category ->
        EntryListProjector.project(
            items,
            EntryListQuery(
                searchText = query,
                filter = EntryFilter.ALL,
                category = category,
                sort = sort,
            ),
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<VaultListState> = combine(
        _isLoading,
        categories,
        sortedItems
    ) { loading, cats, items ->
        VaultListState(
            isLoading = loading,
            categories = cats,
            items = items,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())
}
