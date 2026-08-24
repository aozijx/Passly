package com.aozijx.passly.presentation.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.settings.model.SettingsCommand
import com.aozijx.passly.domain.settings.port.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementSettingsViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementSettingsUiState())
    val uiState: StateFlow<DataManagementSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                mutate(
                    DataManagementSettingsMutation.SettingsChanged(
                        autoDownloadIcons = settings.interaction.isAutoDownloadIcons,
                        directoryUri = settings.backup.directoryTreeUri,
                    )
                )
            }
        }
        viewModelScope.launch {
            entryListQueryRepository.deletedEntries
                .catch { error ->
                    mutate(
                        DataManagementSettingsMutation.TrashLoadFailed(
                            error.toUiMessage("无法读取回收站")
                        )
                    )
                }
                .collect { entries ->
                    mutate(DataManagementSettingsMutation.TrashLoaded(entries))
                }
        }
    }

    fun onAction(action: DataManagementSettingsUiAction) {
        when (action) {
            is DataManagementSettingsUiAction.SetAutoDownloadIcons ->
                updateSettings(SettingsCommand.SetAutoDownloadIcons(action.enabled))
            is DataManagementSettingsUiAction.SetBackupDirectoryUri ->
                updateSettings(SettingsCommand.SetBackupDirectoryUri(action.uri))
            is DataManagementSettingsUiAction.ClearBackupDirectory ->
                updateSettings(SettingsCommand.ClearBackupDirectoryUri)

            is DataManagementSettingsUiAction.RestoreTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.restoreEntry(
                    id = EntryId(action.entryId),
                    expectedVersion = EntryVersion(action.expectedVersion)
                )
            }

            is DataManagementSettingsUiAction.DeleteTrashEntry -> runTrashEntryAction(action.entryId) {
                entryCommandRepository.deletePermanently(
                    id = EntryId(action.entryId),
                    expectedVersion = EntryVersion(action.expectedVersion)
                )
            }

            is DataManagementSettingsUiAction.EmptyTrash -> emptyTrash()

            is DataManagementSettingsUiAction.ClearTrashError -> {
                mutate(DataManagementSettingsMutation.TrashErrorCleared)
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
        if (_uiState.value.isTrashBusy) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            mutate(DataManagementSettingsMutation.TrashEntryActionStarted(entryId))
            try {
                operation().updateTrashError("回收站操作失败")
            } finally {
                mutate(DataManagementSettingsMutation.TrashEntryActionFinished)
            }
        }
    }

    private fun emptyTrash() {
        if (_uiState.value.isTrashBusy || _uiState.value.deletedEntries.isEmpty()) return
        if (!requireTrashAccess()) return
        viewModelScope.launch {
            if (!requireTrashAccess()) return@launch
            mutate(DataManagementSettingsMutation.EmptyTrashStarted)
            try {
                entryCommandRepository.emptyTrash().updateTrashError("无法清空回收站")
            } finally {
                mutate(DataManagementSettingsMutation.EmptyTrashFinished)
            }
        }
    }

    private fun requireTrashAccess(): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        mutate(DataManagementSettingsMutation.TrashActionFailed("当前会话不能操作回收站"))
        return false
    }

    private fun AppResult<*>.updateTrashError(fallback: String) {
        if (this is AppResult.Failure) {
            mutate(DataManagementSettingsMutation.TrashActionFailed(error.toUiMessage(fallback)))
        }
    }

    private fun mutate(mutation: DataManagementSettingsMutation) {
        _uiState.value = DataManagementSettingsReducer.reduce(_uiState.value, mutation)
    }
}
