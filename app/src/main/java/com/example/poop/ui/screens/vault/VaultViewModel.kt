package com.example.poop.ui.screens.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.R
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.core.AddType
import com.example.poop.ui.screens.vault.core.VaultTab
import com.example.poop.ui.screens.vault.types.totp.TotpState
import com.example.poop.ui.screens.vault.utils.BackupManager
import com.example.poop.ui.screens.vault.utils.BiometricHelper
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import com.example.poop.util.Logcat
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 全局控制中心 - 统一管理 TOTP 的解密、刷新和进度计算逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).vaultDao()
    private val preference = com.example.poop.AppContext.get().preference

    // --- 全局 UI 状态 ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)
    var showTOTPCode by mutableStateOf(true)

    // --- 全局 TOTP 状态管理 ---
    private val _totpStates = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val totpStates: StateFlow<Map<Int, TotpState>> = _totpStates

    // --- 安全锁定逻辑 ---
    private val lockTimeMs = 60000L
    private var lockJob: Job? = null
    private var lastInteractionTime = System.currentTimeMillis()
    var isAuthorized by mutableStateOf(false)
        private set

    // --- 当前正在交互的对象（路由核心） ---
    var addType by mutableStateOf(AddType.NONE)
    var detailItem by mutableStateOf<VaultEntry?>(null)
    var itemToDelete by mutableStateOf<VaultEntry?>(null)

    // 图标选择共享状态
    var showIconPicker by mutableStateOf(false)

    // --- 偏好与数据流 ---
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

    init {
        // 1. 启动全局 TOTP 刷新器
        startTotpRefresher()
        
        // 2. 自动尝试静默解密所有 TOTP
        observeAndSilentUnlock()
    }

    private fun startTotpRefresher() {
        viewModelScope.launch {
            while (true) {
                val currentMap = _totpStates.value
                if (currentMap.isNotEmpty()) {
                    val entries = vaultItems.value
                    val updatedMap = currentMap.mapValues { (id, state) ->
                        val entry = entries.find { it.id == id } ?: return@mapValues state
                        val secret = state.decryptedSecret ?: return@mapValues state
                        
                        val period = entry.totpPeriod.coerceAtLeast(1)
                        val currentTime = System.currentTimeMillis() / 1000
                        val remaining = period - (currentTime % period)
                        val progress = remaining.toFloat() / period
                        
                        val isSteam = entry.totpAlgorithm.uppercase() == "STEAM"
                        val code = TwoFAUtils.generateTotp(
                            secret = secret,
                            digits = if (isSteam) 5 else entry.totpDigits,
                            period = entry.totpPeriod,
                            algorithm = entry.totpAlgorithm
                        )
                        state.copy(code = code, progress = progress)
                    }
                    _totpStates.value = updatedMap
                }
                delay(500)
            }
        }
    }

    private fun observeAndSilentUnlock() {
        viewModelScope.launch {
            vaultItems.collect { entries ->
                val currentStates = _totpStates.value
                entries.forEach { entry ->
                    if (entry.totpSecret != null && !currentStates.containsKey(entry.id)) {
                        trySilentUnlock(entry)
                    }
                }
            }
        }
    }

    private fun trySilentUnlock(entry: VaultEntry) {
        val encrypted = entry.totpSecret ?: return
        try {
            val iv = CryptoManager.getIvFromCipherText(encrypted) ?: return
            val cipher = CryptoManager.getDecryptCipher(iv, isSilent = true)
            val decrypted = cipher?.let { CryptoManager.decrypt(encrypted, it) }
            
            if (decrypted != null) {
                Logcat.i("VaultViewModel", "Entry ${entry.id} unlocked silently")
                unlockTotp(entry, decrypted, isManual = false)
            }
        } catch (_: Exception) {
            // 静默失败很正常，可能是因为密钥是 Secure 类型的
        }
    }

    /**
     * 确保 TOTP 已解锁。由 UI 组件在显示前调用。
     * 支持 PIN/密码解锁：不直接传递 CryptoObject，而是先验证用户身份。
     */
    fun ensureTotpUnlocked(activity: FragmentActivity, entry: VaultEntry) {
        if (_totpStates.value.containsKey(entry.id)) return

        val encrypted = entry.totpSecret ?: return
        val iv = CryptoManager.getIvFromCipherText(encrypted) ?: return

        // 1. 再次尝试静默解锁 (免验证密钥)
        try {
            val silentCipher = CryptoManager.getDecryptCipher(iv, isSilent = true)
            val decrypted = silentCipher?.let { CryptoManager.decrypt(encrypted, it) }
            if (decrypted != null) {
                Logcat.i("VaultViewModel", "Entry ${entry.id} unlocked silently on demand")
                unlockTotp(entry, decrypted, isManual = false)
                return
            }
        } catch (_: Exception) {}

        // 2. 静默失败，提示用户验证 (支持 PIN/密码)
        Logcat.i("VaultViewModel", "Silent unlock failed for ${entry.id}, requesting auth (PIN supported)")
        authenticate(activity, activity.getString(R.string.vault_auth_decrypt_title), entry.title) {
            // 验证成功后，10秒内可以直接获取解密 Cipher，无需传入 CryptoObject
            val cipher = CryptoManager.getDecryptCipher(iv, isSilent = false)
            val decrypted = cipher?.let { CryptoManager.decrypt(encrypted, it) }
            if (decrypted != null) {
                Logcat.i("VaultViewModel", "Entry ${entry.id} unlocked with auth (PIN/Biometric)")
                unlockTotp(entry, decrypted, isManual = true)
            }
        }
    }

    fun unlockTotp(entry: VaultEntry, decryptedSecret: String, isManual: Boolean = false) {
        val period = entry.totpPeriod.coerceAtLeast(1)
        val remaining = period - ((System.currentTimeMillis() / 1000) % period)
        
        _totpStates.update { it + (entry.id to TotpState(
            code = "------",
            progress = remaining.toFloat() / period,
            decryptedSecret = decryptedSecret
        )) }

        if (isManual) {
            viewModelScope.launch {
                try {
                    val newCipher = CryptoManager.getEncryptCipher(isSilent = true)
                    if (newCipher != null) {
                        val reEncrypted = CryptoManager.encrypt(decryptedSecret, newCipher)
                        val updated = entry.copy(totpSecret = reEncrypted)
                        dao.update(updated)
                        if (detailItem?.id == entry.id) detailItem = updated
                        Logcat.i("VaultViewModel", "Entry ${entry.id} migrated to silent key")
                    }
                } catch (e: Exception) {
                    Logcat.e("VaultViewModel", "Migration failed for ${entry.id}", e)
                }
            }
        }
    }

    // --- 核心操作：加解密验证 ---

    fun authenticate(activity: FragmentActivity, title: String, subtitle: String = "", onError: ((String) -> Unit)? = null, onSuccess: () -> Unit) {
        // 这里不传 CryptoObject，BiometricPrompt 就会显示 "Use PIN/Password" 按钮
        BiometricHelper.authenticate(activity, title, subtitle, 
            onSuccess = { updateInteraction(); onSuccess() }, onError = onError)
    }

    fun encryptMultiple(
        activity: FragmentActivity, 
        texts: List<String>, 
        title: String = getApplication<Application>().getString(R.string.vault_auth_encrypt_title), 
        subtitle: String = getApplication<Application>().getString(R.string.vault_auth_encrypt_subtitle), 
        onSuccess: (List<String>) -> Unit
    ) {
        authenticate(activity, title, subtitle) {
            onSuccess(texts.map { CryptoManager.encrypt(it, isSilent = false) ?: "" })
        }
    }

    fun decryptSingle(
        activity: FragmentActivity, 
        text: String, 
        title: String = getApplication<Application>().getString(R.string.vault_auth_decrypt_title), 
        subtitle: String = getApplication<Application>().getString(R.string.vault_auth_decrypt_subtitle_reveal), 
        onSuccess: (String?) -> Unit
    ) {
        authenticate(activity, title, subtitle) {
            val iv = CryptoManager.getIvFromCipherText(text)
            val cipher = iv?.let { CryptoManager.getDecryptCipher(it, isSilent = false) }
            onSuccess(cipher?.let { CryptoManager.decrypt(text, it) })
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity, 
        texts: List<String>, 
        title: String = getApplication<Application>().getString(R.string.vault_auth_decrypt_title), 
        subtitle: String = getApplication<Application>().getString(R.string.vault_auth_decrypt_subtitle_generic), 
        onSuccess: (List<String?>) -> Unit
    ) {
        if (texts.isEmpty()) return onSuccess(emptyList())
        authenticate(activity, title, subtitle) {
            onSuccess(texts.map { text ->
                val iv = CryptoManager.getIvFromCipherText(text)
                val cipher = iv?.let { CryptoManager.getDecryptCipher(it, isSilent = false) }
                cipher?.let { CryptoManager.decrypt(text, it) }
            })
        }
    }

    // --- 核心操作：数据库 CRUD ---
    fun addItem(entry: VaultEntry) {
        viewModelScope.launch { dao.insert(entry); addType = AddType.NONE }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            dao.update(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            showIconPicker = false
            _totpStates.update { it - entry.id }
        }
    }

    fun requestDelete(entry: VaultEntry) {
        dismissDetail()
        itemToDelete = entry
    }

    fun dismissDeleteDialog() {
        itemToDelete = null
    }

    fun confirmDelete() {
        itemToDelete?.let { entry ->
            viewModelScope.launch {
                dao.delete(entry)
                itemToDelete = null
                _totpStates.update { it - entry.id }
            }
        }
    }

    // --- 生命周期管理 ---
    fun authorize() { isAuthorized = true; updateInteraction() }
    fun lock() { isAuthorized = false; lockJob?.cancel() }
    fun updateInteraction() { 
        lastInteractionTime = System.currentTimeMillis()
        lockJob?.cancel()
        lockJob = viewModelScope.launch { delay(lockTimeMs); if (isAuthorized) lock() }
    }
    fun checkAndLock() { if (isAuthorized && System.currentTimeMillis() - lastInteractionTime >= lockTimeMs) lock() }

    // --- 其他 UI 事件处理 ---
    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun toggleSearch(active: Boolean) { isSearchActive = active; if (!active) _searchQuery.value = "" }
    fun onCategorySelect(c: String?) { _selectedCategory.value = c }
    fun onTabSelect(t: VaultTab) { _selectedTab.value = t }
    fun onAddTypeSelect(t: AddType) { addType = t }
    fun dismissAddDialog() { addType = AddType.NONE }
    fun showDetail(entry: VaultEntry) { detailItem = entry }
    fun dismissDetail() { detailItem = null }
    fun onIconSelected(name: String?, path: String? = null) {
        detailItem?.let { updateVaultEntry(it.copy(iconName = name, iconCustomPath = path)) }
    }

    // --- 备份逻辑 ---
    var isBackupLoading by mutableStateOf(false)
    var backupMessage by mutableStateOf<String?>(null)
    var showBackupPasswordDialog by mutableStateOf(false)
    var isExporting by mutableStateOf(true)
    var pendingUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")

    fun processBackupAction(context: Context) {
        val uri = pendingUri ?: return
        viewModelScope.launch {
            isBackupLoading = true
            val res = if (isExporting) BackupManager.exportBackup(context, uri, backupPassword.toCharArray()) 
                      else BackupManager.importBackup(context, uri, backupPassword.toCharArray(), BackupManager.ImportMode.OVERWRITE)
            
            val successMsg = context.getString(R.string.vault_backup_success)
            val failedFormat = context.getString(R.string.vault_backup_failed)
            
            backupMessage = if (res.isSuccess) successMsg else failedFormat.format(res.exceptionOrNull()?.message)
            isBackupLoading = false
            showBackupPasswordDialog = false
            pendingUri = null
            backupPassword = ""
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }

    fun startExport(uri: Uri) {
        pendingUri = uri
        isExporting = true
        showBackupPasswordDialog = true
    }

    fun startImport(uri: Uri) {
        pendingUri = uri
        isExporting = false
        showBackupPasswordDialog = true
    }

    fun dismissBackupPasswordDialog() {
        showBackupPasswordDialog = false
        pendingUri = null
        backupPassword = ""
    }
}
