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
import com.aozijx.passly.domain.entry.model.WebsiteInfo
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpType
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
import com.aozijx.passly.feature.vault.model.OtpFormState
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.feature.vault.model.VaultTab
import com.aozijx.passly.feature.vault.otp.TotpCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // --- OTP 表单状态（添加/编辑） ---
    private val _otpFormState = MutableStateFlow(OtpFormState())
    val otpFormState: StateFlow<OtpFormState> = _otpFormState.asStateFlow()

    fun setOtpFormState(state: OtpFormState) {
        _otpFormState.value = state
    }

    fun initOtpFormForEdit(entry: VaultEntry) {
        _otpFormState.value = OtpFormState.fromEntry(entry)
    }

    fun resetOtpForm() {
        _otpFormState.value = OtpFormState()
    }

    fun updateOtpType(type: OtpType) {
        _otpFormState.value = _otpFormState.value.let { current ->
            current.copy(
                type = type,
                digits = when (type) {
                    OtpType.STEAM -> "5"
                    OtpType.TOTP -> if (current.digits == "5") "6" else current.digits
                    OtpType.HOTP -> current.digits
                },
                period = when (type) {
                    OtpType.STEAM -> "30"
                    OtpType.TOTP -> current.period.ifBlank { "30" }
                    OtpType.HOTP -> current.period
                },
                algorithm = when (type) {
                    OtpType.STEAM -> "SHA1"
                    OtpType.HOTP -> current.algorithm
                    OtpType.TOTP -> current.algorithm
                },
                counter = when (type) {
                    OtpType.HOTP -> if (current.counter.isBlank()) "0" else current.counter
                    else -> current.counter
                }
            )
        }
    }

    /**
     * 保存 OTP 条目。
     *
     * @param originalEntry 编辑模式下需要传入原始条目以构造更新版本
     */
    fun saveOtpEntry(originalEntry: VaultEntry? = null) {
        val state = _otpFormState.value
        try {
            when (state.mode) {
                is OtpFormState.Mode.Add -> {
                    val entry = buildOtpEntry(state)
                    addItem(entry, state.domain)
                }

                is OtpFormState.Mode.Edit -> {
                    val entry = originalEntry ?: return
                    val updatedEntry = buildUpdatedEntry(entry, state)
                    updateVaultEntry(updatedEntry)
                }
            }
        } catch (e: Exception) {
            AppTelemetry.e("SaveOtpEntry", "Failed to save OTP entry", e)
            emitError("加密保存失败")
        }
    }

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
                    entryType = EntryType.LOGIN,
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

    private fun buildOtpEntry(state: OtpFormState): VaultEntry = VaultEntry(
        header = EntryHeader(
            id = EntryId(""),
            entryType = EntryType.LOGIN,
            version = EntryVersion.INITIAL,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ),
        summary = EntrySummary(
            title = state.title,
            username = state.username,
            icon = null,
            website = state.domain.ifBlank { null }
                ?.let { WebsiteInfo(primaryUrl = it) }
        ),
        secret = EntrySecret(
            otp = OtpSecret(
                config = buildOtpConfig(state)
            )
        )
    )

    private fun buildUpdatedEntry(entry: VaultEntry, state: OtpFormState): VaultEntry {
        val newOtp = OtpSecret(
            config = buildOtpConfig(state)
        )
        return entry.copy(
            summary = entry.summary.copy(
                title = state.title,
                username = state.username.ifBlank { entry.summary.username }
            ),
            secret = entry.secret.copy(otp = newOtp)
        )
    }

    private fun buildOtpConfig(state: OtpFormState): OtpConfig = OtpConfig(
        type = state.type,
        secret = state.secret.trim(),
        digits = if (state.type == OtpType.STEAM) 5
        else (state.digits.toIntOrNull() ?: 6),
        periodSeconds = if (state.type == OtpType.HOTP) null
        else (state.period.toIntOrNull() ?: 30),
        counter = if (state.type == OtpType.HOTP) {
            state.counter.toLongOrNull() ?: 0L
        } else null,
        algorithm = when (state.algorithm.uppercase()) {
            "SHA256" -> OtpHashAlgorithm.SHA256
            "SHA512" -> OtpHashAlgorithm.SHA512
            else -> OtpHashAlgorithm.SHA1
        },
        encoding = state.encoding,
        issuer = state.issuer.ifBlank { null },
        accountName = state.username.ifBlank { null }
    )

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
            selectedCategory = search.selectedCategory,
            selectedTab = search.selectedTab,
            selectedSort = search.selectedSort,
            isSearchActive = search.isSearchActive,
            isMoreMenuExpanded = search.isMoreMenuExpanded,
            isVaultItemsLoading = list.isLoading,
            availableCategories = list.categories,
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
    fun expandMoreMenu(expanded: Boolean) = searchFilter.expandMoreMenu(expanded)
    fun onSearchQueryChange(q: String) = searchFilter.updateSearchQuery(q)
    fun setSelectedCategory(category: String?) = searchFilter.updateSelectedCategory(category)
    fun clearSelectedCategory() = setSelectedCategory(null)
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
        viewModelScope.launch { entryQueryRepository.getById(entryId)?.let(onLoaded) }

    // --- 条目操作委托 ---
    fun addItem(entry: VaultEntry) =
        entryManager.addItem(entry, onComplete = { setAddType(null) })

    fun addItem(entry: VaultEntry, domain: String) =
        entryManager.addItem(entry, domain, onComplete = { setAddType(null) })
    fun updateVaultEntry(entry: VaultEntry) = entryManager.updateEntry(entry)
    fun quickDelete(item: EntryListItem) = entryManager.deleteEntryById(item.id)
    fun confirmDelete() {
        val item = _dialogState.value.pendingDelete ?: return
        viewModelScope.launch {
            val entry = entryQueryRepository.getById(item.id) ?: return@launch
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
