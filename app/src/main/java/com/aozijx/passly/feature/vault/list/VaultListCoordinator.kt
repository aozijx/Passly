package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.service.VaultListSorter
import com.aozijx.passly.domain.settings.model.VaultQuickFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultListState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val itemsByQuickFilter: Map<VaultQuickFilter, List<EntryListItem>> = emptyMap()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val queryCoordinator: VaultQueryCoordinator,
    private val searchFilter: SearchFilterState,
    private val refreshTrigger: Flow<Long>
) {
    private val _isLoading = MutableStateFlow(true)

    private val loadingTrigger = combine(
        searchFilter.searchQuery,
        searchFilter.selectedQuickFilter
    ) { query, quickFilter ->
        LoadingKey(query.trim(), quickFilter)
    }
        .distinctUntilChanged()

    init {
        scope.launch {
            loadingTrigger.drop(1).collect { _isLoading.value = true }
        }
    }

    private val rawItems: StateFlow<List<EntryListItem>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        refreshTrigger = refreshTrigger
    ).onEach { _ ->
        _isLoading.value = false
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val categories: StateFlow<List<String>> = rawItems
        .map { items ->
            items.flatMap { item -> item.tags }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val sortedItems: StateFlow<List<EntryListItem>> = combine(
        rawItems,
        searchFilter.selectedSort,
        searchFilter.normalizedSelectedCategory
    ) { items, sort, selectedCategory ->
        val filteredItems = selectedCategory?.let { category ->
            items.filter { item ->
                item.tags.any { it.equals(category, ignoreCase = true) }
            }
        } ?: items
        VaultListSorter.sort(filteredItems, sort)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<VaultListState> = combine(
        _isLoading,
        categories,
        sortedItems
    ) { loading, cats, items ->
        VaultListState(
            isLoading = loading,
            categories = cats,
            itemsByQuickFilter = VaultQuickFilter.entries.associateWith { quickFilter ->
                when (quickFilter) {
                    VaultQuickFilter.ALL -> items
                    VaultQuickFilter.PASSWORDS -> items.filter(EntryListItem::hasPassword)
                    VaultQuickFilter.TOTP -> items.filter(EntryListItem::hasOtp)
                }
            }
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())

    private data class LoadingKey(
        val query: String,
        val quickFilter: VaultQuickFilter
    )
}
