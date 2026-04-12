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
import com.aozijx.passly.domain.mapper.toSummary
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.presentation.VaultSummary
import com.aozijx.passly.domain.repository.vault.VaultSearchRepository
import com.aozijx.passly.features.detail.internal.VaultDetailCoordinatorState
import com.aozijx.passly.features.vault.internal.AutofillCoordinator
import com.aozijx.passly.features.vault.internal.CryptoHelper
import com.aozijx.passly.features.vault.internal.DetailCoordinator
import com.aozijx.passly.features.vault.internal.EntryIconHelper
import com.aozijx.passly.features.vault.internal.EntryManager
import com.aozijx.passly.features.vault.internal.SearchFilterState
import com.aozijx.passly.features.vault.internal.TotpCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 保险箱主界面业务逻辑
 */
class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val vaultUseCases = AppContainer.domain.vaultUseCases
    private val autofill = AutofillCoordinator()
    private val crypto = CryptoHelper()
    private val iconHelper = EntryIconHelper()
    private val entryManager = EntryManager(vaultUseCases, iconHelper)
    private val queryCoordinator = VaultQueryCoordinator(vaultUseCases)
    private val searchFilter = SearchFilterState()
    private val detail = DetailCoordinator()
    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> vaultUseCases.getTotpCode(config) },
        decryptSecret = { encrypted -> crypto.decryptTotpSecret(encrypted) }
    )

    // --- Search / Filter ---
    val searchQuery: StateFlow<String> = searchFilter.searchQuery
    val selectedCategory: StateFlow<String?> = searchFilter.selectedCategory
    val selectedTab: StateFlow<VaultTab> = searchFilter.selectedTab
    val isSearchActive: Boolean get() = searchFilter.isSearchActive
    val isMoreMenuExpanded: Boolean get() = searchFilter.isMoreMenuExpanded
    fun expandMoreMenu(expanded: Boolean) = searchFilter.expandMoreMenu(expanded)

    // --- Display state ---
    var showTOTPCode by mutableStateOf(true)
    val isAutofillEnabled: Boolean get() = autofill.isEnabled

    // --- TOTP & loading state ---
    val totpStates: StateFlow<Map<Int, TotpState>> = totp.states
    private val _isVaultItemsLoading = MutableStateFlow(true)
    val isVaultItemsLoading: StateFlow<Boolean> = _isVaultItemsLoading

    // --- Detail / Add / Delete state ---
    val addType: AddType get() = detail.addType
    fun setAddType(type: AddType) = detail.setAddType(type)
    internal val detailCoordinatorState: VaultDetailCoordinatorState get() = detail.coordinatorState
    val itemToDelete: VaultEntry? get() = detail.itemToDelete
    fun setItemToDelete(entry: VaultEntry?) = detail.setItemToDelete(entry)

    // --- Data flows ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val availableCategories: StateFlow<List<String>> =
        searchFilter.selectedTab.flatMapLatest { tab ->
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
    val vaultItems: StateFlow<List<VaultSummary>> = queryCoordinator.observeItems(
        debouncedSearchQuery = searchFilter.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilter.normalizedSelectedCategory,
        distinctSelectedTab = searchFilter.distinctSelectedTab
    ).onEach { items ->
        if (_isVaultItemsLoading.value) _isVaultItemsLoading.value = false
        entryManager.downloadMissingIcons(items, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        totp.start { vaultItems.value }
        autofill.refreshStatus(getApplication())
    }

    // --- Autofill ---
    fun updateAutofillStatus() = autofill.refreshStatus(getApplication())

    fun openAutofillSettings(context: Context) {
        expandMoreMenu(false)
        if (!autofill.openSettings(context)) {
            Toast.makeText(context, R.string.vault_toast_enable_autofill_manual, Toast.LENGTH_LONG).show()
        }
    }

    // --- Crypto ---
    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) = crypto.decryptSingle(activity, encryptedData, authenticate, onResult)

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (List<String?>) -> Unit
    ) = crypto.decryptMultiple(activity, encryptedList, authenticate, onResult)

    // --- TOTP ---
    fun autoUnlockTotp(entry: VaultEntry) = totp.autoUnlock(entry.toSummary())
    fun autoUnlockTotp(entry: VaultSummary) = totp.autoUnlock(entry)
    fun unlockTotp(entryId: Int, decryptedSecret: String) = totp.unlock(entryId, decryptedSecret)

    // --- Detail management ---
    fun showDetail(entry: VaultEntry) = detail.showDetail(entry)
    fun showDetailForEdit(entry: VaultEntry) = detail.showDetailForEdit(entry)
    fun showDetail(entry: VaultSummary) = loadEntryById(entry.id) { showDetail(it) }
    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) } }
    }
    fun dismissDetail() = detail.dismissDetail()
    fun showDetailIconPicker() = detail.showIconPicker()
    fun hideDetailIconPicker() = detail.hideIconPicker()

    // --- Entry CRUD ---
    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            entryManager.addEntry(entry)
            setAddType(AddType.NONE)
        }
    }

    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            entryManager.addEntryWithFavicon(entry, domain)
            setAddType(AddType.NONE)
        }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            entryManager.updateEntry(entry)
            detail.updateEntry(entry)
            totp.removeEntry(entry.id)
            if (!entry.totpSecret.isNullOrBlank()) totp.autoUnlock(entry.toSummary())
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
                totp.removeEntry(entry.id)
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            if (detail.isViewingEntry(entry.id)) detail.dismissDetail()
            entryManager.deleteEntry(entry)
            totp.removeEntry(entry.id)
        }
    }

    fun quickDelete(entry: VaultSummary) = loadEntryById(entry.id) { quickDelete(it) }

    // --- Search / Filter ---
    fun onSearchQueryChange(q: String) = searchFilter.updateSearchQuery(q)
    fun setSelectedCategory(category: String?) = searchFilter.updateSelectedCategory(category)
    fun clearSelectedCategory() = setSelectedCategory(null)
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
}
