package com.example.poop.ui.screens.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
import com.example.poop.util.BackupManager
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
    
    private val handler = Handler(Looper.getMainLooper())
    private val autoHideTasks = mutableMapOf<Int, Runnable>()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    var isSearchActive by mutableStateOf(false)
    var isFilterMenuExpanded by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)

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

    // 新增对话框状态
    var showAddDialog by mutableStateOf(false)
        private set
    
    var addDialogTitle by mutableStateOf("")
    var addDialogUsername by mutableStateOf("")
    var addDialogPassword by mutableStateOf("")
    var addDialogCategory by mutableStateOf("")
    var addDialogPasswordVisible by mutableStateOf(false)

    // 详情对话框状态
    var detailItem by mutableStateOf<VaultItem?>(null)
        private set
    var detailPasswordVisible by mutableStateOf(false)
    var isEditingCategory by mutableStateOf(false)
    var editedCategory by mutableStateOf("")
    
    // 修改账号和密码的独立状态
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")

    // 删除确认状态
    var itemToDelete by mutableStateOf<VaultItem?>(null)
        private set

    // 备份/导入状态
    var isBackupLoading by mutableStateOf(false)
    var backupMessage by mutableStateOf<String?>(null)
    var showBackupPasswordDialog by mutableStateOf(false)
    var isExporting by mutableStateOf(true)
    var pendingUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupManager.ImportMode.OVERWRITE)

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
        isEditingCategory = false
        isEditingUsername = false
        isEditingPassword = false
        editedCategory = item.category
    }

    fun dismissDetail() {
        detailItem?.let { clearRevealedData(it.id) }
        detailItem = null
        isEditingCategory = false
        isEditingUsername = false
        isEditingPassword = false
    }

    fun startEditingCategory() {
        isEditingCategory = true
        editedCategory = detailItem?.category ?: ""
    }

    fun saveCategoryEdit() {
        val item = detailItem ?: return
        val newCategory = editedCategory.trim()
        if (newCategory.isNotEmpty() && newCategory != item.category) {
            viewModelScope.launch {
                val updatedItem = item.copy(category = newCategory)
                dao.update(updatedItem)
                detailItem = updatedItem
                isEditingCategory = false
            }
        } else {
            isEditingCategory = false
        }
    }
    
    fun startEditingUsername(currentUsername: String) {
        editedUsername = currentUsername
        isEditingUsername = true
    }
    
    fun startEditingPassword(currentPassword: String) {
        editedPassword = currentPassword
        isEditingPassword = true
    }
    
    fun cancelEditingUsername() {
        isEditingUsername = false
    }
    
    fun cancelEditingPassword() {
        isEditingPassword = false
    }

    fun saveUsernameEdit(encryptedUsername: String) {
        val item = detailItem ?: return
        viewModelScope.launch {
            val updatedItem = item.copy(username = encryptedUsername)
            dao.update(updatedItem)
            detailItem = updatedItem
            // 更新缓存
            val currentData = revealedItems[item.id]
            revealedItems[item.id] = editedUsername to (currentData?.second ?: "")
            isEditingUsername = false
        }
    }

    fun savePasswordEdit(encryptedPassword: String) {
        val item = detailItem ?: return
        viewModelScope.launch {
            val updatedItem = item.copy(password = encryptedPassword)
            dao.update(updatedItem)
            detailItem = updatedItem
            // 更新缓存
            val currentData = revealedItems[item.id]
            revealedItems[item.id] = (currentData?.first ?: "") to editedPassword
            isEditingPassword = false
        }
    }

    fun requestDelete(item: VaultItem) {
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

    fun toggleMoreMenu(expanded: Boolean) {
        isMoreMenuExpanded = expanded
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
        autoHideTasks[itemId]?.let { handler.removeCallbacks(it) }
        val hideTask = Runnable { clearRevealedData(itemId) }
        autoHideTasks[itemId] = hideTask
        handler.postDelayed(hideTask, 60000L)
    }

    fun clearRevealedData(itemId: Int) {
        revealedItems.remove(itemId)
        autoHideTasks[itemId]?.let {
            handler.removeCallbacks(it)
            autoHideTasks.remove(itemId)
        }
    }

    fun isItemRevealed(itemId: Int): Boolean = revealedItems.containsKey(itemId)

    fun getDecryptedData(itemId: Int): Pair<String, String>? = revealedItems[itemId]

    fun startExport(uri: Uri) {
        pendingUri = uri
        isExporting = true
        backupPassword = ""
        showBackupPasswordDialog = true
    }

    fun startImport(uri: Uri) {
        pendingUri = uri
        isExporting = false
        backupPassword = ""
        importMode = BackupManager.ImportMode.OVERWRITE
        showBackupPasswordDialog = true
    }

    fun dismissBackupPasswordDialog() {
        showBackupPasswordDialog = false
        pendingUri = null
    }

    fun processBackupAction(context: Context) {
        val uri = pendingUri ?: return
        val password = backupPassword.toCharArray()
        showBackupPasswordDialog = false
        viewModelScope.launch {
            isBackupLoading = true
            val result = if (isExporting) {
                BackupManager.exportBackup(context, uri, password)
            } else {
                BackupManager.importBackup(context, uri, password, importMode)
            }
            isBackupLoading = false
            backupMessage = if (result.isSuccess) {
                if (isExporting) "数据备份导出成功" else "数据导入成功"
            } else {
                val error = result.exceptionOrNull()?.message ?: "操作失败"
                "${if (isExporting) "导出" else "导入"}失败: $error"
            }
            pendingUri = null
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }

    override fun onCleared() {
        super.onCleared()
        autoHideTasks.values.forEach { handler.removeCallbacks(it) }
        autoHideTasks.clear()
    }
}
