package com.aozijx.passly.features.vault.internal

import com.aozijx.passly.features.vault.model.VaultTab
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
    val isSearchActive: Boolean = false,
    val isMoreMenuExpanded: Boolean = false
)

internal class SearchFilterState(scope: CoroutineScope) {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _isMoreMenuExpanded = MutableStateFlow(false)
    val isMoreMenuExpanded: StateFlow<Boolean> = _isMoreMenuExpanded

    val uiStateFlow: StateFlow<SearchFilterUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedTab,
        _isSearchActive,
        _isMoreMenuExpanded
    ) { query, category, tab, active, expanded ->
        SearchFilterUiState(query, category, tab, active, expanded)
    }.stateIn(scope, SharingStarted.Eagerly, SearchFilterUiState())

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: Flow<String> =
        _searchQuery.map { it.trim() }.debounce(250).distinctUntilChanged()

    val normalizedSelectedCategory: Flow<String?> =
        _selectedCategory.map { it?.trim()?.takeIf { category -> category.isNotEmpty() } }
            .distinctUntilChanged()

    val distinctSelectedTab: Flow<VaultTab> = _selectedTab

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedTab(tab: VaultTab) { _selectedTab.value = tab }
    fun updateSelectedCategory(category: String?) { _selectedCategory.value = category }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }

    fun expandMoreMenu(expanded: Boolean) {
        _isMoreMenuExpanded.value = expanded
    }
}