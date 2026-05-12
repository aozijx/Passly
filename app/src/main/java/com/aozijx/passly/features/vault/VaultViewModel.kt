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
import com.aozijx.passly.core.designsystem.model.TotpState
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.domain.usecase.vault.VaultUseCases
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.vault.internal.AutofillCoordinator
import com.aozijx.passly.features.vault.internal.DetailCoordinator
import com.aozijx.passly.features.vault.internal.EntryIconHelper
import com.aozijx.passly.features.vault.internal.EntryManager
import com.aozijx.passly.features.vault.internal.SearchFilterState
import com.aozijx.passly.features.vault.internal.TotpCoordinator
import com.aozijx.passly.features.vault.internal.VaultQueryCoordinator
import com.aozijx.passly.features.vault.model.AddType
import com.aozijx.passly.features.vault.model.VaultTab
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(
    application: Application,
    private val vaultUseCases: VaultUseCases,
    private val systemSettingsUseCases: SystemSettingsUseCases
) : AndroidViewModel(application) {
    private val autofill = AutofillCoordinator()
    private val iconHelper = EntryIconHelper()
    private val entryManager = EntryManager(vaultUseCases, iconHelper)
    private val queryCoordinator = VaultQueryCoordinator(vaultUseCases)
    private val searchFilter = SearchFilterState()
    private val detail = DetailCoordinator()
    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> vaultUseCases.getTotpCode(config) },
        decryptSecret = { encrypted -> encrypted }
    )

    // --- Search / Filter ---
    val searchQuery: StateFlow<String> = searchFilter.searchQuery
    val selectedCategory: StateFlow<String?> = searchFilter.selectedCategory
    val selectedTab: StateFlow<VaultTab> = searchFilter.selectedTab
    val isSearchActive: StateFlow<Boolean> = searchFilter.isSearchActive
    val isMoreMenuExpanded: StateFlow<Boolean> = searchFilter.isMoreMenuExpanded
    fun expandMoreMenu(expanded: Boolean) = searchFilter.expandMoreMenu(expanded)

    // --- Display state ---
    var showTOTPCode by mutableStateOf(true)
    val isAutofillEnabled: Boolean get() = autofill.isEnabled

    // --- TOTP & loading state ---
    val totpStates: StateFlow<Map<Int, TotpState>> = totp.states
    private val _isVaultItemsLoading = MutableStateFlow(true)
    val isVaultItemsLoading: StateFlow<Boolean> = _isVaultItemsLoading

    val addType: AddType? get() = detail.addType
    fun setAddType(type: AddType?) = detail.setAddType(type)
    internal val detailCoordinatorState: VaultDetailCoordinatorState get() = detail.coordinatorState
    val itemToDelete: VaultEntry? get() = detail.itemToDelete
    fun setItemToDelete(entry: VaultEntry?) = detail.setItemToDelete(entry)

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableCategories: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab ->
            vaultUseCases.getCategoriesByFilter(tab.entryFilter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleTabs: StateFlow<List<VaultTab>> =
        systemSettingsUseCases.visibleVaultTabs
            .map { VaultTab.resolveVisible(it ?: VaultTab.defaultVisibleKeys) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VaultTab.resolveVisible(VaultTab.defaultVisibleKeys)
            )

    val isAutoDownloadIcons: StateFlow<Boolean> =
        systemSettingsUseCases.isAutoDownloadIcons
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /**
     * 全部数据的原始流。固定使用 VaultTab.ALL 避免滑动重载。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultItems: StateFlow<List<VaultSummary>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilter.normalizedSelectedCategory,
        distinctSelectedTab = flowOf(VaultTab.ALL)
    ).onEach { items ->
        if (_isVaultItemsLoading.value) _isVaultItemsLoading.value = false
        if (isAutoDownloadIcons.value) {
            entryManager.downloadMissingIcons(items, viewModelScope)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 核心优化：按 Tab 预处理后的数据 Map。
     * 将过滤逻辑下沉到 Flow 变换阶段（后台线程）完成。
     * UI 层只需通过 Tab 取 List，彻底消除滑动跨栏时的 UI 线程计算压力。
     */
    val vaultItemsByTab: StateFlow<Map<VaultTab, List<VaultSummary>>> = vaultItems
        .map { items ->
            mapOf(
                VaultTab.ALL to items,
                VaultTab.PASSWORDS to items.filter { it.totpSecret.isNullOrBlank() },
                VaultTab.TOTP to items.filter { !it.totpSecret.isNullOrBlank() }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        totp.start { vaultItems.value }
        autofill.refreshStatus(getApplication())
    }

    fun openAutofillSettings(context: Context) {
        expandMoreMenu(false)
        autofill.requestEnable(context)
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) = vaultUseCases.decryptSingleWithAuth(
        activity = activity,
        encryptedData = encryptedData,
        promptTitle = promptTitle,
        promptSubtitle = promptSubtitle,
        authenticate = authenticate,
        onResult = onResult
    )

    fun autoUnlockTotp(entry: VaultEntry) = totp.autoUnlock(entry)
    fun autoUnlockTotp(entry: VaultSummary) = totp.autoUnlock(entry)

    fun showDetail(entry: VaultEntry) {
        detail.showDetail(entry)
        viewModelScope.launch { vaultUseCases.recordUsage(entry.id) }
    }
    fun showDetail(entry: VaultSummary) = loadEntryById(entry.id) { showDetail(it) }
    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) } }
    }
    fun dismissDetail() {
        detailCoordinatorState.request?.entry?.id?.let { totp.clearSensitiveState(it) }
        detail.dismissDetail()
    }

    fun clearDetailSensitiveState(entryId: Int) = totp.clearSensitiveState(entryId)

    fun showDetailIconPicker() = detail.showIconPicker()
    fun hideDetailIconPicker() = detail.hideIconPicker()

    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            entryManager.addEntry(entry)
            setAddType(null)
        }
    }

    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            entryManager.addEntryWithFavicon(entry, domain)
            setAddType(null)
        }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            entryManager.updateEntry(entry)
            detail.updateEntry(entry)
            totp.onEntryUpdated(entry)
        }
    }

    fun saveCustomIcon(item: VaultEntry, uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updated = entryManager.saveCustomIcon(context, item, uri)
            if (updated != null) updateVaultEntry(updated)
            else Toast.makeText(context, R.string.vault_toast_save_icon_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun confirmDelete() {
        detail.itemToDelete?.let { entry ->
            viewModelScope.launch {
                if (detail.isViewingEntry(entry.id)) detail.dismissDetail()
                entryManager.deleteEntry(entry)
                detail.setItemToDelete(null)
                totp.clearSensitiveState(entry.id)
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            if (detail.isViewingEntry(entry.id)) detail.dismissDetail()
            entryManager.deleteEntry(entry)
            totp.clearSensitiveState(entry.id)
        }
    }

    fun quickDelete(entry: VaultSummary) = loadEntryById(entry.id) { quickDelete(it) }

    fun onSearchQueryChange(q: String) = searchFilter.updateSearchQuery(q)
    fun setSelectedCategory(category: String?) = searchFilter.updateSelectedCategory(category)
    fun clearSelectedCategory() = setSelectedCategory(null)
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
}