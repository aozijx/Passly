package com.aozijx.passly.feature.vault.list

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
    val entryTypes: List<String> = emptyList(),
    val itemsByTab: Map<VaultTab, List<EntryListItem>> = emptyMap()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val queryCoordinator: VaultQueryCoordinator,
    private val searchFilter: SearchFilterState,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val refreshTrigger: Flow<Long>
) {
    private val _isLoading = MutableStateFlow(true)

    private val loadingTrigger = combine(
        searchFilter.searchQuery,
        searchFilter.selectedEntryTypeName
    ) { query, entryTypeName -> Pair(query.trim(), entryTypeName?.trim()) }
        .distinctUntilChanged()

    init {
        scope.launch {
            loadingTrigger.drop(1).collect { _isLoading.value = true }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val rawItems: Flow<List<EntryListItem>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedEntryTypeName = searchFilter.normalizedSelectedEntryTypeName,
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
    private val entryTypes: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab: VaultTab ->
            entryListQueryRepository.observeEntryTypes(tab.entryFilter)
        }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<VaultListState> = combine(
        _isLoading,
        entryTypes,
        sortedItems
    ) { loading, cats, items ->
        VaultListState(
            isLoading = loading,
            entryTypes = cats,
            itemsByTab = VaultTab.entries.associateWith { tab ->
                when (tab) {
                    VaultTab.ALL -> items
                    VaultTab.PASSWORDS -> items.filter(EntryListItem::hasPassword)
                    VaultTab.TOTP -> items.filter(EntryListItem::hasOtp)
                }
            }
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())
}
