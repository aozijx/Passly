package com.aozijx.passly.features.vault.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aozijx.passly.core.common.VaultTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class VaultSearchFilterStateHolder {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedTab(tab: VaultTab) {
        _selectedTab.value = tab
    }

    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun toggleSearch(active: Boolean) {
        isSearchActive = active
        if (!active) {
            _searchQuery.value = ""
        }
    }
}
