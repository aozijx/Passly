package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.domain.entry.model.lookup.EntryListItem
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataUiState(
    val isAutoDownloadIcons: Boolean = true,
    val directoryUri: String? = null,
    val deletedEntries: List<EntryListItem> = emptyList(),
    val isTrashLoading: Boolean = true,
    val activeTrashEntryId: String? = null,
    val isEmptyingTrash: Boolean = false,
    val trashError: String? = null
) {
    val isTrashBusy: Boolean
        get() = activeTrashEntryId != null || isEmptyingTrash
}

sealed interface DataUiAction {
    data class SetAutoDownloadIcons(val enabled: Boolean) : DataUiAction
    data class SetBackupDirectoryUri(val uri: String) : DataUiAction
    data object ClearBackupDirectory : DataUiAction
    data class RestoreTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataUiAction

    data class DeleteTrashEntry(
        val entryId: String,
        val expectedVersion: Int
    ) : DataUiAction

    data object EmptyTrash : DataUiAction
    data object ClearTrashError : DataUiAction
}

@HiltViewModel
class DataViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository
) : ViewModel() {

    private val _config = MutableStateFlow(DataUiState())
    val config: StateFlow<DataUiState> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.settings.map { it.interaction.isAutoDownloadIcons },
                settingsRepository.settings.map { it.backup.directoryTreeUri }
            ) { autoDownloadIcons, directoryUri ->
                autoDownloadIcons to directoryUri
            }.collect { (autoDownloadIcons, directoryUri) ->
                _config.update {
                    it.copy(
                        isAutoDownloadIcons = autoDownloadIcons,
                        directoryUri = directoryUri
                    )
                }
            }
        }
        viewModelScope.launch {
            entryListQueryRepository.deletedEntries
                .catch { error ->
                    _config.update {
                        it.copy(
                            isTrashLoading = false,
                            trashError = error.toUiMessage("无法读取回收站")
                        )
                    }
                }
                .collect { entries ->
                    _config.update {
                        it.copy(
                            deletedEntries = entries,
                            isTrashLoading = false
                        )
                    }
                }
        }
    }

    fun onAction(action: DataUiAction) {
        when (action) {
            is DataUiAction.SetAutoDownloadIcons -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            }

            is DataUiAction.SetBackupDirectoryUri -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetBackupDirectoryUri(action.uri))
            }

            is DataUiAction.ClearBackupDirectory -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.ClearBackupDirectoryUri)
            }

            is DataUiAction.RestoreTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.restoreEntry(
                    id = action.entryId,
                    expectedVersion = action.expectedVersion
                )
            }

            is DataUiAction.DeleteTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.deletePermanently(
                    id = action.entryId,
                    expectedVersion = action.expectedVersion
                )
            }

            is DataUiAction.EmptyTrash -> emptyTrash()

            is DataUiAction.ClearTrashError -> {
                _config.update { it.copy(trashError = null) }
            }
        }
    }

    private fun runTrashEntryAction(
        entryId: String,
        operation: suspend () -> AppResult<Unit>
    ) {
        if (_config.value.isTrashBusy) return
        viewModelScope.launch {
            _config.update {
                it.copy(activeTrashEntryId = entryId, trashError = null)
            }
            try {
                when (val result = operation()) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _config.update {
                            it.copy(trashError = result.error.toUiMessage("回收站操作失败"))
                        }
                    }
                }
            } finally {
                _config.update { it.copy(activeTrashEntryId = null) }
            }
        }
    }

    private fun emptyTrash() {
        if (_config.value.isTrashBusy || _config.value.deletedEntries.isEmpty()) return
        viewModelScope.launch {
            _config.update { it.copy(isEmptyingTrash = true, trashError = null) }
            try {
                when (val result = entryCommandRepository.emptyTrash()) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        _config.update {
                            it.copy(trashError = result.error.toUiMessage("无法清空回收站"))
                        }
                    }
                }
            } finally {
                _config.update { it.copy(isEmptyingTrash = false) }
            }
        }
    }
}
