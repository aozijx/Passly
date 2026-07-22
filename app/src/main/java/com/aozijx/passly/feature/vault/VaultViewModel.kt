package com.aozijx.passly.feature.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.data.repository.command.EntryCommandHandler
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.VaultListItem
import com.aozijx.passly.domain.model.settings.SortOption
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.usecase.settings.PortableSettingsUseCases
import com.aozijx.passly.domain.usecase.vault.VaultQueryUseCases
import com.aozijx.passly.feature.vault.contract.VaultEffect
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.internal.DetailCoordinator
import com.aozijx.passly.feature.vault.internal.EntryIconHelper
import com.aozijx.passly.feature.vault.internal.EntryManager
import com.aozijx.passly.feature.vault.internal.SearchFilterState
import com.aozijx.passly.feature.vault.internal.TotpCoordinator
import com.aozijx.passly.feature.vault.internal.VaultDetailCoordinatorState
import com.aozijx.passly.feature.vault.internal.VaultListCoordinator
import com.aozijx.passly.feature.vault.internal.VaultQueryCoordinator
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.TotpState
import com.aozijx.passly.feature.vault.model.VaultTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    application: Application,
    private val vaultQueryUseCases: VaultQueryUseCases,
    private val entryCommandHandler: EntryCommandHandler,
    private val portableSettingsUseCases: PortableSettingsUseCases,
    private val faviconRepository: FaviconRepository
) : AndroidViewModel(application) {

    private val _effects = MutableSharedFlow<VaultEffect>(extraBufferCapacity = 1)

    private fun emitError(message: String) {
        _effects.tryEmit(VaultEffect.ShowError(message))
    }

    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> vaultQueryUseCases.getTotpCode(config) },
        loadEntryById = { vaultQueryUseCases.getById(it) }
    )
    private val detail = DetailCoordinator()
    private val entryManager = EntryManager(
        scope = viewModelScope,
        entryCommandHandler = entryCommandHandler,
        vaultQueryUseCases = vaultQueryUseCases,
        faviconRepository = faviconRepository,
        iconHelper = EntryIconHelper(),
        detail = detail,
        totp = totp,
        onError = { emitError(it) }
    )

    private val queryCoordinator = VaultQueryCoordinator(vaultQueryUseCases)
    private val searchFilter = SearchFilterState(
        viewModelScope,
        initialSort = SortOption.DEFAULT
    )

    private val isAutoDownloadIcons: StateFlow<Boolean> =
        portableSettingsUseCases.isAutoDownloadIcons
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val listCoordinator = VaultListCoordinator(
        scope = viewModelScope,
        queryCoordinator = queryCoordinator,
        searchFilter = searchFilter,
        vaultQueryUseCases = vaultQueryUseCases,
        entryManager = entryManager,
        isAutoDownloadIcons = isAutoDownloadIcons
    )

    private val _showTOTPCode = MutableStateFlow(true)

    private val visibleTabs: StateFlow<List<VaultTab>> =
        portableSettingsUseCases.visibleVaultTabs
            .map { VaultTab.resolveVisible(it ?: VaultTab.defaultVisibleKeys) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VaultTab.resolveVisible(VaultTab.defaultVisibleKeys)
            )

    private val settingsState: StateFlow<Triple<List<VaultTab>, Boolean, Boolean>> =
        combine(visibleTabs, isAutoDownloadIcons, _showTOTPCode) { tabs, auto, show ->
            Triple(tabs, auto, show)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Triple(
                VaultTab.resolveVisible(VaultTab.defaultVisibleKeys),
                true,
                true
            )
        )

    private val sensitiveState: StateFlow<Pair<VaultDetailCoordinatorState, Map<String, TotpState>>> =
        combine(detail.coordinatorStateFlow, totp.states) { detailState, totpStates ->
            detailState to totpStates
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VaultDetailCoordinatorState() to emptyMap()
        )

    val uiState: StateFlow<VaultUiState> = combine(
        searchFilter.uiStateFlow,
        settingsState,
        listCoordinator.state,
        sensitiveState
    ) { search, settings, list, sensitive ->
        val (tabs, autoIcons, showCode) = settings
        val (detailState, totpStates) = sensitive
        VaultUiState(
            searchQuery = search.searchQuery,
            selectedCategory = search.selectedCategory,
            selectedTab = search.selectedTab,
            selectedSort = search.selectedSort,
            isSearchActive = search.isSearchActive,
            isMoreMenuExpanded = search.isMoreMenuExpanded,
            isVaultItemsLoading = list.isLoading,
            availableCategories = list.categories,
            visibleTabs = tabs,
            isAutoDownloadIcons = autoIcons,
            vaultItems = list.items,
            vaultItemsByTab = list.itemsByTab,
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
    fun selectSortOption(sort: SortOption) {
        searchFilter.updateSelectedSort(sort)
        viewModelScope.launch { portableSettingsUseCases.setVaultSortOption(sort) }
    }
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
    fun toggleShowTOTPCode() {
        _showTOTPCode.value = !_showTOTPCode.value
    }

    val addType: AddType? get() = detail.addType
    fun setAddType(type: AddType?) = detail.setAddType(type)
    val itemToDelete: VaultListItem? get() = detail.itemToDelete
    fun setItemToDelete(item: VaultListItem?) = detail.setItemToDelete(item)

    fun showDetailIconPicker() = detail.showIconPicker()
    fun hideDetailIconPicker() = detail.hideIconPicker()

    fun autoUnlockTotp(entryId: String) = totp.autoUnlock(entryId)

    init {
        totp.start { listCoordinator.state.value.items }

        viewModelScope.launch {
            portableSettingsUseCases.vaultSortOption.first().let {
                searchFilter.updateSelectedSort(it)
            }
        }
    }

    // --- 业务协调 ---
    fun showDetail(item: VaultListItem) {
        viewModelScope.launch {
            val entry = vaultQueryUseCases.getById(item.id) ?: return@launch
            detail.showDetail(entry)
            totp.autoUnlock(item.id)
            entryCommandHandler.recordUsage(
                item.id,
                com.aozijx.passly.domain.model.activity.ActivityType.VIEW
            )
        }
    }

    fun dismissDetail() {
        detail.dismissDetail()
    }

    fun loadEntryById(entryId: String, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { vaultQueryUseCases.getById(entryId)?.let { onLoaded(it) } }
    }

    fun decryptSingle(
        encryptedData: String,
        authenticate: (onSuccess: () -> Unit) -> Unit,
        onResult: (String?) -> Unit
    ) {
        if (encryptedData.isEmpty()) {
            onResult("")
            return
        }
        authenticate {
            onResult(encryptedData)
        }
    }

    // --- 条目操作委托 ---
    fun addItem(entry: VaultEntry) = entryManager.addItem(entry)
    fun addItem(entry: VaultEntry, domain: String) = entryManager.addItem(entry, domain)
    fun updateVaultEntry(entry: VaultEntry) = entryManager.updateEntry(entry)
    fun quickDelete(item: VaultListItem) = entryManager.deleteEntryById(item.id)
    fun confirmDelete() {
        val item = itemToDelete ?: return
        viewModelScope.launch {
            val entry = vaultQueryUseCases.getById(item.id) ?: return@launch
            entryManager.deleteEntry(entry)
        }
    }

    fun saveCustomIcon(entry: VaultEntry, uri: Uri) {
        entryManager.saveCustomIcon(
            context = getApplication(),
            item = entry,
            uri = uri,
            onFailed = {
                _effects.tryEmit(VaultEffect.ShowToast("图标保存失败"))
            }
        )
    }
}
