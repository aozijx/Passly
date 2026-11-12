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
import com.aozijx.passly.domain.entry.port.EntryPagingRepository
import com.aozijx.passly.domain.settings.port.InteractionSettingsSource
import com.aozijx.passly.domain.settings.port.InterfaceSettingsRepository
import com.aozijx.passly.domain.settings.port.LibraryViewSettingsRepository
import com.aozijx.passly.feature.vault.otp.OtpCodeRuntimePort
import com.aozijx.passly.feature.vault.otp.OtpCodeState
import com.aozijx.passly.feature.vault.otp.presentation.OtpPresentation
import com.aozijx.passly.feature.vault.otp.presentation.OtpUiState
import com.aozijx.passly.feature.vault.otp.presentation.toOtpUiState
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VaultViewModel
    @Inject
    internal constructor(
        private val entryListQueryRepository: EntryListQueryRepository,
        private val entryPagingRepository: EntryPagingRepository,
        private val settingsRepository: LibraryViewSettingsRepository,
        interfaceSettingsRepository: InterfaceSettingsRepository,
        interactionSettingsSource: InteractionSettingsSource,
        private val entryCommandWorkflowFactory: VaultEntryCommandWorkflowFactory,
        private val otpRuntime: OtpCodeRuntimePort,
    ) : ViewModel() {
        private val pageEffects = PageEffects<VaultEffect>()
        val effects = pageEffects.effects

        private val _uiState = MutableStateFlow(VaultUiState())
        val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

        private val entryCommands =
            entryCommandWorkflowFactory.create(
                scope = viewModelScope,
                pageEffects = pageEffects,
                onOtpEntryRemoved = ::onOtpEntryRemoved,
            )

        private val librarySettingsRead =
            RecoverableRead(
                source = settingsRepository.libraryViewSettings,
                scope = viewModelScope,
            )

        private val displaySettingsRead =
            RecoverableRead(
                source =
                    combine(
                        interfaceSettingsRepository.interfaceSettings,
                        librarySettingsRead.state,
                        interactionSettingsSource.interaction,
                        ::VaultSettingsSnapshot,
                    ),
                scope = viewModelScope,
            )

        val displayState: StateFlow<VaultDisplayUiState> =
            displaySettingsRead.state
                .map { it.toDisplayUiState() }
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), VaultDisplayUiState.Loading)

        private val categoriesRead =
            RecoverableRead(
                source = entryListQueryRepository.availableCategories,
                scope = viewModelScope,
            )

        private val queryState: Flow<VaultQueryGeneration> =
            buildRecoverableVaultQueryGenerations(
                uiStates = uiState,
                librarySettings = librarySettingsRead.state,
            )

        val entries: Flow<PagingData<VaultListItemUiModel>> =
            queryState
                .switchRecoverableQueryGenerations(
                    unavailable = flowOf(PagingData.empty<EntryListItem>()),
                    load = { query -> entryPagingRepository.pages(query, ENTRY_PAGING_CONFIG) },
                ).map { pagingData -> pagingData.map(EntryListItem::toUiModel) }
                .cachedIn(viewModelScope)

        /** Runtime-owned OTP codes stay separate from the page state to avoid refreshing Paging UI. */
        val otpStates: StateFlow<Map<String, OtpCodeState>> = otpRuntime.states

        internal fun otpUiStateFlow(
            entryId: String,
            otpPresentation: OtpPresentation,
            showCode: Boolean,
        ): StateFlow<OtpUiState> {
            val initialState = otpStates.value[entryId].toOtpUiState(otpPresentation, showCode)
            if (otpPresentation !is OtpPresentation.Applicable || !showCode) {
                return MutableStateFlow(initialState)
            }
            return flow {
                val lease = otpRuntime.acquire(entryId)
                try {
                    emitAll(
                        otpStates
                            .map { states -> states[entryId].toOtpUiState(otpPresentation, showCode) }
                            .distinctUntilChanged(),
                    )
                } finally {
                    lease.close()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
                initialValue = initialState,
            )
        }

        // --- onAction 统一入口 ---
        fun onAction(action: VaultUiAction) {
            when (action) {
                is VaultUiAction.SearchQueryChanged -> {
                    _uiState.update { state ->
                        when (val search = state.search) {
                            VaultSearchInput.Closed -> state
                            is VaultSearchInput.Open -> state.copy(search = search.copy(query = action.query))
                        }
                    }
                }

                is VaultUiAction.CategorySelectionChanged -> {
                    _uiState.update { it.copy(categorySelection = action.selection) }
                }

                is VaultUiAction.CategorySearchQueryChanged -> {
                    _uiState.update { it.copy(categorySearchQuery = action.query) }
                }

                VaultUiAction.RetryCategories -> {
                    categoriesRead.retry()
                }

                is VaultUiAction.SortOptionSelected -> {
                    selectSortOption(action.option)
                }

                is VaultUiAction.FilterToggled -> {
                    _uiState.update { state ->
                        val filters = state.selectedFilters
                        state.copy(
                            selectedFilters =
                                if (action.filter in filters) {
                                    filters - action.filter
                                } else {
                                    filters + action.filter
                                },
                        )
                    }
                }

                VaultUiAction.ClearFilters -> {
                    _uiState.update { it.copy(selectedFilters = emptySet()) }
                }

                is VaultUiAction.SearchToggled -> {
                    _uiState.update { state ->
                        state.copy(
                            search =
                                if (action.active) {
                                    when (val current = state.search) {
                                        VaultSearchInput.Closed -> VaultSearchInput.Open("")
                                        is VaultSearchInput.Open -> current
                                    }
                                } else {
                                    VaultSearchInput.Closed
                                },
                        )
                    }
                }

                VaultUiAction.ToggleShowTotpCode -> {
                    _uiState.update { it.copy(showTotpCode = !it.showTotpCode) }
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

        private fun selectSortOption(option: VaultSortOption) {
            val currentSort =
                (librarySettingsRead.state.value as? RecoverableReadState.Ready)?.value?.sort ?: return
            val requestedSort =
                if (currentSort.field == option.field) {
                    currentSort.toggled()
                } else {
                    EntrySort.presets().single { it.field == option.field }
                }
            val owner = pageEffects.currentOwner ?: return
            viewModelScope.launch {
                writePreferenceWithFeedback(
                    failureRes = R.string.vault_error_sort_failed,
                    onFailure = { emitError(owner, it) },
                ) { settingsRepository.setSort(requestedSort) }
            }
        }

        init {
            viewModelScope.launch {
                categoriesRead.state.collect { readState ->
                    val categories =
                        when (readState) {
                            RecoverableReadState.Loading -> {
                                VaultCategoriesState.Loading
                            }

                            is RecoverableReadState.Ready -> {
                                readState.value
                                    .takeIf { it.isNotEmpty() }
                                    ?.let(VaultCategoriesState::Ready)
                                    ?: VaultCategoriesState.ReadyEmpty
                            }

                            is RecoverableReadState.Failed -> {
                                VaultCategoriesState.Failed
                            }
                        }
                    _uiState.update { it.copy(categories = categories) }
                }
            }
        }

        fun onPageEntered(): Any = pageEffects.enter()

        fun onPageExited(owner: Any) {
            pageEffects.exit(owner)
        }

        fun retrySettings() {
            if (librarySettingsRead.state.value is RecoverableReadState.Failed) {
                librarySettingsRead.retry()
            }
            if (displaySettingsRead.state.value is RecoverableReadState.Failed) {
                displaySettingsRead.retry()
            }
        }

        private fun emitError(
            owner: Any,
            message: UiErrorMessage,
        ) {
            pageEffects.tryEmit(owner, VaultEffect.ShowError(message))
        }

        private fun onOtpEntryRemoved(entryId: String) {
            otpRuntime.entryRemoved(entryId)
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
