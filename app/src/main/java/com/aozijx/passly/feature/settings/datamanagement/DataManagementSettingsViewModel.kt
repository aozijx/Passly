package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ui.toUiMessage
import com.aozijx.passly.domain.authentication.VaultAccessState
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

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val vaultAccessState: VaultAccessState
) : ViewModel() {

    private val _config = MutableStateFlow(DataManagementSettingsUiState())
    val config: StateFlow<DataManagementSettingsUiState> = _config.asStateFlow()

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

    fun onAction(action: DataManagementSettingsAction) {
        when (action) {
            is DataManagementSettingsAction.SetAutoDownloadIcons -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            }

            is DataManagementSettingsAction.SetBackupDirectoryUri -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.SetBackupDirectoryUri(action.uri))
            }

            is DataManagementSettingsAction.ClearBackupDirectory -> viewModelScope.launch {
                settingsRepository.update(SettingsCommand.ClearBackupDirectoryUri)
            }

            is DataManagementSettingsAction.RestoreTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.restoreEntry(
                    id = action.entryId,
                    expectedVersion = action.expectedVersion
                )
            }

            is DataManagementSettingsAction.DeleteTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.deletePermanently(
                    id = action.entryId,
                    expectedVersion = action.expectedVersion
                )
            }

            is DataManagementSettingsAction.EmptyTrash -> emptyTrash()

            is DataManagementSettingsAction.ClearTrashError -> {
                _config.update { it.copy(trashError = null) }
            }
        }
    }

    private fun runTrashEntryAction(
        entryId: String,
        operation: suspend () -> AppResult<Unit>
    ) {
        if (_config.value.isTrashBusy) return
        if (!vaultAccessState.hasFullVaultAccess()) {
            _config.update { it.copy(trashError = "当前会话不能操作回收站") }
            return
        }
        viewModelScope.launch {
            if (!vaultAccessState.hasFullVaultAccess()) {
                _config.update { it.copy(trashError = "当前会话不能操作回收站") }
                return@launch
            }
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
        if (!vaultAccessState.hasFullVaultAccess()) {
            _config.update { it.copy(trashError = "当前会话不能操作回收站") }
            return
        }
        viewModelScope.launch {
            if (!vaultAccessState.hasFullVaultAccess()) {
                _config.update { it.copy(trashError = "当前会话不能操作回收站") }
                return@launch
            }
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
