package com.aozijx.passly.feature.vault.presentation.list

import androidx.lifecycle.ViewModel
import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.query.EntryHierarchyDisplayMode
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldResult
import com.aozijx.passly.feature.vault.entry.CopyEntryFieldUseCase
import com.aozijx.passly.feature.vault.entry.CopyOtpCodeUseCase
import com.aozijx.passly.feature.vault.trash.TrashCommandResult
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import com.aozijx.passly.feature.vault.entry.VaultDataChangeSignal
import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject internal constructor(
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryPageSource: VaultEntryPageSource,
    private val settingsRepository: LibraryViewSettingsRepository,
    private val trashCommand: TrashCommandUseCase,
    private val copyEntryField: CopyEntryFieldUseCase,
    private val copyOtpCode: CopyOtpCodeUseCase,
    private val dataChangeSignal: VaultDataChangeSignal,
    private val secureSessionAccessState: SecureSessionAccessState,
    otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
) : ViewModel() {

    private val pageEffects = PageEffects<VaultEffect>()
    val effects = pageEffects.effects

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private fun emitError(message: UiErrorMessage) {
        emitEffect(VaultEffect.ShowError(message))
    }

    private val _refreshTrigger = MutableStateFlow(0L)
    private val deletingEntryIds = mutableSetOf<EntryId>()

    /**
     * 外部替换数据库内容后重建 Room 订阅。
     * 普通增删改依靠 Room 的失效通知，不应手动触发重复查询。
     */
    private fun requestFullReload() {
        _refreshTrigger.value++
    }

    private val totp = otpCodeRuntimeFactory.create(viewModelScope)

    private val hierarchyMode: Flow<EntryHierarchyDisplayMode> =
        settingsRepository.libraryViewSettings
            .map { settings -> settings.entryHierarchyDisplayMode }
            .distinctUntilChanged()

    private val queryState: Flow<VaultQueryState> = buildVaultQueryStates(
        uiStates = uiState,
        hierarchyModes = hierarchyMode,
        reloadVersions = _refreshTrigger,
    )

    val entries: Flow<PagingData<VaultListItemUiModel>> = queryState
        .switchQueryGenerations { query -> entryPageSource.pages(query, ENTRY_PAGING_CONFIG) }
        .map { pagingData -> pagingData.map(EntryListItem::toUiModel) }
        .cachedIn(viewModelScope)

    /**
     * OTP 状态（高频率变化，每秒更新）。
     * 独立 Flow 避免 OTP 更新触发整个 UI 状态重组。
     */
    val totpStatesFlow: StateFlow<Map<String, OtpCodeState>> = totp.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- onAction 统一入口 ---
    fun onAction(action: VaultUiAction) {
        when (action) {
            is VaultUiAction.SearchQueryChanged ->
                mutate(VaultMutation.SearchQueryChanged(action.query))

            is VaultUiAction.CategorySelected ->
                mutate(VaultMutation.CategoryChanged(action.category))

            VaultUiAction.ClearCategory -> mutate(VaultMutation.CategoryChanged(null))
            is VaultUiAction.SortOptionSelected -> selectSortOption(action.sort)
            is VaultUiAction.FilterToggled -> mutate(VaultMutation.FilterToggled(action.filter))

            is VaultUiAction.SearchToggled ->
                mutate(VaultMutation.SearchVisibilityChanged(action.active))

            VaultUiAction.ToggleShowTotpCode -> toggleShowTOTPCode()
            is VaultUiAction.ItemToDeleteSelected -> setItemToDelete(action.item)
            VaultUiAction.ConfirmDelete -> confirmDelete()
            is VaultUiAction.QuickDelete -> quickDelete(action.entryId)
            is VaultUiAction.CopyField -> copyField(action.entryId, action.entryType, action.fieldKey)
            is VaultUiAction.CopyOtp -> copyOtp(action.entryId)
        }
    }

    private fun selectSortOption(sort: EntrySort) {
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.vault_error_sort_failed,
                onFailure = { emitError(owner, it) },
            ) { settingsRepository.setSort(sort) }
        }
    }

    private fun toggleShowTOTPCode() {
        mutate(VaultMutation.TotpVisibilityToggled)
    }

    private fun setItemToDelete(item: EntryListItem?) {
        mutate(VaultMutation.PendingDeleteChanged(item))
    }

    fun subscribeVisibleOtp(entryId: String) {
        totp.subscribe(entryId)
    }

    fun unsubscribeVisibleOtp(entryId: String) {
        totp.unsubscribe(entryId)
    }

    private fun quickDelete(entryId: String) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_delete_access)) return
        moveToTrash(EntryId(entryId))
    }

    private fun confirmDelete() {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_delete_access)) return
        val item = uiState.value.pendingDelete ?: return
        moveToTrash(item.id)
    }

    private fun copyField(
        entryId: String,
        entryType: EntryType,
        fieldKey: FieldKey,
    ) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_copy_access)) return
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            when (copyEntryField(EntryId(entryId), entryType, fieldKey)) {
                CopyEntryFieldResult.Copied -> pageEffects.send(owner, VaultEffect.FieldCopied(fieldKey))
                CopyEntryFieldResult.NotAuthorized,
                CopyEntryFieldResult.Unavailable,
                -> Unit
            }
        }
    }

    private fun copyOtp(entryId: String) {
        if (!ensureFullSecureSessionAccess(R.string.vault_error_copy_otp_access)) return
        val owner = pageEffects.currentOwner ?: return
        viewModelScope.launch {
            when (
                copyOtpCode(EntryId(entryId)) {
                    (totp.states.value[entryId] as? OtpCodeState.Ready)?.code
                }
            ) {
                CopyEntryFieldResult.Copied -> pageEffects.send(owner, VaultEffect.OtpCopied)
                CopyEntryFieldResult.NotAuthorized,
                CopyEntryFieldResult.Unavailable,
                -> Unit
            }
        }
    }

    private fun moveToTrash(entryId: EntryId) {
        val owner = pageEffects.currentOwner ?: return
        if (!deletingEntryIds.add(entryId)) return
        viewModelScope.launch {
            try {
                when (val result = trashCommand.moveToTrash(entryId)) {
                    TrashCommandResult.Completed -> {
                        totp.entryRemoved(entryId.value)
                        if (pageEffects.isActive(owner)) {
                            mutate(VaultMutation.DeletedEntryHandled(entryId.value))
                        }
                    }

                    TrashCommandResult.NotAuthorized -> Unit
                    is TrashCommandResult.Failed -> emitError(
                        owner,
                        result.error.toUiErrorMessage(R.string.vault_error_trash_failed),
                    )
                }
            } finally {
                deletingEntryIds.remove(entryId)
            }
        }
    }

    init {
        viewModelScope.launch {
            settingsRepository.libraryViewSettings
                .map { it.sort }
                .distinctUntilChanged()
                .collect { mutate(VaultMutation.SortChanged(it)) }
        }

        viewModelScope.launch {
            entryListQueryRepository.availableCategories.collect { categories ->
                mutate(VaultMutation.CategoriesChanged(categories))
            }
        }

        viewModelScope.launch {
            dataChangeSignal.changes().collect {
                requestFullReload()
            }
        }
    }

    fun onPageEntered(): Any {
        return pageEffects.enter()
    }

    fun onPageExited(owner: Any) {
        pageEffects.exit(owner)
    }

    private fun emitEffect(effect: VaultEffect) {
        pageEffects.tryEmit(effect)
    }

    private fun emitError(owner: Any, message: UiErrorMessage) {
        pageEffects.tryEmit(owner, VaultEffect.ShowError(message))
    }

    private fun ensureFullSecureSessionAccess(@StringRes messageRes: Int): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        emitError(UiErrorMessage(messageRes))
        mutate(VaultMutation.DialogsCleared)
        return false
    }

    private fun mutate(mutation: VaultMutation) {
        _uiState.value = VaultReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        totp.clearAllSensitiveState()
    }

    private companion object {
        val ENTRY_PAGING_CONFIG = PagingConfig(
            pageSize = 30,
            initialLoadSize = 60,
            prefetchDistance = 10,
            enablePlaceholders = false,
            maxSize = 180,
        )
    }
}
