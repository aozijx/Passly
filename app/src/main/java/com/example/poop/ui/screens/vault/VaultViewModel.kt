package com.example.poop.ui.screens.vault

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    var isSearchActive by mutableStateOf(false)
    var isFilterMenuExpanded by mutableStateOf(false)

    val availableCategories: StateFlow<List<String>> = dao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultItem>> = combine(_searchQuery, _selectedCategory) { query, category ->
        query to category
    }.flatMapLatest { (query, category) ->
        if (query.isNotEmpty()) {
            dao.searchItems(query)
        } else if (category != null) {
            dao.getItemsByCategory(category)
        } else {
            dao.getAllItems()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var showAddDialog by mutableStateOf(false)
        private set
    
    var addDialogTitle by mutableStateOf("")
    var addDialogUsername by mutableStateOf("")
    var addDialogPassword by mutableStateOf("")
    var addDialogCategory by mutableStateOf("")
    var addDialogPasswordVisible by mutableStateOf(false)

    var detailItem by mutableStateOf<VaultItem?>(null)
        private set
    var detailPasswordVisible by mutableStateOf(false)

    var itemToDelete by mutableStateOf<VaultItem?>(null)
        private set

    val revealedItems = mutableStateMapOf<Int, Pair<String, String>>()

    fun onAddClick() {
        resetAddDialogFields()
        showAddDialog = true
    }

    fun dismissAddDialog() {
        showAddDialog = false
    }

    private fun resetAddDialogFields() {
        addDialogTitle = ""
        addDialogUsername = ""
        addDialogPassword = ""
        addDialogCategory = ""
        addDialogPasswordVisible = false
    }

    fun showDetail(item: VaultItem) {
        detailItem = item
        detailPasswordVisible = false
    }

    fun dismissDetail() {
        detailItem?.let { clearRevealedData(it.id) }
        detailItem = null
    }

    fun requestDelete(item: VaultItem) {
        // 先关闭详情页，腾出空间给确认对话框
        dismissDetail()
        itemToDelete = item
    }

    fun dismissDeleteDialog() {
        itemToDelete = null
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleSearch(active: Boolean) {
        isSearchActive = active
        if (!active) _searchQuery.value = ""
    }

    fun toggleFilterMenu(expanded: Boolean) {
        isFilterMenuExpanded = expanded
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = category
        isFilterMenuExpanded = false
    }

    fun addItem(title: String, encryptedUser: String, encryptedPass: String, category: String) {
        viewModelScope.launch {
            dao.insert(
                VaultItem(
                    title = title,
                    username = encryptedUser,
                    password = encryptedPass,
                    category = category.trim()
                )
            )
            showAddDialog = false
            resetAddDialogFields()
        }
    }

    fun confirmDelete() {
        val item = itemToDelete ?: return
        viewModelScope.launch {
            dao.delete(item)
            itemToDelete = null
        }
    }

    fun setRevealedData(itemId: Int, username: String, password: String) {
        revealedItems[itemId] = username to password
    }

    fun clearRevealedData(itemId: Int) {
        revealedItems.remove(itemId)
    }

    fun isItemRevealed(itemId: Int): Boolean = revealedItems.containsKey(itemId)

    fun getDecryptedData(itemId: Int): Pair<String, String>? = revealedItems[itemId]
}
