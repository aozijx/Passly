package com.aozijx.passly.feature.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.otp.OtpResult
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.lookup.EntryListItem
import com.aozijx.passly.domain.model.settings.VaultSortSpec
import com.aozijx.passly.domain.repository.activity.ActivityRecorder
import com.aozijx.passly.domain.repository.entry.EntryCommandRepository
import com.aozijx.passly.domain.repository.entry.EntryListQueryRepository
import com.aozijx.passly.domain.repository.entry.EntryQueryRepository
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.repository.otp.OtpConfigRepository
import com.aozijx.passly.domain.repository.settings.PortableRepository
import com.aozijx.passly.domain.service.entry.EntryFieldReader
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
import com.aozijx.passly.feature.vault.model.OtpUiState
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
    private val entryQueryRepository: EntryQueryRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val otpConfigRepository: OtpConfigRepository,
    private val portableRepository: PortableRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val activityRecorder: ActivityRecorder,
    private val faviconRepository: FaviconRepository,
    val entryFieldReader: EntryFieldReader
) : AndroidViewModel(application) {

    private val _effects = MutableSharedFlow<VaultEffect>(extraBufferCapacity = 1)

    private fun emitError(message: String) {
        _effects.tryEmit(VaultEffect.ShowError(message))
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    /**
     * 强制重新读取数据库，刷新整个列表。
     * 保存密码或导入备份后调用此方法确保 UI 反映最新数据。
     *
     * 使用递增计数器作为触发值，确保每次调用都产生不同的 [refreshTrigger] 发射，
     * 从而绕过后继 [distinctUntilChanged]，并使 [flatMapLatest] 自动取消
     * 前一次未完成的数据库订阅，防止竞态条件。
     */
    fun refreshItems() {
        _refreshTrigger.value++
    }

    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> OtpGenerator.generate(config) },
        loadOtpConfig = { otpConfigRepository.getConfig(it) }
    )
    private val detail = DetailCoordinator()
    private val entryManager = EntryManager(
        scope = viewModelScope,
        entryCommandRepository = entryCommandRepository,
        entryQueryRepository = entryQueryRepository,
        faviconRepository = faviconRepository,
        iconHelper = EntryIconHelper(),
        detail = detail,
        totp = totp,
        onError = { emitError(it) },
        onRefreshItems = { refreshItems() }
    )

    private val queryCoordinator = VaultQueryCoordinator(entryListQueryRepository)
    private val searchFilter = SearchFilterState(
        viewModelScope,
        initialSort = VaultSortSpec.DEFAULT
    )

    private val isAutoDownloadIcons: StateFlow<Boolean> =
        portableRepository.isAutoDownloadIcons
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val listCoordinator = VaultListCoordinator(
        scope = viewModelScope,
        queryCoordinator = queryCoordinator,
        searchFilter = searchFilter,
        entryListQueryRepository = entryListQueryRepository,
        entryManager = entryManager,
        isAutoDownloadIcons = isAutoDownloadIcons,
        refreshTrigger = _refreshTrigger
    )

    private val _showTOTPCode = MutableStateFlow(true)

    private val visibleTabs: StateFlow<List<VaultTab>> =
        portableRepository.visibleVaultTabs
            .map { VaultTab.resolveVisible(it ?: VaultTab.defaultVisibleKeys) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                VaultTab.resolveVisible(VaultTab.defaultVisibleKeys)
            )

    /**
     * OTP 状态（高频率变化，每秒更新）。
     * 独立 Flow 避免 OTP 更新触发整个 UI 状态重组。
     */
    val totpStatesFlow: StateFlow<Map<String, OtpUiState>> = totp.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * 详情协调器状态。
     * 独立 Flow 避免详情状态变化触发整个 UI 状态重组。
     */
    val detailStateFlow: StateFlow<VaultDetailCoordinatorState> = detail.coordinatorStateFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VaultDetailCoordinatorState()
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

    val uiState: StateFlow<VaultUiState> = combine(
        searchFilter.uiStateFlow,
        settingsState,
        listCoordinator.state
    ) { search, settings, list ->
        val (tabs, autoIcons, showCode) = settings
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
            showTOTPCode = showCode
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
    fun selectSortOption(sort: VaultSortSpec) {
        searchFilter.updateSelectedSort(sort)
        viewModelScope.launch { portableRepository.setVaultSortOption(sort) }
    }
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
    fun toggleShowTOTPCode() {
        _showTOTPCode.value = !_showTOTPCode.value
    }

    val addType: AddType? get() = detail.addType
    fun setAddType(type: AddType?) = detail.setAddType(type)
    val itemToDelete: EntryListItem? get() = detail.itemToDelete
    fun setItemToDelete(item: EntryListItem?) = detail.setItemToDelete(item)

    fun showDetailIconPicker() = detail.showIconPicker()
    fun hideDetailIconPicker() = detail.hideIconPicker()

    fun autoUnlockTotp(entryId: String) = totp.autoUnlock(entryId)

    /**
     * 生成 HOTP 验证码并持久化递增后的 counter。
     *
     * 流程：
     * 1. [totp.generateHotpCode] 使用当前 counter 生成验证码
     * 2. 成功后将 [OtpResult.Success.nextCounter] 持久化到数据库
     *
     * @param entryId 条目 ID
     * @param onResult 回调：code 为生成的验证码（null 表示生成失败）
     */
    fun generateHotpCode(entryId: String, onResult: (code: String?) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = totp.generateHotpCode(entryId)) {
                is OtpResult.Success -> {
                    val entry = entryQueryRepository.getById(entryId)
                    if (entry != null && result.nextCounter != null) {
                        val newSecret = entry.secret.otp?.let { otpData ->
                            val updatedConfig =
                                otpData.config?.copy(counter = result.nextCounter)
                            entry.secret.copy(otp = otpData.copy(config = updatedConfig))
                        } ?: entry.secret
                        entryCommandRepository.updateEntry(
                            entryId,
                            entry.entryVersion,
                            EntryChanges(secret = newSecret)
                        )
                    }
                    onResult(result.code)
                }

                is OtpResult.Failure -> {
                    onResult(null)
                }
            }
        }
    }

    init {
        totp.start()

        viewModelScope.launch {
            portableRepository.vaultSortOption.first().let {
                searchFilter.updateSelectedSort(it)
            }
        }
    }

    // --- 业务协调 ---
    fun showDetail(item: EntryListItem) {
        viewModelScope.launch {
            val entry = entryQueryRepository.getById(item.id) ?: return@launch
            detail.showDetail(entry)
            totp.autoUnlock(item.id)
            activityRecorder.recordUsage(
                item.id,
                ActivityType.VIEW
            )
        }
    }

    fun dismissDetail() {
        detail.dismissDetail()
    }

    fun loadEntryById(entryId: String, onLoaded: (VaultEntry) -> Unit) {
        viewModelScope.launch { entryQueryRepository.getById(entryId)?.let { onLoaded(it) } }
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
    fun quickDelete(item: EntryListItem) = entryManager.deleteEntryById(item.id)
    fun confirmDelete() {
        val item = itemToDelete ?: return
        viewModelScope.launch {
            val entry = entryQueryRepository.getById(item.id) ?: return@launch
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

    override fun onCleared() {
        super.onCleared()
        totp.clearAllSensitiveState()
    }
}
