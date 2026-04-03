package com.aozijx.passly.features.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.R
import com.aozijx.passly.core.common.AddType
import com.aozijx.passly.core.common.VaultTab
import com.aozijx.passly.core.designsystem.state.TotpState
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.FaviconResult
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.internal.VaultAutofillSupport
import com.aozijx.passly.features.vault.internal.VaultCryptoSupport
import com.aozijx.passly.features.vault.internal.VaultEntryFileSupport
import com.aozijx.passly.features.vault.internal.VaultTotpSupport
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 保险箱主界面业务逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vaultUseCases = AppContainer.vaultUseCases
    private val autofillSupport = VaultAutofillSupport()
    private val cryptoSupport = VaultCryptoSupport()
    private val totpSupport = VaultTotpSupport()
    private val entryFileSupport = VaultEntryFileSupport()

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

    val availableCategories: StateFlow<List<String>> = vaultUseCases.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultSummary>> =
        combine(_searchQuery, _selectedCategory) { query, category ->
            Pair(query, category)
        }.flatMapLatest { (query, category) ->
            val baseFlow = when {
                query.isNotEmpty() -> vaultUseCases.searchEntries(query)
                category != null -> vaultUseCases.getEntriesByCategory(category)
                else -> vaultUseCases.observeAllEntries()
            }
            baseFlow
        }.map { entries -> entries.map { it.toSummary() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        isAutofillEnabled = autofillSupport.isAutofillEnabled(context)
    }

    /**
     * 跳转至自动填充设置
     */
    fun openAutofillSettings(context: Context) {
        isMoreMenuExpanded = false
        val started = autofillSupport.openAutofillSettings(context)
        if (!started) {
            Toast.makeText(
                context, 
                context.getString(R.string.vault_toast_enable_autofill_manual), 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startTotpRefresher() {
        viewModelScope.launch {
            totpSupport.runRefresher(
                statesProvider = { _totpStates.value },
                entriesProvider = { vaultItems.value },
                updateStates = { refreshed -> _totpStates.value = refreshed },
                codeGenerator = { config -> vaultUseCases.getTotpCode(config) }
            )
        }
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        cryptoSupport.decryptSingle(activity, encryptedData, authenticate, onResult)
    }

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (List<String?>) -> Unit
    ) {
        cryptoSupport.decryptMultiple(activity, encryptedList, authenticate, onResult)
    }

    fun autoUnlockTotp(entry: VaultEntry) {
        autoUnlockTotp(entry.toSummary())
    }

    fun autoUnlockTotp(entry: VaultSummary) {
        if (_totpStates.value.containsKey(entry.id)) return
        val decrypted = cryptoSupport.decryptTotpSecret(entry.totpSecret)
        if (decrypted == null) {
            Logcat.w("VaultViewModel", "Auto unlock failed: secret decrypt returned null")
            return
        }
        unlockTotp(entry.id, decrypted)
    }

    fun showDetail(entry: VaultEntry) { detailItem = entry }
    fun showDetail(entry: VaultSummary) {
        loadEntryById(entry.id) { detailItem = it }
    }
    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch {
            vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) }
        }
    }
    fun dismissDetail() { detailItem = null }
    fun addItem(entry: VaultEntry) { viewModelScope.launch { vaultUseCases.insertEntry(entry); addType = AddType.NONE } }
    
    /**
     * 添加条目并自动下载 favicon（如果提供了域名）
     */
    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            val insertedId = vaultUseCases.insertEntry(entry)
            addType = AddType.NONE
            
            if (domain.isNotBlank()) {
                val outcome = vaultUseCases.downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    val updatedEntry = entry.copy(
                        id = insertedId.toInt(),
                        iconName = null,
                        iconCustomPath = outcome.filePath
                    )
                    vaultUseCases.updateEntry(updatedEntry)
                }
            }
        }
    }
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            vaultUseCases.updateEntry(entry)
            if (detailItem?.id == entry.id) detailItem = entry
            showIconPicker = false
            _totpStates.update { it - entry.id }
            if (!entry.totpSecret.isNullOrBlank()) {
                autoUnlockTotp(entry)
            }
        }
    }

    /**
     * 处理自定义图片的保存逻辑
     */
    fun saveCustomIcon(item: VaultEntry, uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val internalPath = entryFileSupport.saveCustomIcon(context, item, uri)
            
            if (internalPath != null) {
                updateVaultEntry(
                    item.copy(
                        iconName = null,
                        iconCustomPath = internalPath
                    )
                )
            } else {
                Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun confirmDelete() {
        itemToDelete?.let { entry ->
            viewModelScope.launch {
                // 如果当前正在查看的条目正是被删除的条目，先关闭详情弹窗
                if (detailItem?.id == entry.id) {
                    detailItem = null
                }
                entryFileSupport.cleanupIcon(entry.iconCustomPath)
                vaultUseCases.deleteEntry(entry)
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
            entryFileSupport.cleanupIcon(entry.iconCustomPath)
            vaultUseCases.deleteEntry(entry)
            _totpStates.update { it - entry.id }
        }
    }

    fun quickDelete(entry: VaultSummary) {
        loadEntryById(entry.id) { quickDelete(it) }
    }

    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun selectTab(tab: VaultTab) { _selectedTab.value = tab }
    fun toggleSearch(active: Boolean) { isSearchActive = active; if (!active) _searchQuery.value = "" }
    fun unlockTotp(entryId: Int, decryptedSecret: String) {
        _totpStates.update { current ->
            totpSupport.unlock(current, entryId, decryptedSecret)
        }
    }
}


