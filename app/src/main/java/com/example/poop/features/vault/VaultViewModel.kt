package com.example.poop.features.vault

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.AppContext
import com.example.poop.R
import com.example.poop.core.common.AddType
import com.example.poop.core.common.VaultTab
import com.example.poop.core.crypto.CryptoManager
import com.example.poop.core.designsystem.state.TotpState
import com.example.poop.core.util.TwoFAUtils
import com.example.poop.core.util.VaultFileUtils
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.model.VaultEntry
import com.example.poop.data.repository.VaultRepository
import com.example.poop.util.Logcat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 保险箱主界面业务逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        VaultRepository(database.vaultDao(), AppContext.get().preference)
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedTab = MutableStateFlow(VaultTab.ALL)
    val selectedTab: StateFlow<VaultTab> = _selectedTab

    var isSearchActive by mutableStateOf(false)
    var isMoreMenuExpanded by mutableStateOf(false)
    var showTOTPCode by mutableStateOf(true)

    // 自动填充状态
    var isAutofillEnabled by mutableStateOf(false)
        private set

    private val _totpStates = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val totpStates: StateFlow<Map<Int, TotpState>> = _totpStates

    var addType by mutableStateOf(AddType.NONE)
    var detailItem by mutableStateOf<VaultEntry?>(null)
    var itemToDelete by mutableStateOf<VaultEntry?>(null)
    var showIconPicker by mutableStateOf(false)

    val availableCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultEntry>> =
        combine(_searchQuery, _selectedCategory) { query, category ->
            Pair(query, category)
        }.flatMapLatest { (query, category) ->
            val baseFlow = when {
                query.isNotEmpty() -> repository.searchEntries(query)
                category != null -> repository.getEntriesByCategory(category)
                else -> repository.allEntries
            }
            baseFlow
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startTotpRefresher()
        updateAutofillStatus()
    }

    /**
     * 更新自动填充服务启用状态
     * 采用双重检查：系统设置字符串匹配 + 官方 API 状态
     */
    fun updateAutofillStatus() {
        val context = getApplication<Application>()
        val afm = context.getSystemService(AutofillManager::class.java)
        
        // 1. 直接检查系统设置（最快最准）：当前选中的服务包名是否包含我们
        val currentService = Settings.Secure.getString(context.contentResolver, "autofill_service")
        val isOurServiceSelected = currentService != null && currentService.contains(context.packageName)
        
        // 2. 官方 API 检查（用于兼容不同系统版本）
        val isEnabledByApi = afm != null && afm.isEnabled && afm.hasEnabledAutofillServices()
        
        // 只要有一项成立，就认为已开启，从而隐藏“去开启”按钮
        isAutofillEnabled = isOurServiceSelected || isEnabledByApi
    }

    /**
     * 跳转至自动填充设置
     */
    fun openAutofillSettings(context: Context) {
        isMoreMenuExpanded = false
        
        fun tryStartActivity(intent: Intent): Boolean {
            return try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Logcat.e("VaultViewModel", "Failed to start activity: ${intent.action}", e)
                false
            }
        }

        // 优先级 1：标准的自动填充请求
        val standardIntent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
            data = "package:${context.packageName}".toUri()
        }

        var started = tryStartActivity(standardIntent)
        
        if (!started) {
            // 优先级 2：进入“默认应用”设置 (适配鸿蒙/国产机)
            started = tryStartActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        
        if (!started) {
            // 优先级 3：终极兜底跳转总设置
            tryStartActivity(Intent(Settings.ACTION_SETTINGS))
            Toast.makeText(
                context, 
                context.getString(R.string.vault_toast_enable_autofill_manual), 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startTotpRefresher() {
        viewModelScope.launch {
            while (true) {
                val currentMap = _totpStates.value
                if (currentMap.isNotEmpty()) {
                    val entries = vaultItems.value
                    _totpStates.update { current ->
                        current.mapValues { (id, state) ->
                            val entry = entries.find { it.id == id } ?: return@mapValues state
                            val secret = state.decryptedSecret ?: return@mapValues state
                            val period = entry.totpPeriod.coerceAtLeast(1)
                            val remaining = period - ((System.currentTimeMillis() / 1000) % period)
                            val code = TwoFAUtils.generateTotp(secret, if (entry.totpAlgorithm == "STEAM") 5 else entry.totpDigits, entry.totpPeriod, entry.totpAlgorithm)
                            state.copy(code = code, progress = remaining.toFloat() / period)
                        }
                    }
                }
                delay(500)
            }
        }
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) { onResult(""); return }
        authenticate(activity, "查看详情", "", null) {
            try { onResult(CryptoManager.decrypt(encryptedData)) } catch (e: Exception) { onResult(null) }
        }
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (List<String?>) -> Unit
    ) {
        if (encryptedList.isEmpty()) { onResult(emptyList()); return }
        authenticate(activity, "查看详情", "", null) {
            try {
                val results = encryptedList.map { if (it.isEmpty()) "" else CryptoManager.decrypt(it) }
                onResult(results)
            } catch (e: Exception) {
                onResult(encryptedList.map { null })
            }
        }
    }

    fun autoUnlockTotp(entry: VaultEntry) {
        if (_totpStates.value.containsKey(entry.id)) return
        val encrypted = entry.totpSecret ?: return
        try {
            val decrypted = CryptoManager.decrypt(encrypted)
            unlockTotp(entry, decrypted)
        } catch (e: Exception) {
            Logcat.e("VaultViewModel", "Auto unlock failed", e)
        }
    }

    fun showDetail(entry: VaultEntry) { detailItem = entry }
    fun dismissDetail() { detailItem = null }
    fun addItem(entry: VaultEntry) { viewModelScope.launch { repository.insert(entry); addType = AddType.NONE } }
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            repository.update(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            showIconPicker = false
            _totpStates.update { it - entry.id }
        }
    }
    fun confirmDelete() {
        itemToDelete?.let { entry ->
            viewModelScope.launch {
                // 如果当前正在查看的条目正是被删除的条目，先关闭详情弹窗
                if (detailItem?.id == entry.id) {
                    detailItem = null
                }
                entry.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
                repository.delete(entry)
                itemToDelete = null
                _totpStates.update { it - entry.id }
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            // 同样处理快速删除时的详情弹窗状态
            if (detailItem?.id == entry.id) {
                detailItem = null
            }
            entry.iconCustomPath?.let { VaultFileUtils.deleteImage(it) }
            repository.delete(entry)
            _totpStates.update { it - entry.id }
        }
    }

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun toggleSearch(active: Boolean) { isSearchActive = active; if (!active) _searchQuery.value = "" }
    fun unlockTotp(entry: VaultEntry, decryptedSecret: String) {
        _totpStates.update { it + (entry.id to TotpState("------", 1f, decryptedSecret)) }
    }
}
