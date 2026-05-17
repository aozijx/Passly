package com.aozijx.passly.features.vault

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
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
import com.aozijx.passly.features.vault.contract.VaultUiState
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(
    application: Application,
    private val vaultUseCases: VaultUseCases,
    systemSettingsUseCases: SystemSettingsUseCases
) : AndroidViewModel(application) {

    private val autofill = AutofillCoordinator()
    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> vaultUseCases.getTotpCode(config) },
        decryptSecret = { encrypted -> encrypted }
    )
    private val detail = DetailCoordinator()
    private val entryManager = EntryManager(
        scope = viewModelScope,
        vaultUseCases = vaultUseCases,
        iconHelper = EntryIconHelper(),
        detail = detail,
        totp = totp
    )

    private val queryCoordinator = VaultQueryCoordinator(vaultUseCases)
    private val searchFilter = SearchFilterState(viewModelScope)

    // --- 内部状态 (Private) ---
    private val _isVaultItemsLoading = MutableStateFlow(true)
    private val _showTOTPCode = MutableStateFlow(true)

    // --- 基础数据流 (Private) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val availableCategories: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab ->
            vaultUseCases.observeCategoriesByFilter(tab.entryFilter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val visibleTabs: StateFlow<List<VaultTab>> =
        systemSettingsUseCases.visibleVaultTabs
            .map { VaultTab.resolveVisible(it ?: VaultTab.defaultVisibleKeys) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VaultTab.resolveVisible(VaultTab.defaultVisibleKeys)
            )

    private val isAutoDownloadIcons: StateFlow<Boolean> =
        systemSettingsUseCases.isAutoDownloadIcons
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val vaultItems: StateFlow<List<VaultSummary>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilter.normalizedSelectedCategory,
        distinctSelectedTab = searchFilter.distinctSelectedTab
    ).onEach { items ->
        _isVaultItemsLoading.value = false
        if (isAutoDownloadIcons.value) {
            entryManager.downloadMissingIcons(items)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val loadingTrigger = combine(
        searchFilter.searchQuery,
        searchFilter.selectedCategory
    ) { query, category -> Pair(query.trim(), category?.trim()) }
        .distinctUntilChanged()

    private val settingsState: StateFlow<Triple<List<VaultTab>, Boolean, Boolean>> =
        combine(visibleTabs, isAutoDownloadIcons, _showTOTPCode) { tabs, auto, show ->
            Triple(tabs, auto, show)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(VaultTab.resolveVisible(VaultTab.defaultVisibleKeys), true, true)
        )

    private val sensitiveState: StateFlow<Pair<VaultDetailCoordinatorState, Map<Int, TotpState>>> =
        combine(detail.coordinatorStateFlow, totp.states) { detailState, totpStates ->
            detailState to totpStates
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VaultDetailCoordinatorState() to emptyMap()
        )

    private val contentState: StateFlow<Triple<Boolean, List<String>, List<VaultSummary>>> =
        combine(
            _isVaultItemsLoading,
            availableCategories,
            vaultItems
        ) { loading, categories, items ->
            Triple(loading, categories, items)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(true, emptyList(), emptyList())
        )

    // --- 统一 UI 状态暴露 (类型安全) ---
    val uiState: StateFlow<VaultUiState> = combine(
        searchFilter.uiStateFlow,
        settingsState,
        contentState,
        sensitiveState
    ) { search, settings, content, sensitive ->
        val (tabs, autoIcons, showCode) = settings
        val (loading, cats, items) = content
        val (detailState, totpStates) = sensitive
        VaultUiState(
            searchQuery = search.searchQuery,
            selectedCategory = search.selectedCategory,
            selectedTab = search.selectedTab,
            isSearchActive = search.isSearchActive,
            isMoreMenuExpanded = search.isMoreMenuExpanded,
            isVaultItemsLoading = loading,
            availableCategories = cats,
            visibleTabs = tabs,
            isAutoDownloadIcons = autoIcons,
            vaultItems = items,
            vaultItemsByTab = mapOf(
                VaultTab.ALL to items,
                VaultTab.PASSWORDS to items.filter { it.totpSecret.isNullOrBlank() },
                VaultTab.TOTP to items.filter { !it.totpSecret.isNullOrBlank() }
            ),
            showTOTPCode = showCode,
            totpStates = totpStates,
            detailCoordinatorState = detailState
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VaultUiState()
    )

    // --- 操作方法 ---
    fun expandMoreMenu(expanded: Boolean) = searchFilter.expandMoreMenu(expanded)
    fun onSearchQueryChange(q: String) = searchFilter.updateSearchQuery(q)
    fun setSelectedCategory(category: String?) = searchFilter.updateSelectedCategory(category)
    fun clearSelectedCategory() = setSelectedCategory(null)
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
    fun toggleShowTOTPCode() {
        _showTOTPCode.value = !_showTOTPCode.value
    }

    // --- 其他业务状态 ---
    val isAutofillEnabled: Boolean get() = autofill.isEnabled
    fun openAutofillSettings(context: Context) = autofill.requestEnable(context)

    val addType: AddType? get() = detail.addType
    fun setAddType(type: AddType?) = detail.setAddType(type)
    val itemToDelete: VaultEntry? get() = detail.itemToDelete
    fun setItemToDelete(entry: VaultEntry?) = detail.setItemToDelete(entry)

    fun showDetailIconPicker() = detail.showIconPicker()
    fun hideDetailIconPicker() = detail.hideIconPicker()

    fun autoUnlockTotp(entry: VaultEntry) = totp.autoUnlock(entry)
    fun autoUnlockTotp(entry: VaultSummary) = totp.autoUnlock(entry)
    fun clearDetailSensitiveState(entryId: Int) = totp.clearSensitiveState(entryId)

    init {
        viewModelScope.launch {
            loadingTrigger.drop(1).collect { _isVaultItemsLoading.value = true }
        }
        totp.start { vaultItems.value }
        autofill.refreshStatus(getApplication())
    }

    // --- 业务协调 ---
    fun showDetail(entry: VaultEntry) {
        detail.showDetail(entry)
        totp.autoUnlock(entry)
        viewModelScope.launch { vaultUseCases.recordUsage(entry.id) }
    }
    fun showDetail(entry: VaultSummary) = loadEntryById(entry.id) { showDetail(it) }

    fun dismissDetail() {
        detail.dismissDetail()
    }

    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) } }
    }

    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        promptTitle: String,
        promptSubtitle: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) = vaultUseCases.decryptSingleWithAuth(
        activity,
        encryptedData,
        promptTitle,
        promptSubtitle,
        authenticate,
        onResult
    )

    // --- 条目操作委托 ---
    fun addItem(entry: VaultEntry) = entryManager.addItem(entry)
    fun addItem(entry: VaultEntry, domain: String) = entryManager.addItem(entry, domain)
    fun updateVaultEntry(entry: VaultEntry) = entryManager.updateEntry(entry)
    fun quickDelete(entry: VaultSummary) = entryManager.deleteEntryById(entry.id)
    fun confirmDelete() = detail.itemToDelete?.let { entryManager.deleteEntry(it) }

    fun saveCustomIcon(item: VaultEntry, uri: Uri) {
        entryManager.saveCustomIcon(
            context = getApplication(),
            item = item,
            uri = uri,
            onFailed = {
                Toast.makeText(
                    getApplication(),
                    R.string.vault_toast_save_icon_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}