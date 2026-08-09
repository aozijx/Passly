package com.aozijx.passly.feature.vault.list

import com.aozijx.passly.domain.settings.model.VaultQuickFilter
import com.aozijx.passly.domain.settings.model.VaultSortSpec
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
    val selectedQuickFilter: VaultQuickFilter = VaultQuickFilter.ALL,
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

    private val _selectedQuickFilter = MutableStateFlow(VaultQuickFilter.ALL)
    val selectedQuickFilter: StateFlow<VaultQuickFilter> = _selectedQuickFilter

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive

    private val _selectedSort = MutableStateFlow(initialSort)
    val selectedSort: StateFlow<VaultSortSpec> = _selectedSort

    val uiStateFlow: StateFlow<SearchFilterUiState> = combine(
        combine(
            _searchQuery,
            _selectedCategory,
            _selectedQuickFilter,
            _isSearchActive
        ) { query, category, quickFilter, active ->
            PartialState(query, category, quickFilter, active)
        },
        _selectedSort
    ) { partial, sort ->
        SearchFilterUiState(
            searchQuery = partial.query,
            selectedCategory = partial.category,
            selectedQuickFilter = partial.quickFilter,
            selectedSort = sort,
            isSearchActive = partial.active
        )
    }.stateIn(scope, SharingStarted.Eagerly, SearchFilterUiState())

    private data class PartialState(
        val query: String,
        val category: String?,
        val quickFilter: VaultQuickFilter,
        val active: Boolean
    )

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: Flow<String> =
        _searchQuery.map { it.trim() }.debounce(250).distinctUntilChanged()

    val normalizedSelectedCategory: Flow<String?> =
        _selectedCategory.map { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinctUntilChanged()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedQuickFilter(quickFilter: VaultQuickFilter) {
        _selectedQuickFilter.value = quickFilter
    }

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun updateSelectedSort(sort: VaultSortSpec) {
        _selectedSort.value = sort
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }
}
