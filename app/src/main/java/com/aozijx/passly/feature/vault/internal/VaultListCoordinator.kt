package com.aozijx.passly.feature.vault.internal

import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.entry.service.VaultListSorter
import com.aozijx.passly.feature.vault.model.VaultTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultListState(
    val isLoading: Boolean = true,
    val categories: List<String> = emptyList(),
    val items: List<EntryListItem> = emptyList(),
    val itemsByTab: Map<VaultTab, List<EntryListItem>> = emptyMap()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val queryCoordinator: VaultQueryCoordinator,
    private val searchFilter: SearchFilterState,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryManager: EntryManager,
    private val isAutoDownloadIcons: StateFlow<Boolean>,
    private val refreshTrigger: Flow<Long>
) {
    private val _isLoading = MutableStateFlow(true)

    private val loadingTrigger = combine(
        searchFilter.searchQuery,
        searchFilter.selectedCategory
    ) { query, category -> Pair(query.trim(), category?.trim()) }
        .distinctUntilChanged()

    init {
        scope.launch {
            loadingTrigger.drop(1).collect { _isLoading.value = true }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawItems: Flow<List<EntryListItem>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilter.normalizedSelectedCategory,
        distinctSelectedTab = searchFilter.distinctSelectedTab,
        refreshTrigger = refreshTrigger
    ).onEach { items ->
        _isLoading.value = false
    }

    private val sortedItems: StateFlow<List<EntryListItem>> = combine(
        rawItems,
        searchFilter.selectedSort
    ) { items, sort ->
        VaultListSorter.sort(items, sort)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val categories: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab: VaultTab ->
            entryListQueryRepository.observeCategories(tab.entryFilter)
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
            itemsByTab = mapOf(
                VaultTab.ALL to items,
                VaultTab.PASSWORDS to items.filter { it.hasPassword },
                VaultTab.TOTP to items.filter { it.hasOtp }
            )
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())
}
