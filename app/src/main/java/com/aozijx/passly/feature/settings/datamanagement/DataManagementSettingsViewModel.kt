package com.aozijx.passly.feature.settings.datamanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.domain.entry.repository.EntryListQueryRepository
import com.aozijx.passly.domain.settings.command.SettingsCommand
import com.aozijx.passly.domain.settings.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val vaultAccessState: SecureSessionAccessState
) : ViewModel() {

    private val _config = MutableStateFlow(DataManagementSettingsUiState())
    val config: StateFlow<DataManagementSettingsUiState> = _config.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _config.update {
                    it.copy(
                        isAutoDownloadIcons = settings.interaction.isAutoDownloadIcons,
                        directoryUri = settings.backup.directoryTreeUri
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
            is DataManagementSettingsAction.SetAutoDownloadIcons ->
                updateSettings(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            is DataManagementSettingsAction.SetBackupDirectoryUri ->
                updateSettings(SettingsCommand.SetBackupDirectoryUri(action.uri))
            is DataManagementSettingsAction.ClearBackupDirectory ->
                updateSettings(SettingsCommand.ClearBackupDirectoryUri)

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

    private fun updateSettings(command: SettingsCommand) {
        viewModelScope.launch { settingsRepository.update(command) }
    }

    private fun runTrashEntryAction(
        entryId: String,
        operation: suspend () -> AppResult<Unit>
    ) {
        if (_config.value.isTrashBusy) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            _config.update { it.copy(activeTrashEntryId = entryId, trashError = null) }
            try {
                operation().updateTrashError("回收站操作失败")
            } finally {
                _config.update { it.copy(activeTrashEntryId = null) }
            }
        }
    }

    private fun emptyTrash() {
        if (_config.value.isTrashBusy || _config.value.deletedEntries.isEmpty()) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            _config.update { it.copy(isEmptyingTrash = true, trashError = null) }
            try {
                entryCommandRepository.emptyTrash().updateTrashError("无法清空回收站")
            } finally {
                _config.update { it.copy(isEmptyingTrash = false) }
            }
        }
    }

    private fun requireTrashAccess(): Boolean {
        if (vaultAccessState.hasFullSecureSessionAccess()) return true
        _config.update { it.copy(trashError = "当前会话不能操作回收站") }
        return false
    }

    private fun AppResult<*>.updateTrashError(fallback: String) {
        if (this is AppResult.Failure) {
            _config.update { it.copy(trashError = error.toUiMessage(fallback)) }
        }
    }
}
