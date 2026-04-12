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
import com.aozijx.passly.features.vault.internal.VaultAutofillSupport
import com.aozijx.passly.features.vault.internal.VaultCryptoSupport
import com.aozijx.passly.features.vault.internal.VaultDetailCoordinator
import com.aozijx.passly.features.vault.internal.VaultEntryFileSupport
import com.aozijx.passly.features.vault.internal.VaultEntryLifecycleSupport
import com.aozijx.passly.features.vault.internal.VaultSearchFilterStateHolder
import com.aozijx.passly.features.vault.internal.VaultTotpCoordinator
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
    private val autofillSupport = VaultAutofillSupport()
    private val cryptoSupport = VaultCryptoSupport()
    private val entryFileSupport = VaultEntryFileSupport()
    private val entryLifecycleSupport = VaultEntryLifecycleSupport(vaultUseCases, entryFileSupport)
    private val onDemandQuerySupport = VaultOnDemandQuerySupport(vaultUseCases)
    private val searchFilterState = VaultSearchFilterStateHolder()
    private val detailCoordinator = VaultDetailCoordinator()
    private val totpCoordinator = VaultTotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> vaultUseCases.getTotpCode(config) },
        decryptSecret = { encrypted -> cryptoSupport.decryptTotpSecret(encrypted) }
    )

    // --- Search / Filter ---
    val searchQuery: StateFlow<String> = searchFilterState.searchQuery
    val selectedCategory: StateFlow<String?> = searchFilterState.selectedCategory
    val selectedTab: StateFlow<VaultTab> = searchFilterState.selectedTab
    val isSearchActive: Boolean get() = searchFilterState.isSearchActive
    val isMoreMenuExpanded: Boolean get() = searchFilterState.isMoreMenuExpanded
    fun expandMoreMenu(expanded: Boolean) = searchFilterState.expandMoreMenu(expanded)

    // --- Display state ---
    var showTOTPCode by mutableStateOf(true)
    val isAutofillEnabled: Boolean get() = autofillSupport.isEnabled

    // --- TOTP & loading state ---
    val totpStates: StateFlow<Map<Int, TotpState>> = totpCoordinator.states
    private val _isVaultItemsLoading = MutableStateFlow(true)
    val isVaultItemsLoading: StateFlow<Boolean> = _isVaultItemsLoading

    // --- Detail / Add / Delete state ---
    val addType: AddType get() = detailCoordinator.addType
    fun setAddType(type: AddType) = detailCoordinator.setAddType(type)
    internal val detailCoordinatorState: VaultDetailCoordinatorState get() = detailCoordinator.coordinatorState
    val itemToDelete: VaultEntry? get() = detailCoordinator.itemToDelete
    fun setItemToDelete(entry: VaultEntry?) = detailCoordinator.setItemToDelete(entry)

    // --- Data flows ---
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
        debouncedSearchQuery = searchFilterState.debouncedSearchQuery,
        normalizedSelectedCategory = searchFilterState.normalizedSelectedCategory,
        distinctSelectedTab = searchFilterState.distinctSelectedTab
    ).onEach { items ->
        if (_isVaultItemsLoading.value) _isVaultItemsLoading.value = false
        entryLifecycleSupport.autoUpdateMissingIcons(items, viewModelScope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        totpCoordinator.start { vaultItems.value }
        updateAutofillStatus()
    }

    // --- Autofill ---
    fun updateAutofillStatus() {
        autofillSupport.isEnabled = autofillSupport.isAutofillEnabled(getApplication())
    }

    fun openAutofillSettings(context: Context) {
        expandMoreMenu(false)
        val started = autofillSupport.openAutofillSettings(context)
        if (!started) {
            Toast.makeText(
                context,
                context.getString(R.string.vault_toast_enable_autofill_manual),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- Crypto ---
    fun decryptSingle(
        activity: FragmentActivity,
        encryptedData: String,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) = cryptoSupport.decryptSingle(activity, encryptedData, authenticate, onResult)

    fun decryptMultiple(
        activity: FragmentActivity,
        encryptedList: List<String>,
        authenticate: (FragmentActivity, String, String, ((String) -> Unit)?, () -> Unit) -> Unit,
        onResult: (List<String?>) -> Unit
    ) = cryptoSupport.decryptMultiple(activity, encryptedList, authenticate, onResult)

    // --- TOTP ---
    fun autoUnlockTotp(entry: VaultEntry) = totpCoordinator.autoUnlock(entry.toSummary())
    fun autoUnlockTotp(entry: VaultSummary) = totpCoordinator.autoUnlock(entry)
    fun unlockTotp(entryId: Int, decryptedSecret: String) = totpCoordinator.unlock(entryId, decryptedSecret)

    // --- Detail management ---
    fun showDetail(entry: VaultEntry) = detailCoordinator.showDetail(entry)
    fun showDetailForEdit(entry: VaultEntry) = detailCoordinator.showDetailForEdit(entry)
    fun showDetail(entry: VaultSummary) = loadEntryById(entry.id) { showDetail(it) }
    fun loadEntryById(entryId: Int, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { vaultUseCases.getEntryById(entryId)?.let { onLoaded(it) } }
    }
    fun dismissDetail() = detailCoordinator.dismissDetail()
    fun showDetailIconPicker() = detailCoordinator.showIconPicker()
    fun hideDetailIconPicker() = detailCoordinator.hideIconPicker()

    // --- Entry CRUD ---
    fun addItem(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntry(entry)
            setAddType(AddType.NONE)
        }
    }

    fun addItem(entry: VaultEntry, domain: String) {
        viewModelScope.launch {
            entryLifecycleSupport.addEntryWithFavicon(entry, domain)
            setAddType(AddType.NONE)
        }
    }

    fun updateVaultEntry(entry: VaultEntry) {
        viewModelScope.launch {
            entryLifecycleSupport.updateEntry(entry)
            detailCoordinator.updateEntry(entry)
            totpCoordinator.removeEntry(entry.id)
            if (!entry.totpSecret.isNullOrBlank()) totpCoordinator.autoUnlock(entry.toSummary())
        }
    }

    fun saveCustomIcon(item: VaultEntry, uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val updated = entryLifecycleSupport.saveCustomIcon(context, item, uri)
            if (updated != null) updateVaultEntry(updated)
            else Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show()
        }
    }

    fun confirmDelete() {
        detailCoordinator.itemToDelete?.let { entry ->
            viewModelScope.launch {
                if (detailCoordinator.isViewingEntry(entry.id)) detailCoordinator.dismissDetail()
                entryLifecycleSupport.deleteEntry(entry)
                detailCoordinator.setItemToDelete(null)
                totpCoordinator.removeEntry(entry.id)
            }
        }
    }

    fun quickDelete(entry: VaultEntry) {
        viewModelScope.launch {
            if (detailCoordinator.isViewingEntry(entry.id)) detailCoordinator.dismissDetail()
            entryLifecycleSupport.deleteEntry(entry)
            totpCoordinator.removeEntry(entry.id)
        }
    }

    fun quickDelete(entry: VaultSummary) = loadEntryById(entry.id) { quickDelete(it) }

    // --- Search / Filter ---
    fun onSearchQueryChange(q: String) = searchFilterState.updateSearchQuery(q)
    fun setSelectedCategory(category: String?) = searchFilterState.updateSelectedCategory(category)
    fun clearSelectedCategory() = setSelectedCategory(null)
    fun selectTab(tab: VaultTab) = searchFilterState.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilterState.toggleSearch(active)
}
