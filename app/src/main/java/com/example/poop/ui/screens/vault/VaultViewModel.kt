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

    // --- 状态管理 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isFilterMenuExpanded by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)

    // TOTP 显示控制
    var showTOTPCode by mutableStateOf(true)

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
    var addDialogIconName by mutableStateOf<String?>(null)
    var addDialogIconPath by mutableStateOf<String?>(null)
    
    // TOTP 状态
    var addDialogTotpSecret by mutableStateOf("")
    var addDialogTotpPeriod by mutableStateOf("30")
    var addDialogTotpDigits by mutableStateOf("6")
    var addDialogTotpAlgorithm by mutableStateOf("SHA1")

    // 详情与编辑
    var detailItem by mutableStateOf<VaultEntry?>(null)
        private set
    var isEditingCategory by mutableStateOf(false)
    var editedCategory by mutableStateOf("")
    var isEditingUsername by mutableStateOf(false)
    var isEditingPassword by mutableStateOf(false)
    var editedUsername by mutableStateOf("")
    var editedPassword by mutableStateOf("")

    // 新增：域名和包名编辑状态
    var isEditingDomain by mutableStateOf(false)
    var editedDomain by mutableStateOf("")
    var isEditingPackage by mutableStateOf(false)
    var editedPackage by mutableStateOf("")

    // TOTP 编辑状态
    var isEditingTotpConfig by mutableStateOf(false)
    var editedTotpSecret by mutableStateOf("")
    var editedTotpPeriod by mutableStateOf("")
    var editedTotpDigits by mutableStateOf("")
    var editedTotpAlgorithm by mutableStateOf("")

    // 图标选择状态
    var showIconPicker by mutableStateOf(false)

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

    // --- 方法 ---
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
        addDialogPasswordVisible = false
        addDialogIconName = null; addDialogIconPath = null; showIconPicker = false
    }

    fun addItem(entry: VaultEntry) {
        viewModelScope.launch { dao.insert(entry); addType = AddType.NONE; resetAddDialogFields() }
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

    fun showDetail(entry: VaultEntry) { 
        detailItem = entry
        isEditingCategory = false; isEditingUsername = false; isEditingPassword = false
        isEditingDomain = false; isEditingPackage = false
        isEditingTotpConfig = false; showIconPicker = false
    }
    fun dismissDetail() { detailItem = null }

    fun onIconSelected(name: String?, path: String? = null) {
        detailItem?.let { item ->
            updateVaultEntry(item.copy(iconName = name, iconCustomPath = path))
        } ?: run {
            addDialogIconName = name
            addDialogIconPath = path
            showIconPicker = false
        }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            dao.update(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            showIconPicker = false
        }
    }

    // TOTP 配置相关
    fun startEditingTotp(secret: String) {
        editedTotpSecret = secret
        detailItem?.let {
            editedTotpPeriod = it.totpPeriod.toString()
            editedTotpDigits = it.totpDigits.toString()
            editedTotpAlgorithm = it.totpAlgorithm
        }
        isEditingTotpConfig = true
    }

    fun saveTotpEdit(newEncryptedSecret: String) {
        detailItem?.let { item ->
            updateVaultEntry(item.copy(
                totpSecret = newEncryptedSecret,
                totpPeriod = editedTotpPeriod.toIntOrNull() ?: 30,
                totpDigits = editedTotpDigits.toIntOrNull() ?: 6,
                totpAlgorithm = editedTotpAlgorithm
            ))
            isEditingTotpConfig = false
        }
    }

    fun detectSteam(text: String, isEditing: Boolean) {
        if (text.contains("Steam", ignoreCase = true)) {
            if (isEditing) {
                editedTotpAlgorithm = "STEAM"; editedTotpDigits = "5"
            } else {
                addDialogTotpAlgorithm = "STEAM"; addDialogTotpDigits = "5"
            }
        }
    }

    fun startEditingCategory() { isEditingCategory = true; editedCategory = detailItem?.category ?: "" }
    fun saveCategoryEdit() {
        val item = detailItem ?: return
        val newCategory = editedCategory.trim()
        if (newCategory.isNotEmpty() && newCategory != item.category) {
            updateVaultEntry(item.copy(category = newCategory))
        } else isEditingCategory = false
    }
    fun startEditingUsername(u: String) { editedUsername = u; isEditingUsername = true }
    fun startEditingPassword(p: String) { editedPassword = p; isEditingPassword = true }
    fun saveUsernameEdit(enc: String) { detailItem?.let { updateVaultEntry(it.copy(username = enc)); isEditingUsername = false } }
    fun savePasswordEdit(enc: String) { detailItem?.let { updateVaultEntry(it.copy(password = enc)); isEditingPassword = false } }

    // 新增：域名和包名编辑保存方法
    fun startEditingDomain(d: String) { editedDomain = d; isEditingDomain = true }
    fun saveDomainEdit(newDomain: String) { detailItem?.let { updateVaultEntry(it.copy(associatedDomain = newDomain)); isEditingDomain = false } }
    fun startEditingPackage(p: String) { editedPackage = p; isEditingPackage = true }
    fun savePackageEdit(newPackage: String) { detailItem?.let { updateVaultEntry(it.copy(associatedAppPackage = newPackage)); isEditingPackage = false } }

    fun requestDelete(entry: VaultEntry) { dismissDetail(); itemToDelete = entry }
    fun dismissDeleteDialog() { itemToDelete = null }
    fun confirmDelete() { itemToDelete?.let { viewModelScope.launch { dao.delete(it); itemToDelete = null } } }

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
    fun startExport(uri: Uri) { pendingUri = uri; isExporting = true; showBackupPasswordDialog = true }
    fun startImport(uri: Uri) { pendingUri = uri; isExporting = false; showBackupPasswordDialog = true }
    fun dismissBackupPasswordDialog() { showBackupPasswordDialog = false; pendingUri = null }
}
