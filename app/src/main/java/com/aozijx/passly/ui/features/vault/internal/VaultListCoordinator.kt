package com.aozijx.passly.ui.features.vault.internal

import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import com.aozijx.passly.ui.features.vault.model.VaultTab
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
    val items: List<VaultSummary> = emptyList(),
    val itemsByTab: Map<VaultTab, List<VaultSummary>> = emptyMap()
)

internal class VaultListCoordinator(
    private val scope: CoroutineScope,
    private val queryCoordinator: VaultQueryCoordinator,
    private val searchFilter: SearchFilterState,
    private val vaultUseCases: VaultUseCases,
    private val entryManager: EntryManager,
    private val isAutoDownloadIcons: StateFlow<Boolean>
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
    private val rawItems: Flow<List<VaultSummary>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilter.normalizedSelectedCategory,
        distinctSelectedTab = searchFilter.distinctSelectedTab
    ).onEach { items ->
        _isLoading.value = false
        if (isAutoDownloadIcons.value) {
            entryManager.downloadMissingIcons(items)
        }
    }

    private val sortedItems: StateFlow<List<VaultSummary>> = combine(
        rawItems,
        searchFilter.selectedSort
    ) { items, sort ->
        sort.apply(items)
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val categories: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab ->
            vaultUseCases.observeCategoriesByFilter(tab.entryFilter)
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
                VaultTab.PASSWORDS to items.filter { it.totpSecret.isNullOrBlank() },
                VaultTab.TOTP to items.filter { !it.totpSecret.isNullOrBlank() }
            )
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), VaultListState())
}