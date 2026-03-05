package com.example.poop.ui.screens.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
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
enum class AddType { 
    NONE, PASSWORD, TOTP, SCAN, WIFI, 
    CREDIT_CARD, CRYPTO_SEED, IDENTITY_DOC, SSH_KEY 
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()
    private val preference = com.example.poop.AppContext.get().preference

    // --- 1. 状态管理 (UI States) ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isFilterMenuExpanded by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)

    // 安全锁定
    private val lockTimeMs = 60000L
    private var lockJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    var isAuthorized by mutableStateOf(false)
        private set

    // 添加表单状态
    var addType by mutableStateOf(AddType.NONE)
        private set
    var addDialogTitle by mutableStateOf("")
    var addDialogUsername by mutableStateOf("")
    var addDialogPassword by mutableStateOf("")
    var addDialogCategory by mutableStateOf("")
    var addDialogNotes by mutableStateOf("")
    var addDialogPasswordVisible by mutableStateOf(false)
    
    // TOTP 相关
    var addDialogTotpSecret by mutableStateOf("")
    var addDialogTotpPeriod by mutableStateOf("30")
    var addDialogTotpDigits by mutableStateOf("6")
    var addDialogTotpAlgorithm by mutableStateOf("SHA1")

    var addDialogCardCvv by mutableStateOf("")
    var addDialogCardExpiration by mutableStateOf("")
    var addDialogIdNumber by mutableStateOf("")
    var addDialogPaymentPin by mutableStateOf("")
    var addDialogSshPrivateKey by mutableStateOf("")
    var addDialogCryptoSeedPhrase by mutableStateOf("")
    var addDialogRecoveryCodes by mutableStateOf("")
    var addDialogWifiEncryption by mutableStateOf("WPA")
    var addDialogWifiIsHidden by mutableStateOf(false)

    // 详情与编辑状态
    var detailItem by mutableStateOf<VaultEntry?>(null)
        private set
    var isEditingCategory by mutableStateOf(false)
    var editedCategory by mutableStateOf("")
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")

    // 删除与备份
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
        private set
    var isBackupLoading by mutableStateOf(false)
    var backupMessage by mutableStateOf<String?>(null)
    var showBackupPasswordDialog by mutableStateOf(false)
    var isExporting by mutableStateOf(true)
    var pendingUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupManager.ImportMode.OVERWRITE)

    // --- 2. 偏好与数据流 ---
    val isDarkMode: StateFlow<Boolean?> = preference.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isDynamicColor: StateFlow<Boolean> = preference.isDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val availableCategories: StateFlow<List<String>> = dao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultEntry>> =
        combine(_searchQuery, _selectedCategory, _selectedTab) { query, category, tab ->
            Triple(query, category, tab)
        }.flatMapLatest { (query, category, tab) ->
            val baseFlow = when {
                query.isNotEmpty() -> dao.searchEntries(query)
                category != null -> dao.getEntriesByCategory(category)
                else -> dao.getAllEntries()
            }
            baseFlow.map { entries ->
                when (tab) {
                    VaultTab.ALL -> entries
                    VaultTab.PASSWORDS -> entries.filter { it.totpSecret == null }
                    VaultTab.TOTP -> entries.filter { it.totpSecret != null }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 3. 核心业务方法 ---
    fun authorize() { isAuthorized = true; updateInteraction() }
    fun lock() { isAuthorized = false; cancelLockTimer() }
    fun startLockTimer() {
        lockJob?.cancel()
        lockJob = viewModelScope.launch { delay(lockTimeMs); if (isAuthorized) lock() }
    }
    fun cancelLockTimer() { lockJob?.cancel() }
    fun updateInteraction() { lastInteractionTime = System.currentTimeMillis(); if (isAuthorized) startLockTimer() }
    fun checkAndLock() { if (isAuthorized && System.currentTimeMillis() - lastInteractionTime >= lockTimeMs) lock() }

    // 搜索与过滤
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
        addDialogCategory = ""; addDialogNotes = ""; addDialogTotpSecret = ""
        addDialogTotpPeriod = "30"; addDialogTotpDigits = "6"; addDialogTotpAlgorithm = "SHA1"
        addDialogCardCvv = ""; addDialogCardExpiration = ""; addDialogIdNumber = ""
        addDialogPaymentPin = ""; addDialogSshPrivateKey = ""; addDialogCryptoSeedPhrase = ""
        addDialogRecoveryCodes = ""; addDialogWifiEncryption = "WPA"; addDialogWifiIsHidden = false
        addDialogPasswordVisible = false
    }

    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            dao.insert(entry)
            addType = AddType.NONE
            resetAddDialogFields()
        }
    }

    /**
     * 为兼容旧版调用的 addItem 重载
     */
    fun addItem(
        title: String,
        encryptedUser: String,
        encryptedPass: String,
        category: String,
        totpSecret: String? = null
    ) {
        val entry = VaultEntry(
            title = title,
            username = encryptedUser,
            password = encryptedPass,
            category = category,
            totpSecret = totpSecret,
            entryType = if (totpSecret != null) 1 else 0
        )
        addItem(entry)
    }

    // 详情与编辑
    fun showDetail(entry: VaultEntry) { 
        detailItem = entry
        isEditingCategory = false; isEditingUsername = false; isEditingPassword = false
        editedCategory = entry.category 
    }
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

    fun saveUsernameEdit(newEncryptedUsername: String) {
        val item = detailItem ?: return
        viewModelScope.launch {
            val updatedItem = item.copy(username = newEncryptedUsername)
            dao.update(updatedItem)
            detailItem = updatedItem
            isEditingUsername = false
        }
    }

    fun savePasswordEdit(newEncryptedPassword: String) {
        val item = detailItem ?: return
        viewModelScope.launch {
            val updatedItem = item.copy(password = newEncryptedPassword)
            dao.update(updatedItem)
            detailItem = updatedItem
            isEditingPassword = false
        }
    }

    /**
     * 统一更新入口：更新数据并重置所有编辑状态
     */
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            dao.update(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            isEditingCategory = false
            isEditingUsername = false
            isEditingPassword = false
        }
    }

    // 删除逻辑
    fun requestDelete(entry: VaultEntry) { dismissDetail(); itemToDelete = entry }
    fun dismissDeleteDialog() { itemToDelete = null }
    fun confirmDelete() { itemToDelete?.let { viewModelScope.launch { dao.delete(it); itemToDelete = null } } }

    // 备份处理
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
            backupMessage = if (res.isSuccess) (if (isExporting) "数据备份成功" else "导入成功") 
            else "${if (isExporting) "导出" else "导入"}失败: ${res.exceptionOrNull()?.message}"
            pendingUri = null
        }
    }
    fun clearBackupMessage() { backupMessage = null }

    override fun onCleared() { super.onCleared(); cancelLockTimer() }
}
