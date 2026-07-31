package com.aozijx.passly.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.platform.VaultDataRefreshNotifier
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.secret.OtpSecret
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import com.aozijx.passly.domain.entry.repository.FaviconRepository
import com.aozijx.passly.domain.entry.repository.OtpConfigRepository
import com.aozijx.passly.domain.entry.service.EntryFieldReader
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.model.VaultSortSpec
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import com.aozijx.passly.feature.vault.contract.VaultEffect
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.entry.EntryManager
import com.aozijx.passly.feature.vault.list.SearchFilterState
import com.aozijx.passly.feature.vault.list.VaultListCoordinator
import com.aozijx.passly.feature.vault.list.VaultQueryCoordinator
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.feature.vault.model.VaultTab
import com.aozijx.passly.feature.vault.otp.TotpCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val entryQueryRepository: EntryQueryRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val otpConfigRepository: OtpConfigRepository,
    private val settingsRepository: AppSettingsRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val faviconRepository: FaviconRepository,
    val entryFieldReader: EntryFieldReader,
    private val vaultDataRefreshNotifier: VaultDataRefreshNotifier,
    private val sessionStateProvider: SessionStateProvider
) : ViewModel() {

    private val _effects = MutableSharedFlow<VaultEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private fun emitError(message: String) {
        _effects.tryEmit(VaultEffect.ShowError(message))
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    /**
     * 外部替换数据库内容后重建 Room 订阅。
     * 普通增删改依靠 Room 的失效通知，不应手动触发重复查询。
     */
    private fun requestFullReload() {
        _refreshTrigger.value++
    }

    private val totp = TotpCoordinator(
        scope = viewModelScope,
        codeGenerator = { config -> OtpGenerator.generate(config) },
        loadOtpConfig = { otpConfigRepository.getConfig(it) },
        initiallyUnlocked = sessionStateProvider.isWritable
    )
    private val _dialogState = MutableStateFlow(VaultDialogState())
    private val entryManager = EntryManager(
        scope = viewModelScope,
        entryCommandRepository = entryCommandRepository,
        entryQueryRepository = entryQueryRepository,
        faviconRepository = faviconRepository,
        totp = totp,
        onError = { emitError(it) },
        onEntryDeleted = { deletedId ->
            _dialogState.value = _dialogState.value.let { state ->
                if (state.pendingDelete?.id == deletedId) {
                    state.copy(pendingDelete = null)
                } else {
                    state
                }
            }
        }
    )

    private val queryCoordinator = VaultQueryCoordinator(entryListQueryRepository)
    private val searchFilter = SearchFilterState(
        viewModelScope,
        initialSort = VaultSortSpec.DEFAULT
    )

    private val listCoordinator = VaultListCoordinator(
        scope = viewModelScope,
        queryCoordinator = queryCoordinator,
        searchFilter = searchFilter,
        entryListQueryRepository = entryListQueryRepository,
        refreshTrigger = _refreshTrigger
    )

    private val _showTOTPCode = MutableStateFlow(true)

    fun addScannedOtp(config: OtpConfig) {
        try {
            val title = buildString {
                if (!config.issuer.isNullOrBlank()) append(config.issuer)
                if (!config.accountName.isNullOrBlank()) {
                    if (isNotEmpty()) append(": ")
                    append(config.accountName)
                }
                if (isEmpty()) append("TOTP")
            }
            val entry = VaultEntry(
                header = EntryHeader(
                    id = EntryId(""),
                    entryType = EntryType.OTP,
                    version = EntryVersion.INITIAL,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ),
                summary = EntrySummary(
                    title = title,
                    username = config.accountName ?: title,
                    icon = null
                ),
                secret = EntrySecret(otp = OtpSecret(config = config))
            )
            entryManager.addItem(entry)
        } catch (error: Exception) {
            AppTelemetry.e("SaveScannedOtp", "Failed to save scanned OTP entry", error)
            emitError("加密保存失败")
        }
    }

    private val visibleTabs: StateFlow<List<VaultTab>> =
        settingsRepository.settings.map { it.vault.visibleTabs }
            .map { config ->
                val keys = config?.tabKeys ?: VaultTab.defaultVisibleKeys
                VaultTab.resolveVisible(keys)
            }
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

    private val settingsState: StateFlow<Pair<List<VaultTab>, Boolean>> =
        combine(visibleTabs, _showTOTPCode) { tabs, show ->
            tabs to show
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            VaultTab.resolveVisible(VaultTab.defaultVisibleKeys) to true
        )

    val uiState: StateFlow<VaultUiState> = combine(
        searchFilter.uiStateFlow,
        settingsState,
        listCoordinator.state,
        _dialogState
    ) { search, settings, list, dialogs ->
        val (tabs, showCode) = settings
        VaultUiState(
            searchQuery = search.searchQuery,
            selectedEntryTypeName = search.selectedEntryTypeName,
            selectedTab = search.selectedTab,
            selectedSort = search.selectedSort,
            isSearchActive = search.isSearchActive,
            isVaultItemsLoading = list.isLoading,
            availableEntryTypes = list.entryTypes,
            visibleTabs = tabs,
            vaultItemsByTab = list.itemsByTab,
            showTOTPCode = showCode,
            addType = dialogs.addType,
            pendingDelete = dialogs.pendingDelete
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VaultUiState()
    )

    // --- 操作方法 ---
    fun onSearchQueryChange(q: String) = searchFilter.updateSearchQuery(q)
    fun setSelectedEntryType(entryTypeName: String?) =
        searchFilter.updateSelectedEntryTypeName(entryTypeName)
    fun clearSelectedEntryType() = setSelectedEntryType(null)
    fun selectSortOption(sort: VaultSortSpec) {
        searchFilter.updateSelectedSort(sort)
        viewModelScope.launch { settingsRepository.update(SettingsCommand.SetVaultSortOption(sort)) }
    }
    fun selectTab(tab: VaultTab) = searchFilter.updateSelectedTab(tab)
    fun toggleSearch(active: Boolean) = searchFilter.toggleSearch(active)
    fun toggleShowTOTPCode() {
        _showTOTPCode.value = !_showTOTPCode.value
    }

    fun setAddType(type: AddType?) {
        _dialogState.value = _dialogState.value.copy(addType = type)
    }

    fun setItemToDelete(item: EntryListItem?) {
        _dialogState.value = _dialogState.value.copy(pendingDelete = item)
    }

    fun autoUnlockTotp(entryId: String) = totp.autoUnlock(entryId)

    init {
        totp.start()

        viewModelScope.launch {
            sessionStateProvider.lockStateFlow.collect { lockState ->
                totp.onSessionStateChanged(
                    unlocked = lockState == com.aozijx.passly.core.session.LockState.UNLOCKED
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.settings.map { it.vault.sort }.first().let {
                searchFilter.updateSelectedSort(it)
            }
        }

        viewModelScope.launch {
            vaultDataRefreshNotifier.events.collect {
                requestFullReload()
            }
        }
    }

    fun loadEntryById(entryId: String, onLoaded: (VaultEntry) -> Unit) =
        viewModelScope.launch { entryQueryRepository.getByIdWithoutHighSensitivity(entryId)?.let(onLoaded) }

    // --- 条目操作委托 ---
    fun addItem(entry: VaultEntry) =
        entryManager.addItem(entry, onComplete = { setAddType(null) })

    fun updateVaultEntry(entry: VaultEntry) = entryManager.updateEntry(entry)
    fun quickDelete(item: EntryListItem) = entryManager.deleteEntryById(item.id)
    fun confirmDelete() {
        val item = _dialogState.value.pendingDelete ?: return
        viewModelScope.launch {
            val entry = entryQueryRepository.getByIdWithoutHighSensitivity(item.id) ?: return@launch
            entryManager.deleteEntry(entry)
        }
    }

    override fun onCleared() {
        super.onCleared()
        totp.clearAllSensitiveState()
    }
}

private data class VaultDialogState(
    val addType: AddType? = null,
    val pendingDelete: EntryListItem? = null
)
