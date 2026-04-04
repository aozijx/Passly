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
import com.aozijx.passly.core.common.ui.AddType
import com.aozijx.passly.core.common.ui.VaultTab
import com.aozijx.passly.core.designsystem.state.TotpState
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultSummary
import com.aozijx.passly.features.vault.internal.VaultAutofillSupport
import com.aozijx.passly.features.vault.internal.VaultCryptoSupport
import com.aozijx.passly.features.vault.internal.VaultDetailStateHolder
import com.aozijx.passly.features.vault.internal.VaultEntryFileSupport
import com.aozijx.passly.features.vault.internal.VaultEntryLifecycleSupport
import com.aozijx.passly.features.vault.internal.VaultSearchFilterStateHolder
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
    private val entryLifecycleSupport = VaultEntryLifecycleSupport(vaultUseCases, entryFileSupport)

    private val searchFilterState = VaultSearchFilterStateHolder()
    private val detailState = VaultDetailStateHolder()

    val searchQuery: StateFlow<String> = searchFilterState.searchQuery
    val selectedCategory: StateFlow<String?> = searchFilterState.selectedCategory
    val selectedTab: StateFlow<VaultTab> = searchFilterState.selectedTab

    var isSearchActive: Boolean
        get() = searchFilterState.isSearchActive
        set(value) {
            searchFilterState.isSearchActive = value
        }

    var isMoreMenuExpanded: Boolean
        get() = searchFilterState.isMoreMenuExpanded
        set(value) {
            searchFilterState.isMoreMenuExpanded = value
        }

    var showTOTPCode by mutableStateOf(true)

    // 自动填充状态
    var isAutofillEnabled by mutableStateOf(false)
        private set

    private val _totpStates = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val totpStates: StateFlow<Map<Int, TotpState>> = _totpStates

    var addType: AddType
        get() = detailState.addType
        set(value) {
            detailState.addType = value
        }

    var detailItem: VaultEntry?
        get() = detailState.detailItem
        set(value) {
            detailState.detailItem = value
        }

    var itemToDelete: VaultEntry?
        get() = detailState.itemToDelete
        set(value) {
            detailState.itemToDelete = value
        }

    var showIconPicker: Boolean
        get() = detailState.showIconPicker
        set(value) {
            detailState.showIconPicker = value
        }

    var shouldStartDetailInEditMode: Boolean
        get() = detailState.shouldStartDetailInEditMode
        private set(value) {
            detailState.shouldStartDetailInEditMode = value
        }

    var shouldStartTotpEdit: Boolean
        get() = detailState.shouldStartTotpEdit
        private set(value) {
            detailState.shouldStartTotpEdit = value
        }

    val prefilledUsername: String?
        get() = detailState.prefilledUsername

    val prefilledPassword: String?
        get() = detailState.prefilledPassword

    val prefilledTotpSecret: String?
        get() = detailState.prefilledTotpSecret

    val availableCategories: StateFlow<List<String>> = vaultUseCases.getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultSummary>> =
        combine(searchFilterState.searchQuery, searchFilterState.selectedCategory) { query, category ->
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

    private fun clearDetailLaunchState() {
        shouldStartDetailInEditMode = false
        shouldStartTotpEdit = false
        detailState.prefilledUsername = null
        detailState.prefilledPassword = null
        detailState.prefilledTotpSecret = null
    }

    fun consumeDetailLaunchState() {
        clearDetailLaunchState()
    }

    fun showDetail(entry: VaultEntry) {
        clearDetailLaunchState()
        detailItem = entry
    }

    fun showDetailForEdit(entry: VaultEntry) {
        clearDetailLaunchState()
        shouldStartDetailInEditMode = true

        if (entry.totpSecret.isNullOrBlank()) {
            shouldStartTotpEdit = false
            detailState.prefilledUsername = cryptoSupport.decryptSilently(entry.username)
            detailState.prefilledPassword = cryptoSupport.decryptSilently(entry.password)
        } else {
            shouldStartTotpEdit = true
            detailState.prefilledTotpSecret = cryptoSupport.decryptTotpSecret(entry.totpSecret)
        }

        detailItem = entry
    }

    fun showDetailForEdit(entry: VaultSummary) {
        loadEntryById(entry.id) { showDetailForEdit(it) }
    }

    fun showDetail(entry: VaultSummary) {
        loadEntryById(entry.id) { showDetail(it) }
    }
    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch {
            vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) }
        }
    }
    fun dismissDetail() {
        detailItem = null
        clearDetailLaunchState()
    }
    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntry(entry)
            addType = AddType.NONE
        }
    }
    
    /**
     * 添加条目并自动下载 favicon（如果提供了域名）
     */
    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntryWithFavicon(entry, domain)
            addType = AddType.NONE
        }
    }
    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.updateEntry(entry)
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
            val updated = entryLifecycleSupport.saveCustomIcon(context, item, uri)

            if (updated != null) {
                updateVaultEntry(updated)
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
                    dismissDetail()
                }
                entryLifecycleSupport.deleteEntry(entry)
                itemToDelete = null
                _totpStates.update { it - entry.id }
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            // 同样处理快速删除时的详情弹窗状态
            if (detailItem?.id == entry.id) {
                dismissDetail()
            }
            entryLifecycleSupport.deleteEntry(entry)
            _totpStates.update { it - entry.id }
        }
    }

    fun quickDelete(entry: VaultSummary) {
        loadEntryById(entry.id) { quickDelete(it) }
    }

    fun onSearchQueryChange(q: String) {
        searchFilterState.updateSearchQuery(q)
    }

    fun setSelectedCategory(category: String?) {
        searchFilterState.updateSelectedCategory(category)
    }

    fun clearSelectedCategory() {
        setSelectedCategory(null)
    }

    fun selectTab(tab: VaultTab) {
        searchFilterState.updateSelectedTab(tab)
    }

    fun toggleSearch(active: Boolean) {
        searchFilterState.toggleSearch(active)
    }

    fun unlockTotp(entryId: Int, decryptedSecret: String) {
        _totpStates.update { current ->
            totpSupport.unlock(current, entryId, decryptedSecret)
        }
    }
}


