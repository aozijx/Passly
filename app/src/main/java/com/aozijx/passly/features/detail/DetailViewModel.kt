package com.aozijx.passly.features.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.icon.FaviconResult
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry
import com.aozijx.passly.features.detail.page.DetailEvent
import com.aozijx.passly.features.detail.page.DetailUiState
import com.aozijx.passly.features.detail.page.internal.DetailEntryAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val detailUseCases = AppContainer.domain.detailUseCases
    private val entryAnalyzer = DetailEntryAnalyzer()

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        EntryTypeStrategyRegistry.ensureRegistered()
    }

    fun onEvent(event: DetailEvent): VaultEntry? {
        return when (event) {
            is DetailEvent.Initialize -> {
                refreshFromEntry(event.initialEntry, isEditingTitle = false, editedTitle = event.initialEntry.title)
                
                viewModelScope.launch {
                    val latest = detailUseCases.getEntryById(event.initialEntry.id) ?: event.initialEntry
                    refreshFromEntry(latest, isEditingTitle = false, editedTitle = latest.title)
                    autoDownloadFavicon(latest)
                }
                null
            }

            is DetailEvent.SyncEntry -> {
                val editedTitle = if (_uiState.value.isEditingTitle) _uiState.value.editedTitle else event.entry.title
                refreshFromEntry(event.entry, _uiState.value.isEditingTitle, editedTitle)
                null
            }

            DetailEvent.StartTitleEdit -> {
                _uiState.update {
                    val currentTitle = it.entry?.title.orEmpty()
                    it.copy(isEditingTitle = true, editedTitle = currentTitle)
                }
                null
            }

            DetailEvent.CancelTitleEdit -> {
                _uiState.update {
                    it.copy(
                        isEditingTitle = false,
                        editedTitle = it.entry?.title.orEmpty()
                    )
                }
                null
            }

            is DetailEvent.UpdateEditedTitle -> {
                _uiState.update { it.copy(editedTitle = event.value) }
                null
            }

            DetailEvent.SaveTitle -> {
                val state = _uiState.value
                val current = state.entry ?: return null
                val newTitle = state.editedTitle.trim()
                if (newTitle.isBlank() || newTitle == current.title) {
                    _uiState.update {
                        it.copy(
                            isEditingTitle = false,
                            editedTitle = current.title
                        )
                    }
                    null
                } else {
                    val updated = current.copy(title = newTitle)
                    refreshFromEntry(updated, isEditingTitle = false, editedTitle = updated.title)
                    updated
                }
            }

            DetailEvent.ToggleFavorite -> {
                val current = _uiState.value.entry ?: return null
                val updated = current.copy(favorite = !current.favorite)
                refreshFromEntry(updated, _uiState.value.isEditingTitle, _uiState.value.editedTitle)
                updated
            }
        }
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
}