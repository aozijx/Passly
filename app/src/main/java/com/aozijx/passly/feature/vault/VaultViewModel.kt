package com.aozijx.passly.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.domain.entry.otp.OtpGenerator
import com.aozijx.passly.data.local.database.port.EntryDataRefreshNotifier
import com.aozijx.passly.runtime.session.SessionStateProvider
import com.aozijx.passly.domain.entry.model.EntryIdentity
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryTimestamps
import com.aozijx.passly.domain.entry.model.EntryIcon
import com.aozijx.passly.feature.vault.model.OtpUiState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.credential.OtpCredential
import com.github.f4b6a3.uuid.UuidCreator
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
import com.aozijx.passly.domain.entry.port.FaviconRepository
import com.aozijx.passly.domain.entry.port.OtpConfigRepository
import com.aozijx.passly.domain.entry.policy.EntryFieldReader
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.model.LibraryQuickFilter
import com.aozijx.passly.domain.settings.model.LibrarySortSpec
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import com.aozijx.passly.feature.vault.contract.VaultEffect
import com.aozijx.passly.feature.vault.contract.VaultIntent
import com.aozijx.passly.feature.vault.contract.VaultUiState
import com.aozijx.passly.feature.vault.entry.EntryManager
import com.aozijx.passly.feature.vault.list.VaultListCoordinator
import com.aozijx.passly.feature.vault.list.VaultQueryCoordinator
import com.aozijx.passly.feature.vault.model.AddType
import com.aozijx.passly.feature.vault.otp.TotpCoordinator
import com.aozijx.passly.feature.vault.presentation.VaultMutation
import com.aozijx.passly.feature.vault.presentation.VaultReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val dataRefreshNotifier: EntryDataRefreshNotifier,
    private val sessionStateProvider: SessionStateProvider,
    private val accessPolicy: SecureSessionAccessPolicy
) : ViewModel() {

    private val _effects = Channel<VaultEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private fun emitError(message: String) {
        _effects.trySend(VaultEffect.ShowError(message))
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
    private val entryManager = EntryManager(
        scope = viewModelScope,
        entryCommandRepository = entryCommandRepository,
        entryQueryRepository = entryQueryRepository,
        faviconRepository = faviconRepository,
        totp = totp,
        onError = { emitError(it) },
        onEntryDeleted = { deletedId ->
            mutate(VaultMutation.DeletedEntryHandled(deletedId))
        }
    )

    private val queryCoordinator = VaultQueryCoordinator(entryListQueryRepository)

    private val listCoordinator = VaultListCoordinator(
        scope = viewModelScope,
        queryCoordinator = queryCoordinator,
        uiState = uiState,
        refreshTrigger = _refreshTrigger
    )

    private fun addScannedOtp(config: OtpConfig) {
        if (!ensureFullSecureSessionAccess("恢复模式不能保存 OTP")) return
        try {
            val title = buildString {
                if (!config.issuer.isNullOrBlank()) append(config.issuer)
                if (!config.accountName.isNullOrBlank()) {
                    if (isNotEmpty()) append(": ")
                    append(config.accountName)
                }
                if (isEmpty()) append("TOTP")
            }
            val entry = Entry(
                identity = EntryIdentity(
                    id = EntryId(UuidCreator.getTimeOrderedEpoch().toString()),
                    type = EntryType.OTP,
                    version = EntryVersion.INITIAL,
                    timestamps = EntryTimestamps(System.currentTimeMillis()),
                ),
                profile = EntryProfile(
                    title = title,
                    username = config.accountName ?: title,
                    icon = EntryIcon(),
                ),
                secret = EntrySecret(credential = OtpCredential(config = config))
            )
            entryManager.addItem(entry)
        } catch (error: Exception) {
            AppTelemetry.e("SaveScannedOtp", "Failed to save scanned OTP entry", error)
            emitError("加密保存失败")
        }
    }

    /**
     * OTP 状态（高频率变化，每秒更新）。
     * 独立 Flow 避免 OTP 更新触发整个 UI 状态重组。
     */
    val totpStatesFlow: StateFlow<Map<String, OtpUiState>> = totp.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- onIntent 统一入口 ---
    fun onIntent(intent: VaultIntent) {
        when (intent) {
            is VaultIntent.SearchQueryChanged ->
                mutate(VaultMutation.SearchQueryChanged(intent.query))
            is VaultIntent.CategorySelected ->
                mutate(VaultMutation.CategoryChanged(intent.category))
            VaultIntent.ClearCategory -> mutate(VaultMutation.CategoryChanged(null))
            is VaultIntent.SortOptionSelected -> selectSortOption(intent.sort)
            is VaultIntent.QuickFilterSelected ->
                mutate(VaultMutation.QuickFilterChanged(intent.filter))
            is VaultIntent.SearchToggled ->
                mutate(VaultMutation.SearchVisibilityChanged(intent.active))
            VaultIntent.ToggleShowTotpCode -> toggleShowTOTPCode()
            is VaultIntent.AddTypeSelected -> setAddType(intent.type)
            is VaultIntent.ItemToDeleteSelected -> setItemToDelete(intent.item)
            VaultIntent.ConfirmDelete -> confirmDelete()
            is VaultIntent.QuickDelete -> quickDelete(intent.item)
            is VaultIntent.AddItem -> addItem(intent.entry)
            is VaultIntent.UpdateEntry -> updateEntry(intent.entry)
            is VaultIntent.AddScannedOtp -> addScannedOtp(intent.config)
            is VaultIntent.AutoUnlockTotp -> autoUnlockTotp(intent.entryId)
        }
    }

    private fun selectSortOption(sort: LibrarySortSpec) {
        mutate(VaultMutation.SortChanged(sort))
        viewModelScope.launch { settingsRepository.update(SettingsCommand.SetVaultSortOption(sort)) }
    }

    private fun toggleShowTOTPCode() {
        mutate(VaultMutation.TotpVisibilityToggled)
    }

    private fun setAddType(type: AddType?) {
        if (type != null && !ensureFullSecureSessionAccess("当前会话不能新建条目")) return
        mutate(VaultMutation.AddTypeChanged(type))
    }

    private fun setItemToDelete(item: EntryListItem?) {
        mutate(VaultMutation.PendingDeleteChanged(item))
    }

    private fun autoUnlockTotp(entryId: String) {
        if (accessPolicy.hasFullAccess()) {
            totp.autoUnlock(entryId)
        }
    }

    suspend fun loadEntryById(entryId: String): Entry? {
        if (!accessPolicy.hasFullAccess()) return null
        return entryQueryRepository.getById(EntryId(entryId))
    }

    private fun addItem(entry: Entry) {
        if (!ensureFullSecureSessionAccess("当前会话不能新建条目")) return
        entryManager.addItem(entry, onComplete = { setAddType(null) })
    }

    private fun updateEntry(entry: Entry) {
        if (!ensureFullSecureSessionAccess("当前会话不能修改条目")) return
        entryManager.updateEntry(entry)
    }

    private fun quickDelete(item: EntryListItem) {
        if (!ensureFullSecureSessionAccess("当前会话不能删除条目")) return
        entryManager.deleteEntryById(item.id.value)
    }

    private fun confirmDelete() {
        if (!ensureFullSecureSessionAccess("当前会话不能删除条目")) return
        val item = uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            if (!accessPolicy.hasFullAccess()) return@launch
            val entry = entryQueryRepository.getById(item.id) ?: return@launch
            entryManager.deleteEntry(entry)
        }
    }

    init {
        totp.start()

        viewModelScope.launch {
            sessionStateProvider.lockStateFlow.collect { lockState ->
                totp.onSessionStateChanged(
                    unlocked = lockState == com.aozijx.passly.runtime.session.SecureSessionState.UNLOCKED
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { it.vault.sort }
                .distinctUntilChanged()
                .collect { mutate(VaultMutation.SortChanged(it)) }
        }

        viewModelScope.launch {
            settingsRepository.settings
                .map { settings ->
                    val keys = settings.vault.visibleQuickFilters?.filterKeys
                        ?: LibraryQuickFilter.defaultVisibleKeys
                    LibraryQuickFilter.resolveVisible(keys)
                }
                .distinctUntilChanged()
                .collect { mutate(VaultMutation.VisibleQuickFiltersChanged(it)) }
        }

        viewModelScope.launch {
            listCoordinator.state.collect { state ->
                mutate(
                    VaultMutation.ListChanged(
                        isLoading = state.isLoading,
                        categories = state.categories,
                        itemsByQuickFilter = state.itemsByQuickFilter,
                    )
                )
            }
        }

        viewModelScope.launch {
            dataRefreshNotifier.events.collect {
                requestFullReload()
            }
        }
    }

    private fun ensureFullSecureSessionAccess(message: String): Boolean {
        if (accessPolicy.hasFullAccess()) return true
        emitError(message)
        mutate(VaultMutation.DialogsCleared)
        return false
    }

    private fun mutate(mutation: VaultMutation) {
        _uiState.value = VaultReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        super.onCleared()
        totp.clearAllSensitiveState()
    }
}
