package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.feature.vault.model.VaultTab
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
import kotlinx.coroutines.flow.stateIn

internal data class SearchFilterUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedTab: VaultTab = VaultTab.ALL,
    val selectedSort: VaultSortSpec = VaultSortSpec.DEFAULT,
    val isSearchActive: Boolean = false
)

internal class SearchFilterState(
    scope: CoroutineScope,
    initialSort: VaultSortSpec = VaultSortSpec.DEFAULT
) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _selectedSort = MutableStateFlow(initialSort)
    val selectedSort: StateFlow<VaultSortSpec> = _selectedSort

    val uiStateFlow: StateFlow<SearchFilterUiState> = combine(
        combine(
            _searchQuery,
            _selectedCategory,
            _selectedTab,
            _isSearchActive
        ) { query, category, tab, active ->
            PartialState(query, category, tab, active)
        },
        _selectedSort
    ) { partial, sort ->
        SearchFilterUiState(
            searchQuery = partial.query,
            selectedCategory = partial.category,
            selectedTab = partial.tab,
            selectedSort = sort,
            isSearchActive = partial.active
        )
    }.stateIn(scope, SharingStarted.Eagerly, SearchFilterUiState())

    private data class PartialState(
        val query: String,
        val category: String?,
        val tab: VaultTab,
        val active: Boolean
    )

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: Flow<String> =
        _searchQuery.map { it.trim() }.debounce(250).distinctUntilChanged()

    val normalizedSelectedCategory: Flow<String?> =
        _selectedCategory.map { it?.trim()?.takeIf { category -> category.isNotEmpty() } }
            .distinctUntilChanged()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedTab(tab: VaultTab) { _selectedTab.value = tab }
    fun updateSelectedCategory(category: String?) { _selectedCategory.value = category }
    fun updateSelectedSort(sort: VaultSortSpec) {
        _selectedSort.value = sort
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }
}
