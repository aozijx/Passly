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
import com.aozijx.passly.core.designsystem.model.AddType
import com.aozijx.passly.core.designsystem.model.VaultTab
import com.aozijx.passly.core.designsystem.state.TotpState
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.detail.page.DetailLaunchMode
import com.aozijx.passly.features.detail.page.DetailOpenRequest
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 保险箱主界面业务逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vaultUseCases = AppContainer.domain.vaultUseCases
    private val autofillSupport = VaultAutofillSupport()
    private val cryptoSupport = VaultCryptoSupport()
    private val totpSupport = VaultTotpSupport()
    private val entryFileSupport = VaultEntryFileSupport()
    private val entryLifecycleSupport = VaultEntryLifecycleSupport(vaultUseCases, entryFileSupport)

    private val onDemandQuerySupport = VaultOnDemandQuerySupport(vaultUseCases)

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

    var isAutofillEnabled by mutableStateOf(false)
        private set

    private val _totpStates = MutableStateFlow<Map<Int, TotpState>>(emptyMap())
    val totpStates: StateFlow<Map<Int, TotpState>> = _totpStates
    private val _isVaultItemsLoading = MutableStateFlow(true)
    val isVaultItemsLoading: StateFlow<Boolean> = _isVaultItemsLoading

    var addType: AddType
        get() = detailState.addType
        set(value) {
            detailState.addType = value
        }

    internal val detailCoordinatorState: VaultDetailCoordinatorState
        get() = detailState.detailCoordinatorState

    var itemToDelete: VaultEntry?
        get() = detailState.itemToDelete
        set(value) {
            detailState.itemToDelete = value
        }

    private fun updateDetailCoordinator(transform: (VaultDetailCoordinatorState) -> VaultDetailCoordinatorState) {
        detailState.detailCoordinatorState = transform(detailState.detailCoordinatorState)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableCategories: StateFlow<List<String>> =
        searchFilterState.selectedTab.flatMapLatest { tab ->
                val entryFilter = when (tab) {
                    VaultTab.PASSWORDS -> VaultSearchRepository.EntryFilter.PASSWORD_ONLY
                    VaultTab.TOTP -> VaultSearchRepository.EntryFilter.TOTP_ONLY
                    else -> VaultSearchRepository.EntryFilter.ALL
                }
                vaultUseCases.getCategoriesByFilter(entryFilter)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 核心优化：使用处理后的流（去抖动、标准化、去重）来驱动数据库查询
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultSummary>> = onDemandQuerySupport.observeVaultItems(
        debouncedSearchQuery = searchFilterState.debouncedSearchQuery, // 1. 使用去抖动的搜索流
        normalizedSelectedCategory = searchFilterState.normalizedSelectedCategory, // 2. 使用标准化的分类流
        distinctSelectedTab = searchFilterState.distinctSelectedTab // 3. 使用去重的 Tab 流
    ).onEach { items ->
        if (_isVaultItemsLoading.value) {
            _isVaultItemsLoading.value = false
        }
        // 自动运行：当列表更新时，检查并自动补充缺失图标
        entryLifecycleSupport.autoUpdateMissingIcons(items, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startTotpRefresher()
        updateAutofillStatus()
    }

    fun updateAutofillStatus() {
        val context = getApplication<Application>()
        isAutofillEnabled = autofillSupport.isAutofillEnabled(context)
    }

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
                codeGenerator = { config -> vaultUseCases.getTotpCode(config) })
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

    fun autoUnlockTotp(entry: VaultEntry) = autoUnlockTotp(entry.toSummary())

    fun autoUnlockTotp(entry: VaultSummary) {
        if (_totpStates.value.containsKey(entry.id)) return
        val decrypted = cryptoSupport.decryptTotpSecret(entry.totpSecret)
        if (decrypted == null) {
            Logcat.w("VaultViewModel", "Auto unlock failed: secret decrypt returned null")
            return
        }
        unlockTotp(entry.id, decrypted)
    }

    fun showDetail(entry: VaultEntry) {
        updateDetailCoordinator {
            it.copy(
                request = DetailOpenRequest(entry = entry, launchMode = DetailLaunchMode.VIEW),
                isIconPickerVisible = false
            )
        }
    }

    fun showDetailForEdit(entry: VaultEntry) {
        val launchMode = if (entry.totpSecret.isNullOrBlank()) {
            DetailLaunchMode.EDIT_FIELDS
        } else {
            DetailLaunchMode.EDIT_TOTP
        }
        updateDetailCoordinator {
            it.copy(
                request = DetailOpenRequest(entry = entry, launchMode = launchMode),
                isIconPickerVisible = false
            )
        }
    }

    fun showDetail(entry: VaultSummary) = loadEntryById(entry.id) { showDetail(it) }

    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch {
            vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) }
        }
    }

    fun dismissDetail() {
        updateDetailCoordinator {
            it.copy(request = null, isIconPickerVisible = false)
        }
    }

    fun showDetailIconPicker() {
        updateDetailCoordinator { it.copy(isIconPickerVisible = true) }
    }

    fun hideDetailIconPicker() {
        updateDetailCoordinator { it.copy(isIconPickerVisible = false) }
    }

    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntry(entry)
            addType = AddType.NONE
        }
    }

    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntryWithFavicon(entry, domain)
            addType = AddType.NONE
        }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.updateEntry(entry)
            updateDetailCoordinator { current ->
                val request = current.request
                if (request?.entry?.id == entry.id) {
                    current.copy(request = request.copy(entry = entry), isIconPickerVisible = false)
                } else {
                    current.copy(isIconPickerVisible = false)
                }
            }
            _totpStates.update { it - entry.id }
            if (!entry.totpSecret.isNullOrBlank()) autoUnlockTotp(entry)
        }
    }

    fun saveCustomIcon(item: VaultEntry, uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updated = entryLifecycleSupport.saveCustomIcon(context, item, uri)
            if (updated != null) updateVaultEntry(updated) else Toast.makeText(
                context, "图片保存失败", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun confirmDelete() {
        itemToDelete?.let { entry ->
            viewModelScope.launch {
                if (detailCoordinatorState.request?.entry?.id == entry.id) dismissDetail()
                entryLifecycleSupport.deleteEntry(entry)
                itemToDelete = null
                _totpStates.update { it - entry.id }
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            if (detailCoordinatorState.request?.entry?.id == entry.id) dismissDetail()
            entryLifecycleSupport.deleteEntry(entry)
            _totpStates.update { it - entry.id }
        }
    }

    fun quickDelete(entry: VaultSummary) = loadEntryById(entry.id) { quickDelete(it) }

    fun onSearchQueryChange(q: String) = searchFilterState.updateSearchQuery(q)

    fun setSelectedCategory(category: String?) = searchFilterState.updateSelectedCategory(category)

    fun clearSelectedCategory() = setSelectedCategory(null)

    fun selectTab(tab: VaultTab) = searchFilterState.updateSelectedTab(tab)

    fun toggleSearch(active: Boolean) = searchFilterState.toggleSearch(active)

    fun unlockTotp(entryId: Int, decryptedSecret: String) {
        _totpStates.update { current -> totpSupport.unlock(current, entryId, decryptedSecret) }
    }
}