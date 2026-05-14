package com.aozijx.passly.features.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import com.aozijx.passly.domain.usecase.detail.DetailUseCases
import com.aozijx.passly.domain.usecase.userconfig.UserConfigUseCases
import com.aozijx.passly.features.detail.contract.DetailEffect
import com.aozijx.passly.features.detail.contract.DetailEvent
import com.aozijx.passly.features.detail.contract.DetailUiState
import com.aozijx.passly.features.detail.page.internal.DetailEntryAnalyzer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    application: Application,
    private val detailUseCases: DetailUseCases,
    private val userConfigUseCases: UserConfigUseCases
) : AndroidViewModel(application) {
    private val entryAnalyzer = DetailEntryAnalyzer()

    companion object {
        private const val ACCESS_HISTORY_TOGGLE_KEY = "detail.access_history_enabled"
    }

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<DetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<DetailEffect> = _effects.asSharedFlow()

    init {
        EntryTypeStrategyRegistry.ensureRegistered()
        viewModelScope.launch {
            userConfigUseCases.userConfig.collect { config ->
                val enabled = config.extras[ACCESS_HISTORY_TOGGLE_KEY]
                    ?.toBooleanStrictOrNull()
                    ?: false
                _uiState.update { it.copy(isAccessHistoryEnabled = enabled) }
            }
        }
    }

    fun onEvent(event: DetailEvent) {
        when (event) {
            is DetailEvent.Initialize -> {
                refreshFromEntry(event.initialEntry, isEditingTitle = false, editedTitle = event.initialEntry.title)

                viewModelScope.launch {
                    val latest = detailUseCases.getEntryById(event.initialEntry.id) ?: event.initialEntry
                    refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
                    autoDownloadFavicon(latest)
                }

                viewModelScope.launch {
                    detailUseCases.getHistoryByEntryId(event.initialEntry.id)
                        .collect { list: List<VaultHistory> ->
                            _uiState.update { it.copy(history = list) }
                        }
                }
            }

            is DetailEvent.SyncEntry -> {
                val editedTitle = if (_uiState.value.isEditingTitle) _uiState.value.editedTitle else event.entry.title
                refreshFromEntry(event.entry, _uiState.value.isEditingTitle, editedTitle)
            }

            is DetailEvent.CommitEntryUpdate -> {
                val editedTitle = if (_uiState.value.isEditingTitle) {
                    _uiState.value.editedTitle
                } else {
                    event.entry.title
                }
                refreshFromEntry(event.entry, _uiState.value.isEditingTitle, editedTitle)
                emitEntryUpdated(event.entry)
            }

            DetailEvent.ShowIconPicker -> {
                _effects.tryEmit(DetailEffect.IconPickerRequested)
            }

            DetailEvent.StartTitleEdit -> {
                _uiState.update {
                    val currentTitle = it.entry?.title.orEmpty()
                    it.copy(isEditingTitle = true, editedTitle = currentTitle)
                }
            }

            DetailEvent.CancelTitleEdit -> {
                _uiState.update {
                    it.copy(
                        isEditingTitle = false,
                        editedTitle = it.entry?.title.orEmpty()
                    )
                }
            }

            is DetailEvent.UpdateEditedTitle -> {
                _uiState.update { it.copy(editedTitle = event.value) }
            }

            DetailEvent.SaveTitle -> {
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
                    commitEntryUpdate(current.copy(title = newTitle), isEditingTitle = false)
                }
            }

            DetailEvent.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return
                commitEntryUpdate(current.copy(favorite = !current.favorite))
            }

            is DetailEvent.RevealField -> {
                _uiState.update { state ->
                    val updated = state.revealedFields.toMutableMap()
                    if (event.value != null) updated[event.key] = event.value
                    else updated.remove(event.key)
                    state.copy(revealedFields = updated)
                }
            }

            is DetailEvent.RecordAction -> {
                val current = _uiState.value.entry ?: return
                if (event.type == VaultHistory.HistoryType.ACCESS && !_uiState.value.isAccessHistoryEnabled) return
                if (event.type == VaultHistory.HistoryType.COPY) {
                    _uiState.update { it.copy(revealedFields = emptyMap()) }
                }

                viewModelScope.launch {
                    detailUseCases.insertHistory(
                        VaultHistory(
                            entryId = current.id,
                            fieldName = event.field,
                            changeType = event.type
                        )
                    )
                }
            }

            is DetailEvent.ToggleAccessHistoryRecording -> {
                _uiState.update { it.copy(isAccessHistoryEnabled = event.enabled) }
                viewModelScope.launch {
                    userConfigUseCases.update { config ->
                        config.copy(
                            extras = config.extras.toMutableMap().apply {
                                this[ACCESS_HISTORY_TOGGLE_KEY] = event.enabled.toString()
                            },
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                }
            }

            DetailEvent.ClearSensitiveState -> {
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
            val updated = detailUseCases.downloadAndApplyFavicon(entry)
            if (updated != null) {
                refreshFromEntry(updated, _uiState.value.isEditingTitle, _uiState.value.editedTitle)
            }
        }
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