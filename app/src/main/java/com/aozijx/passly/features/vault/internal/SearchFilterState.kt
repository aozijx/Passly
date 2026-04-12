package com.aozijx.passly.features.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.core.designsystem.model.VaultTab
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class SearchFilterState {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    @OptIn(FlowPreview::class)
    val debouncedSearchQuery: Flow<String> =
        _searchQuery.map { it.trim() }.debounce(250).distinctUntilChanged()

    val normalizedSelectedCategory: Flow<String?> =
        _selectedCategory.map { it?.trim()?.takeIf { category -> category.isNotEmpty() } }
            .distinctUntilChanged()

    val distinctSelectedTab: Flow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
        private set
    var isMoreMenuExpanded by mutableStateOf(false)
        private set

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSelectedTab(tab: VaultTab) { _selectedTab.value = tab }
    fun updateSelectedCategory(category: String?) { _selectedCategory.value = category }

    fun toggleSearch(active: Boolean) {
        isSearchActive = active
        if (!active) _searchQuery.value = ""
    }

    fun expandMoreMenu(expanded: Boolean) { isMoreMenuExpanded = expanded }
}
