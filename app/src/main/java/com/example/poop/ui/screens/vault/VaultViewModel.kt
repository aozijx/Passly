package com.example.poop.ui.screens.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.AppContext
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
import com.example.poop.ui.screens.vault.utils.BackupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultTab { ALL, PASSWORDS, TOTP }
enum class AddType { NONE, PASSWORD, TOTP, SCAN }

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()
    private val preference = AppContext.get().preference

    // --- 1. 搜索、分类与 Tab 过滤 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isFilterMenuExpanded by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)

    // --- 2. 自动锁定与安全 ---
    private val lockTimeMs = 60000L
    private var lockJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    var isAuthorized by mutableStateOf(false)
        private set

    // --- 3. 添加流程 (Add) ---
    var addType by mutableStateOf(AddType.NONE)
        private set
    var addDialogTitle by mutableStateOf("")
    var addDialogUsername by mutableStateOf("")
    var addDialogPassword by mutableStateOf("")
    var addDialogCategory by mutableStateOf("")
    var addDialogPasswordVisible by mutableStateOf(false)
    var addDialogTotpSecret by mutableStateOf("")

    // --- 4. 详情与编辑流程 (Detail & Edit) ---
    var detailItem by mutableStateOf<VaultItem?>(null)
        private set
    var isEditingCategory by mutableStateOf(false)
    var editedCategory by mutableStateOf("")
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")

    // --- 5. 删除流程 (Delete) ---
    var itemToDelete by mutableStateOf<VaultItem?>(null)
        private set

    // --- 6. 备份与导出 (Backup) ---
    var isBackupLoading by mutableStateOf(false)
    var backupMessage by mutableStateOf<String?>(null)
    var showBackupPasswordDialog by mutableStateOf(false)
    var isExporting by mutableStateOf(true)
    var pendingUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupManager.ImportMode.OVERWRITE)

    // --- 7. 主题与偏好 (Settings) ---
    val isDarkMode: StateFlow<Boolean?> = preference.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isDynamicColor: StateFlow<Boolean> = preference.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- 8. 响应式数据流 (Flows) ---
    val availableCategories: StateFlow<List<String>> = dao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultItem>> =
        combine(_searchQuery, _selectedCategory, _selectedTab) { query, category, tab ->
            Triple(query, category, tab)
        }.flatMapLatest { (query, category, tab) ->
            val baseFlow = when {
                query.isNotEmpty() -> dao.searchItems(query)
                category != null -> dao.getItemsByCategory(category)
                else -> dao.getAllItems()
            }
            baseFlow.map { items ->
                when (tab) {
                    VaultTab.ALL -> items
                    VaultTab.PASSWORDS -> items.filter { it.totpSecret == null }
                    VaultTab.TOTP -> items.filter { it.totpSecret != null }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 9. 安全逻辑方法 ---
    fun authorize() { isAuthorized = true; updateInteraction() }
    fun lock() { isAuthorized = false; cancelLockTimer() }
    fun startLockTimer() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch { delay(lockTimeMs); if (isAuthorized) lock() }
    }
    fun cancelLockTimer() { lockJob?.cancel() }
    fun updateInteraction() { lastInteractionTime = System.currentTimeMillis(); if (isAuthorized) startLockTimer() }
    fun checkAndLock() { if (isAuthorized && System.currentTimeMillis() - lastInteractionTime >= lockTimeMs) lock() }

    // --- 10. 交互方法 (UI Actions) ---
    fun onSearchQueryChange(newQuery: String) { _searchQuery.value = newQuery }
    fun toggleSearch(active: Boolean) { isSearchActive = active; if (!active) _searchQuery.value = "" }
    fun toggleFilterMenu(expanded: Boolean) { isFilterMenuExpanded = expanded }
    fun toggleMoreMenu(expanded: Boolean) { isMoreMenuExpanded = expanded }
    fun onCategorySelect(category: String?) { _selectedCategory.value = category; isFilterMenuExpanded = false }
    fun onTabSelect(tab: VaultTab) { _selectedTab.value = tab }

    // 添加逻辑
    fun onAddTypeSelect(type: AddType) { resetAddDialogFields(); addType = type }
    fun dismissAddDialog() { addType = AddType.NONE }
    private fun resetAddDialogFields() {
        addDialogTitle = ""; addDialogUsername = ""; addDialogPassword = ""
        addDialogCategory = ""; addDialogTotpSecret = ""; addDialogPasswordVisible = false
    }
    fun addItem(title: String, encryptedUser: String, encryptedPass: String, category: String, totpSecret: String? = null) {
        viewModelScope.launch {
            dao.insert(VaultItem(title = title, username = encryptedUser, password = encryptedPass, category = category.trim(), totpSecret = totpSecret))
            addType = AddType.NONE; resetAddDialogFields()
        }
    }

    // 详情与编辑逻辑
    fun showDetail(item: VaultItem) { detailItem = item; isEditingCategory = false; isEditingUsername = false; isEditingPassword = false; editedCategory = item.category }
    fun dismissDetail() { detailItem = null }
    fun startEditingCategory() { isEditingCategory = true; editedCategory = detailItem?.category ?: "" }
    fun saveCategoryEdit() {
        val item = detailItem ?: return
        val newCategory = editedCategory.trim()
        if (newCategory.isNotEmpty() && newCategory != item.category) {
            viewModelScope.launch { val updatedItem = item.copy(category = newCategory); dao.update(updatedItem); detailItem = updatedItem; isEditingCategory = false }
        } else isEditingCategory = false
    }
    fun startEditingUsername(u: String) { editedUsername = u; isEditingUsername = true }
    fun startEditingPassword(p: String) { editedPassword = p; isEditingPassword = true }

    fun saveUsernameEdit(enc: String) {
        val item = detailItem ?: return
        viewModelScope.launch { val updatedItem = item.copy(username = enc); dao.update(updatedItem); detailItem = updatedItem; isEditingUsername = false }
    }
    fun savePasswordEdit(enc: String) {
        val item = detailItem ?: return
        viewModelScope.launch { val updatedItem = item.copy(password = enc); dao.update(updatedItem); detailItem = updatedItem; isEditingPassword = false }
    }
    fun updateVaultItem(item: VaultItem) { viewModelScope.launch { dao.update(item); if (detailItem?.id == item.id) detailItem = item } }

    // 删除逻辑
    fun requestDelete(item: VaultItem) { dismissDetail(); itemToDelete = item }
    fun dismissDeleteDialog() { itemToDelete = null }
    fun confirmDelete() { itemToDelete?.let { viewModelScope.launch { dao.delete(it); itemToDelete = null } } }

    // --- 11. 备份处理逻辑 ---
    fun startExport(uri: Uri) { pendingUri = uri; isExporting = true; backupPassword = ""; showBackupPasswordDialog = true }
    fun startImport(uri: Uri) { pendingUri = uri; isExporting = false; backupPassword = ""; importMode = BackupManager.ImportMode.OVERWRITE; showBackupPasswordDialog = true }
    fun dismissBackupPasswordDialog() { showBackupPasswordDialog = false; pendingUri = null }
    fun processBackupAction(context: Context) {
        val uri = pendingUri ?: return
        val pwd = backupPassword.toCharArray()
        showBackupPasswordDialog = false
        viewModelScope.launch {
            isBackupLoading = true
            val res = if (isExporting) BackupManager.exportBackup(context, uri, pwd) else BackupManager.importBackup(context, uri, pwd, importMode)
            isBackupLoading = false
            backupMessage = if (res.isSuccess) (if (isExporting) "数据备份导出成功" else "数据导入成功") 
            else "${if (isExporting) "导出" else "导入"}失败: ${res.exceptionOrNull()?.message ?: "未知错误"}"
            pendingUri = null
        }
    }
    fun clearBackupMessage() { backupMessage = null }

    override fun onCleared() { super.onCleared(); cancelLockTimer() }
}
