package com.aozijx.passly.features.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
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

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val detailUseCases = AppContainer.domain.detailUseCases
    private val entryAnalyzer = DetailEntryAnalyzer()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private val _effects = MutableSharedFlow<DetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<DetailEffect> = _effects.asSharedFlow()

    init {
        EntryTypeStrategyRegistry.ensureRegistered()
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

            is DetailEvent.SetRevealedUsername -> {
                _uiState.update { it.copy(revealedUsername = event.value) }
            }

            is DetailEvent.SetRevealedPassword -> {
                _uiState.update { it.copy(revealedPassword = event.value) }
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
        val domain = entry.associatedDomain
        if (!domain.isNullOrBlank() && entry.iconCustomPath.isNullOrBlank()) {
            viewModelScope.launch {
                val outcome = detailUseCases.downloadFavicon(domain)
                if (outcome.result == FaviconResult.SUCCESS && outcome.filePath != null) {
                    val updatedEntry = entry.copy(iconCustomPath = outcome.filePath)
                    detailUseCases.updateEntry(updatedEntry)
                    refreshFromEntry(updatedEntry, _uiState.value.isEditingTitle, _uiState.value.editedTitle)
                }
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
        // 安全性增强：ViewModel 销毁时立即清空内存中的敏感数据
        _uiState.update { DetailUiState() }
    }
}