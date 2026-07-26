package com.aozijx.passly.feature.vault

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.otp.OtpGenerator
import com.aozijx.passly.core.platform.VaultDataRefreshNotifier
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
import com.aozijx.passly.feature.vault.internal.DetailCoordinator
import com.aozijx.passly.feature.vault.internal.EntryIconHelper
import com.aozijx.passly.feature.vault.internal.EntryManager
import com.aozijx.passly.feature.vault.internal.SearchFilterState
import com.aozijx.passly.feature.vault.internal.TotpCoordinator
import com.aozijx.passly.feature.vault.internal.VaultListCoordinator
import com.aozijx.passly.feature.vault.internal.VaultQueryCoordinator
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.model.OtpFormState
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.feature.vault.model.VaultTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val settingsRepository: AppSettingsRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val faviconRepository: FaviconRepository,
    val entryFieldReader: EntryFieldReader,
    private val vaultDataRefreshNotifier: VaultDataRefreshNotifier
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
        settingsRepository.settings.map { it.interaction.isAutoDownloadIcons }
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
                    setAddType(null)
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
        viewModelScope.launch { settingsRepository.update(SettingsCommand.SetVaultSortOption(sort)) }
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

    init {
        totp.start()

        viewModelScope.launch {
            settingsRepository.settings.map { it.vault.sort }.first().let {
                searchFilter.updateSelectedSort(it)
            }
        }

        viewModelScope.launch {
            vaultDataRefreshNotifier.events.collect {
                refreshItems()
            }
        }
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
