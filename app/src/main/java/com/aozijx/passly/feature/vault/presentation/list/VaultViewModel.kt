package com.aozijx.passly.feature.vault.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.aozijx.passly.R
import com.aozijx.passly.app.settings.RecoverableRead
import com.aozijx.passly.app.settings.RecoverableReadState
import com.aozijx.passly.domain.entry.model.query.EntryListItem
import com.aozijx.passly.domain.entry.model.query.EntrySort
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.settings.model.LibraryViewSettings
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.feature.vault.entry.VaultEntryPageSource
import com.aozijx.passly.feature.vault.model.OtpCodeState
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimeFactory
import com.aozijx.passly.feature.vault.presentation.list.model.VaultListItemUiModel
import com.aozijx.passly.presentation.shared.error.UiErrorMessage
import com.aozijx.passly.presentation.shared.error.toUiErrorMessage
import com.aozijx.passly.presentation.shared.error.writePreferenceWithFeedback
import com.aozijx.passly.presentation.shared.event.PageEffects
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VaultViewModel
    @Inject
    internal constructor(
        private val entryListQueryRepository: EntryListQueryRepository,
        private val entryPageSource: VaultEntryPageSource,
        private val settingsRepository: LibraryViewSettingsRepository,
        private val entryCommandWorkflowFactory: VaultEntryCommandWorkflowFactory,
        otpCodeRuntimeFactory: OtpCodeRuntimeFactory,
    ) : ViewModel() {
        private val pageEffects = PageEffects<VaultEffect>()
        val effects = pageEffects.effects

        private val _uiState = MutableStateFlow(VaultUiState())
        val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

        private val totp = otpCodeRuntimeFactory.create(viewModelScope)

        private val entryCommands =
            entryCommandWorkflowFactory.create(
                scope = viewModelScope,
                otpRuntime = totp,
                pageEffects = pageEffects,
            )

        private val librarySettingsRead =
            RecoverableRead(
                source = settingsRepository.libraryViewSettings,
                scope = viewModelScope,
            )

        val librarySettingsState = librarySettingsRead.state

        private val categoriesRead =
            RecoverableRead(
                source = entryListQueryRepository.availableCategories,
                scope = viewModelScope,
            )

        private val queryState: Flow<VaultQueryState?> =
            buildRecoverableVaultQueryStates(
                uiStates = uiState,
                librarySettings = librarySettingsState,
            )

        val entries: Flow<PagingData<VaultListItemUiModel>> =
            queryState
                .switchRecoverableQueryGenerations(
                    unavailable = flowOf(PagingData.empty<EntryListItem>()),
                    load = { query -> entryPageSource.pages(query, ENTRY_PAGING_CONFIG) },
                ).map { pagingData -> pagingData.map(EntryListItem::toUiModel) }
                .cachedIn(viewModelScope)

        /**
         * OTP 状态（高频率变化，每秒更新）。
         * 独立 Flow 避免 OTP 更新触发整个 UI 状态重组。
         */
        val totpStatesFlow: StateFlow<Map<String, OtpCodeState>> =
            totp.states
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        // --- onAction 统一入口 ---
        fun onAction(action: VaultUiAction) {
            when (action) {
                is VaultUiAction.SearchQueryChanged -> {
                    mutate(VaultMutation.SearchQueryChanged(action.query))
                }

                is VaultUiAction.CategorySelected -> {
                    mutate(VaultMutation.CategoryChanged(action.category))
                }

                is VaultUiAction.CategorySearchQueryChanged -> {
                    mutate(VaultMutation.CategorySearchQueryChanged(action.query))
                }

                VaultUiAction.ClearCategory -> {
                    mutate(VaultMutation.CategoryChanged(null))
                }

                VaultUiAction.RetryCategories -> {
                    categoriesRead.retry()
                }

                is VaultUiAction.SortOptionSelected -> {
                    selectSortOption(action.sort)
                }

                is VaultUiAction.FilterToggled -> {
                    mutate(VaultMutation.FilterToggled(action.filter))
                }

                is VaultUiAction.SearchToggled -> {
                    mutate(VaultMutation.SearchVisibilityChanged(action.active))
                }

                VaultUiAction.ToggleShowTotpCode -> {
                    toggleShowTOTPCode()
                }

                is VaultUiAction.QuickDelete -> {
                    entryCommands.quickDelete(action.entryId)
                }

                is VaultUiAction.CopyField -> {
                    entryCommands.copyField(action.entryId, action.entryType, action.fieldKey)
                }

                is VaultUiAction.CopyOtp -> {
                    entryCommands.copyOtp(action.entryId)
                }
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

        fun subscribeVisibleOtp(entryId: String) {
            totp.subscribe(entryId)
        }

        fun unsubscribeVisibleOtp(entryId: String) {
            totp.unsubscribe(entryId)
        }

        fun refreshVisibleOtp() {
            totp.refreshRequestedEntries()
        }

        init {
            viewModelScope.launch {
                librarySettingsState
                    .mapNotNull { state ->
                        when (state) {
                            is RecoverableReadState.Ready -> state.value.sort
                            else -> null
                        }
                    }.distinctUntilChanged()
                    .collect { mutate(VaultMutation.SortChanged(it)) }
            }

            viewModelScope.launch {
                categoriesRead.state.collect { readState ->
                    val categories =
                        when (readState) {
                            RecoverableReadState.Loading -> VaultCategoriesState.Loading
                            is RecoverableReadState.Ready ->
                                readState.value
                                    .takeIf { it.isNotEmpty() }
                                    ?.let(VaultCategoriesState::Ready)
                                    ?: VaultCategoriesState.ReadyEmpty
                            is RecoverableReadState.Failed -> VaultCategoriesState.Failed
                        }
                    mutate(VaultMutation.CategoriesStateChanged(categories))
                }
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }

        fun retryLibrarySettings() = librarySettingsRead.retry()

        private fun emitError(
            owner: Any,
            message: UiErrorMessage,
        ) {
            pageEffects.tryEmit(owner, VaultEffect.ShowError(message))
        }

        private fun mutate(mutation: VaultMutation) {
            _uiState.value = VaultReducer.reduce(_uiState.value, mutation)
        }

        override fun onCleared() {
            totp.clearAllSensitiveState()
        }

        private companion object {
            val ENTRY_PAGING_CONFIG =
                PagingConfig(
                    pageSize = 30,
                    initialLoadSize = 60,
                    prefetchDistance = 10,
                    enablePlaceholders = false,
                    maxSize = 180,
                )
        }
    }
