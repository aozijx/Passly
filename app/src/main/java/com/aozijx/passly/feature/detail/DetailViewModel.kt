package com.aozijx.passly.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.EntryActivity
import com.aozijx.passly.domain.model.entry.EntryChanges
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.favicon.FaviconOutcome
import com.aozijx.passly.domain.model.favicon.FaviconResult
import com.aozijx.passly.domain.repository.entry.EntryCommands
import com.aozijx.passly.domain.repository.favicon.FaviconRepository
import com.aozijx.passly.domain.strategy.EntryTypeStrategyProvider
import com.aozijx.passly.domain.usecase.detail.DetailQueryUseCases
import com.aozijx.passly.domain.usecase.settings.RuntimeSettingsUseCases
import com.aozijx.passly.feature.detail.contract.DetailEffect
import com.aozijx.passly.feature.detail.contract.DetailIntent
import com.aozijx.passly.feature.detail.contract.DetailUiState
import com.aozijx.passly.feature.detail.page.internal.DetailEntryAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val detailQueryUseCases: DetailQueryUseCases,
    private val entryCommandHandler: EntryCommands,
    private val runtimeSettingsUseCases: RuntimeSettingsUseCases,
    private val faviconRepository: FaviconRepository,
    private val strategyProvider: EntryTypeStrategyProvider
) : ViewModel() {
    private val entryAnalyzer = DetailEntryAnalyzer(strategyProvider)

    companion object {
        private const val ACCESS_HISTORY_TOGGLE_KEY = "detail.access_history_enabled"
    }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<DetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<DetailEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            runtimeSettingsUseCases.userConfigExtras.collect { extras ->
                val enabled = extras[ACCESS_HISTORY_TOGGLE_KEY]
                    ?.toBooleanStrictOrNull()
                    ?: false
                _uiState.update { it.copy(isAccessHistoryEnabled = enabled) }
            }
        }
    }

    fun handleIntent(event: DetailIntent) {
        when (event) {
            is DetailIntent.Initialize -> {
                refreshFromEntry(event.initialEntry, isEditingTitle = false, editedTitle = event.initialEntry.title)

                viewModelScope.launch {
                    val latest =
                        detailQueryUseCases.getById(event.initialEntry.id) ?: event.initialEntry
                    refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
                    autoDownloadFavicon(latest)
                }

                viewModelScope.launch {
                    detailQueryUseCases.getActivityByEntryId(event.initialEntry.id)
                        .collect { list: List<EntryActivity> ->
                            _uiState.update { it.copy(history = list) }
                        }
                }
            }

            is DetailIntent.SyncEntry -> {
                val editedTitle = if (_uiState.value.isEditingTitle) _uiState.value.editedTitle else event.entry.title
                refreshFromEntry(event.entry, _uiState.value.isEditingTitle, editedTitle)
            }

            is DetailIntent.CommitEntryUpdate -> {
                val editedTitle = if (_uiState.value.isEditingTitle) {
                    _uiState.value.editedTitle
                } else {
                    event.entry.title
                }
                refreshFromEntry(event.entry, _uiState.value.isEditingTitle, editedTitle)
                emitEntryUpdated(event.entry)
            }

            DetailIntent.ShowIconPicker -> {
                _effects.tryEmit(DetailEffect.IconPickerRequested)
            }

            DetailIntent.StartTitleEdit -> {
                _uiState.update {
                    val currentTitle = it.entry?.title.orEmpty()
                    it.copy(isEditingTitle = true, editedTitle = currentTitle)
                }
            }

            DetailIntent.CancelTitleEdit -> {
                _uiState.update {
                    it.copy(
                        isEditingTitle = false,
                        editedTitle = it.entry?.title.orEmpty()
                    )
                }
            }

            is DetailIntent.UpdateEditedTitle -> {
                _uiState.update { it.copy(editedTitle = event.value) }
            }

            DetailIntent.SaveTitle -> {
                val state = _uiState.value
                val current = state.entry ?: return
                val newTitle = state.editedTitle.trim()
                if (newTitle.isBlank() || newTitle == current.title) {
                    _uiState.update {
                        it.copy(
                            isEditingTitle = false,
                            editedTitle = current.title
                        )
                    }
                } else {
                    commitEntryUpdate(
                        current.copy(summary = current.summary.copy(title = newTitle)),
                        isEditingTitle = false
                    )
                }
            }

            DetailIntent.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return
                commitEntryUpdate(current.copy(summary = current.summary.copy(favorite = !current.favorite)))
            }

            is DetailIntent.RevealField -> {
                _uiState.update { state ->
                    val updated = state.revealedFields.toMutableMap()
                    if (event.value != null) updated[event.key] = event.value
                    else updated.remove(event.key)
                    state.copy(revealedFields = updated)
                }
            }

            is DetailIntent.RecordAction -> {
                val current = _uiState.value.entry ?: return
                if (event.type == ActivityType.VIEW && !_uiState.value.isAccessHistoryEnabled) return
                if (event.type == ActivityType.COPY_PASSWORD || event.type == ActivityType.COPY_USERNAME) {
                    _uiState.update { it.copy(revealedFields = emptyMap()) }
                }

                viewModelScope.launch {
                    entryCommandHandler.recordUsage(current.id, event.type)
                }
            }

            is DetailIntent.ToggleAccessHistoryRecording -> {
                _uiState.update { it.copy(isAccessHistoryEnabled = event.enabled) }
                viewModelScope.launch {
                    runtimeSettingsUseCases.setUserConfigExtra(ACCESS_HISTORY_TOGGLE_KEY, event.enabled.toString())
                }
            }

            DetailIntent.ClearSensitiveState -> {
                _uiState.update { DetailUiState() }
            }
        }
    }

    private fun commitEntryUpdate(entry: VaultEntry, isEditingTitle: Boolean = _uiState.value.isEditingTitle) {
        val editedTitle = if (isEditingTitle) _uiState.value.editedTitle else entry.title
        refreshFromEntry(entry, isEditingTitle = isEditingTitle, editedTitle = editedTitle)
        emitEntryUpdated(entry)
    }

    private fun emitEntryUpdated(entry: VaultEntry) {
        _effects.tryEmit(DetailEffect.EntryUpdated(entry))
    }

    private fun autoDownloadFavicon(entry: VaultEntry) {
        if (entry.associatedDomain.isNullOrBlank() || !entry.iconCustomPath.isNullOrBlank()) return
        viewModelScope.launch {
            val domain = entry.associatedDomain
            val outcome = downloadFavicon(domain!!)
            if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                val iconSummary = entry.summary.copy(icon = outcome.filePath)
                entryCommandHandler.updateEntry(
                    entry.id, entry.entryVersion, EntryChanges(summary = iconSummary)
                ).onSuccess {
                        refreshFromEntry(
                            entry.copy(summary = entry.summary.copy(icon = outcome.filePath)),
                            _uiState.value.isEditingTitle,
                            _uiState.value.editedTitle
                        )
                    }
            }
        }
    }

    private suspend fun downloadFavicon(input: String): FaviconOutcome {
        if (input.isBlank()) return FaviconOutcome(FaviconResult.EMPTY_INPUT)
        return faviconRepository.download(input)
    }

    private fun refreshFromEntry(entry: VaultEntry, isEditingTitle: Boolean, editedTitle: String) {
        val analysis = entryAnalyzer.analyze(entry)

        _uiState.update {
            it.copy(
                entry = entry,
                vaultType = analysis.vaultType,
                strategySummary = analysis.strategySummary,
                validationError = analysis.validationError,
                isEditingTitle = isEditingTitle,
                editedTitle = editedTitle,
                strategyReady = analysis.strategyReady
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.update { DetailUiState() }
    }
}
